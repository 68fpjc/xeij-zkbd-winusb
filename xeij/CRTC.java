//========================================================================================
//  CRTC.java
//    en:CRT controller
//    ja:CRTコントローラ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//
//    垂直同期
//                                     ラスタ0
//                                       ↓
//                             ┌──────────────垂直周期──────────────┐
//                1                      ┏━━┓                                                          ┏━━┓
//    垂直同期信号      垂直パルス間     ┃    ┃                       垂直パルス間                       ┃    ┃     垂直パルス間
//       V-SYNC   0━━━━━━━━━━━┛    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛    ┗━━━━━━━━━━━
//                             │  垂直  │垂直│  垂直  │                                      │  垂直  │垂直│  垂直  │
//                             │フロント  同期   バック │                                      │フロント  同期   バック │
//                             │ ポーチ  パルス  ポーチ │                                      │ ポーチ  パルス  ポーチ │
//                1━━━━━━┓                        ┏━━━━━━━━━━━━━━━━━━━┓                        ┏━━━━━━1
//    垂直映像信号 垂直映像期間┃      垂直空白期間      ┃             垂直映像期間             ┃      垂直空白期間      ┃垂直映像期間
//       V-DISP   0            ┗━━━━━━━━━━━━┛                                      ┗━━━━━━━━━━━━┛            0
//
//
//    水平同期
//                             ┌──────────────水平周期──────────────┐
//                1                      ┏━━┓                                                          ┏━━┓                      1
//    水平同期信号      水平パルス間     ┃    ┃                       水平パルス間                       ┃    ┃     水平パルス間
//       H-SYNC   0━━━━━━━━━━━┛    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛    ┗━━━━━━━━━━━0
//                             │  水平  │水平│  水平  │                                      │  水平  │水平│  水平  │
//                             │フロント  同期   バック │                                      │フロント  同期   バック │
//                             │ ポーチ  パルス  ポーチ │                                      │ ポーチ  パルス  ポーチ │
//                 ━━━━━━┓                        ┏━━━━━━━━━━━━━━━━━━━┓                        ┏━━━━━━
//    水平映像信号 水平映像期間┃      水平空白期間      ┃             水平映像期間             ┃      水平空白期間      ┃水平映像期間
//                             ┗━━━━━━━━━━━━┛                                      ┗━━━━━━━━━━━━┛
//                             └┐                                                              └┐
//                1━━━━━━━┓                                                                ┏━━━━━━━━━━━━━━━━━━1
//      CRTC IRQ                 ┃                            IRQラスタ                           ┃
//                0              ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛                                    0
//                               └┐                                                              └┐
//                1                ┏━━━━━━━━━━━━━━━ ∥ ━━━━━━━━━━━━━━━┓                                  1
//    垂直映像信号                 ┃    垂直映像期間開始ラスタ     ∥     垂直映像期間終了ラスタ    ┃
//       V-DISP   0━━━━━━━━┛                               ∥                               ┗━━━━━━━━━━━━━━━━━0
//
//
//    HT  水平周期カラム数
//    HS  水平同期パルスカラム数
//    HB  水平バックポーチカラム数
//    HD  水平映像期間カラム数
//    HF  水平フロントポーチカラム数
//    VT  垂直周期ラスタ数
//    VS  垂直同期パルスラスタ数
//    VB  垂直バックポーチラスタ数
//    VD  垂直映像期間ラスタ数
//    VF  垂直フロントポーチラスタ数
//
//    0x00e80000  .w  R00  HT-1=HS+HB+HD+HF-1
//    0x00e80002  .w  R01  HS-1
//    0x00e80004  .w  R02  HS+HB-5
//    0x00e80006  .w  R03  HS+HB+HD-5
//    0x00e80008  .w  R04  VT-1=VS+VB+VD+VF-1
//    0x00e8000a  .w  R05  VS-1
//    0x00e8000c  .w  R06  VS+VB-1
//    0x00e8000e  .w  R07  VS+VB+VD-1
//    0x00e80029  .b  R20L
//
//    0x00e8e007  bit1  HRL
//
//
//  ----------------------------------------------------------------------------------------------------------------------------
//       解像度  サイズ         色x枚    R00   R01   R02   R03    R04   R05   R06   R07    R20L  HRL       水平同期周波数
//  CRTMOD              実画面                                                                             垂直同期周波数
//                                       HT   HS   HB   HD   HF   VT   VS   VB   VD   VF               垂直周期 理論値 実測値
//  ----------------------------------------------------------------------------------------------------------------------------
//    $00  高   512x512  1024     16x1  $005B $0009 $0011 $0051  $0237 $0005 $0028 $0228   $15    0  69.552MHz/3/8/ 92=31.500kHz
//    $04                 512     16x4   92-1  10-1  22-5  86-5  568-1   6-1  41-1 553-1  %10101          31.500kHz/568=55.458Hz
//    $08                 512    256x2   92   10   12   64    6  568    6   35  512   15                  18032μs   18026μs
//    $0C                 512  65536x1
//  ----------------------------------------------------------------------------------------------------------------------------
//    $01  低   512x512  1024     16x1  $004B $0003 $0005 $0045  $0103 $0002 $0010 $0100   $05    0  38.864MHz/4/8/ 76=15.980kHz
//    $05                 512     16x4   76-1   4-1  10-5  74-5  260-1   2-1  17-1 257-1  %00101          15.980kHz/260=61.462Hz
//    $09                 512    256x2   76    4    6   64    2  260    2   15  240    3                  16270μs   16265μs
//    $0D                 512  65536x1
//  ----------------------------------------------------------------------------------------------------------------------------
//    $02  高   256x256  1024     16x1  $002D $0004 $0006 $0026  $0237 $0005 $0028 $0228   $10    0  69.552MHz/6/8/ 46=31.500kHz
//    $06                 512     16x4   46-1   5-1  11-5  43-5  568-1   6-1  41-1 553-1  %10000          31.500kHz/568=55.458Hz
//    $0A                 512    256x2   46    5    6   32    3  568    6   35  512   15                  18032μs   18027μs
//    $0E                 512  65536x1
//  ----------------------------------------------------------------------------------------------------------------------------
//    $03  低   256x256  1024     16x1  $0025 $0001 $0000 $0020  $0103 $0002 $0010 $0100   $00    0  38.864MHz/8/8/ 38=15.980kHz
//    $07                 512     16x4   38-1   2-1   5-5  37-5  260-1   2-1  17-1 257-1  %00000          15.980kHz/260=61.462Hz
//    $0B                 512    256x2   38    2    3   32    1  260    2   15  240    3                  16270μs   16265μs
//    $0F                 512  65536x1
//  ----------------------------------------------------------------------------------------------------------------------------
//    $10  高   768x512  1024     16x1  $0089 $000E $001C $007C  $0237 $0005 $0028 $0228   $16    0  69.552MHz/2/8/138=31.500kHz
//    $14                 512    256x2  138-1  15-1  33-5 129-5  568-1   6-1  41-1 553-1  %10110          31.500kHz/568=55.458Hz
//    $18                 512  65536x1  138   15   18   96    9  568    6   35  512   15                  18032μs   18026μs
//  ----------------------------------------------------------------------------------------------------------------------------
//    $11  中  1024x424  1024     16x1  $00AF $000F $001F $009F  $01D0 $0007 $0020 $01C8   $16    0  69.552MHz/2/8/176=24.699kHz
//    $15                 512    256x2  176-1  16-1  36-5 164-5  465-1   8-1  33-1 457-1  %10110          24.699kHz/465=53.116Hz
//    $19                 512  65536x1  176   16   20  128   12  465    8   25  424    8                  18827μs   18822μs
//  ----------------------------------------------------------------------------------------------------------------------------
//    $12  中  1024x848  1024     16x1  $00AF $000F $001F $009F  $01D0 $0007 $0020 $01C8   $1A    0  69.552MHz/2/8/176=24.699kHz
//    $16                 512    256x2  176-1  16-1  36-5 164-5  465-1   8-1  33-1 457-1  %11010          24.699kHz/465=53.116Hz
//    $1A                 512  65536x1  176   16   20  128   12  465    8   25  424    8                  18827μs   18862μs
//  ----------------------------------------------------------------------------------------------------------------------------
//    $13  中   640x480  1024     16x1  $0063 $000B $000D $005D  $020C $0001 $0021 $0201   $17    0  50.350MHz/2/8/100=31.469kHz
//    $17        (VGA)    512    256x2  100-1  12-1  18-5  98-5  525-1   2-1  34-1 514-1  %10111          31.469kHz/525=59.940Hz
//    $1B                 512  65536x1  100   12    6   80    2  525    2   32  480   11                  16683μs   16678μs
//  ----------------------------------------------------------------------------------------------------------------------------
//
//
//  CRTMOD$10(768x512)のR20LとHRLを変更して垂直周期を計測し、R20LとHRLとオシレータと分周比の関係を調べた
//
//           オシレータ/分周比    垂直周期               オシレータ/分周比    垂直周期
//     R20L HRL               理論値    実測値     R20L HRL               理論値    実測値
//      $00  0  38.864MHz/8  129082μs  129079μs     $00  1  38.864MHz/8  129082μs  129079μs
//      $01  0  38.864MHz/4   64541μs   64539μs     $01  1  38.864MHz/4   64541μs   64539μs
//      $02  0  38.864MHz/8  129082μs  129078μs     $02  1  38.864MHz/8  129082μs  129079μs
//      $03  0  38.864MHz/8  129082μs  129079μs     $03  1  38.864MHz/8  129082μs  129078μs
//      $04  0  38.864MHz/8  129082μs  129306μs     $04  1  38.864MHz/8  129082μs  129306μs
//      $05  0  38.864MHz/4   64541μs   64653μs     $05  1  38.864MHz/4   64541μs   64653μs
//      $06  0  38.864MHz/8  129082μs  129306μs     $06  1  38.864MHz/8  129082μs  129306μs
//      $07  0  38.864MHz/8  129082μs  129307μs     $07  1  38.864MHz/8  129082μs  129306μs
//      $08  0  38.864MHz/8  129082μs  129306μs     $08  1  38.864MHz/8  129082μs  129307μs
//      $09  0  38.864MHz/4   64541μs   64652μs     $09  1  38.864MHz/4   64541μs   64652μs
//      $0A  0  38.864MHz/8  129082μs  129305μs     $0A  1  38.864MHz/8  129082μs  129306μs
//      $0B  0  38.864MHz/8  129082μs  129307μs     $0B  1  38.864MHz/8  129082μs  129306μs
//      $0C  0  38.864MHz/8  129082μs  129307μs     $0C  1  38.864MHz/8  129082μs  129306μs
//      $0D  0  38.864MHz/4   64541μs   64652μs     $0D  1  38.864MHz/4   64541μs   64653μs
//      $0E  0  38.864MHz/8  129082μs  129305μs     $0E  1  38.864MHz/8  129082μs  129306μs
//      $0F  0  38.864MHz/8  129082μs  129306μs     $0F  1  38.864MHz/8  129082μs  129307μs
//      $10  0  69.552MHz/6   54095μs   54093μs     $10  1  69.552MHz/8   72127μs   72126μs
//      $11  0  69.552MHz/3   27048μs   27047μs     $11  1  69.552MHz/4   36064μs   36060μs
//      $12  0  69.552MHz/2   18032μs   18031μs     $12  1  69.552MHz/2   18032μs   18029μs
//      $13  0  50.350MHz/2   24909μs   24905μs     $13  1  50.350MHz/2   24909μs   24905μs
//      $14  0  69.552MHz/6   54095μs   54094μs     $14  1  69.552MHz/8   72127μs   72124μs
//      $15  0  69.552MHz/3   27048μs   27046μs     $15  1  69.552MHz/4   36064μs   36061μs
//      $16  0  69.552MHz/2   18032μs   18030μs     $16  1  69.552MHz/2   18032μs   18030μs
//      $17  0  50.350MHz/2   24909μs   24906μs     $17  1  50.350MHz/2   24909μs   24906μs
//      $18  0  69.552MHz/6   54095μs   54188μs     $18  1  69.552MHz/8   72127μs   72251μs
//      $19  0  69.552MHz/3   27048μs   27094μs     $19  1  69.552MHz/4   36064μs   36124μs
//      $1A  0  69.552MHz/2   18032μs   18061μs     $1A  1  69.552MHz/2   18032μs   18062μs
//      $1B  0  50.350MHz/2   24909μs   24950μs     $1B  1  50.350MHz/2   24909μs   24950μs
//      $1C  0  69.552MHz/6   54095μs   54189μs     $1C  1  69.552MHz/8   72127μs   72250μs
//      $1D  0  69.552MHz/3   27048μs   27092μs     $1D  1  69.552MHz/4   36064μs   36125μs
//      $1E  0  69.552MHz/2   18032μs   18062μs     $1E  1  69.552MHz/2   18032μs   18062μs
//      $1F  0  50.350MHz/2   24909μs   24951μs     $1F  1  50.350MHz/2   24909μs   24950μs
//
//    perl -e "for$o(38.863632,69.551900,50.349800){for$d(6,8,3,4,2){printf'  //      %6.3fMHz/%d  %6.0fus%c',$o,$d,1/($o/($d*8*138*568)),10;}}"
//
//     オシレータ/分周比
//                    理論値    R20L  HRL
//      38.864MHz/6   96811μs
//      38.864MHz/8  129082μs  %0**00  *
//                             %0**1*  *
//      38.864MHz/3   48406μs
//      38.864MHz/4   64541μs  %0**01  *
//      38.864MHz/2   32270μs
//      69.552MHz/6   54095μs  %1**00  0
//      69.552MHz/8   72127μs  %1**00  1
//      69.552MHz/3   27048μs  %1**01  0
//      69.552MHz/4   36064μs  %1**01  1
//      69.552MHz/2   18032μs  %1**10  *
//      50.350MHz/6   74726μs
//      50.350MHz/8   99634μs
//      50.350MHz/3   37363μs
//      50.350MHz/4   49817μs
//      50.350MHz/2   24909μs  %1**11  *
//
//      オシレータと分周比はR20のbit4,1,0とHRLで決まる
//      HRLをセットすると69.552MHzの3分周と6分周が4分周と8分周に変わる
//      VGAモードのための50.350MHzのオシレータと起動時のVキーとNキーの処理はX68000 Compact(IPLROM 1.2)で追加された
//      X68000 XVI(IPLROM 1.1)までの機種ではVGAモードは使用できない
//
//----------------------------------------------------------------------------------------
//
//  R08 外部同期水平アジャスト
//
//    スーパーインポーズするときビデオの映像とX68000の映像を重ねるために、
//    ビデオとX68000の水平同期パルスの先頭の時間差を38.863632MHzのサイクル数で指定する。
//
//    低解像度512x512(インターレース)のとき
//      水平同期パルス幅は4カラム。R01=4-1=3
//      水平バックポーチは6カラム。R02=4+6-5=5
//      推測される計算式。おそらく正しい？
//        perl -e "print((4.7+4.7)*38.863632-(4*8*(4+6))-1)"
//        44.3181408
//      外部同期水平アジャストは44
//
//    低解像度256x256のとき
//      水平同期パルス幅は2カラム。R01=2-1=1
//      水平バックポーチは3カラム。R02=2+3-5=0
//      推測される計算式。1ドット追加する？
//        perl -e "print((4.7+4.7)*38.863632-(8*(8*(2+3)+1))-1)"
//        36.3181408
//      外部同期水平アジャストは36
//
//    低解像度以外の設定値27の根拠は不明
//
//----------------------------------------------------------------------------------------
//
//  ラスタコピー
//
//    関係するレジスタ
//      $00E8002A  .w  CRTC R21         bit3     1=テキストプレーン3をラスタコピーする,0=しない
//                                      bit2     1=テキストプレーン2をラスタコピーする,0=しない
//                                      bit1     1=テキストプレーン1をラスタコピーする,0=しない
//                                      bit0     1=テキストプレーン0をラスタコピーする,0=しない
//      $00E8002C  .w  CRTC R22         bit15-8  ソースラスタブロック番号(0～255)
//                                      bit7-0   デスティネーションラスタブロック番号(0～255)
//      $00E80480  .w  CRTC 動作ポート  bit3     1=水平フロントポーチでラスタコピーする,0=しない
//      $00E88001  .b  MFP GPIPデータ   bit7     1=水平同期パルス,0=水平パルス間(水平バックポーチ+水平映像期間+水平フロントポーチ)
//                                      bit4     1=垂直映像期間,0=垂直空白期間(垂直フロントポーチ+垂直同期パルス+垂直バックポーチ)
//
//    ラスタコピーの動作
//      水平フロントポーチの先頭で(動作ポート&8)!=0のとき、
//      (R21&1)!=0ならばテキストプレーン0($00E00000～$00E1FFFF)について、
//      (R21&2)!=0ならばテキストプレーン1($00E20000～$00E3FFFF)について、
//      (R21&4)!=0ならばテキストプレーン2($00E40000～$00E5FFFF)について、
//      (R21&8)!=0ならばテキストプレーン3($00E60000～$00E7FFFF)について、それぞれ、
//      ラスタブロック(R22>>>8)(テキストVRAMのオフセット(R22>>>8)*512～(R22>>>8)*512+511)の内容を、
//      ラスタブロック(R22&255)(テキストVRAMのオフセット(R22&255)*512～(R22&255)*512+511)へ、コピーする
//
//      メモ
//        ラスタコピーは水平同期パルスではなく水平フロントポーチで行われる
//          水平同期パルスに入る前に終わっている
//        動作ポートのbit3はMPUがラスタコピーの機能をON/OFFするための単なるスイッチである
//          0→1が動作開始を意味するトリガーのようなものではない
//          CRTCが動作終了を知らせるフラグのようなものでもない
//
//    水平同期パルスの待ち方
//      水平同期パルスは非常に短い
//          画面モード        水平同期パルスの長さ          10MHz換算
//          高解像度768x512   1/31500*10/92*1e6=3.45μs   34クロック
//          低解像度512x512   1/15980*4/76*1e6=3.29μs    32クロック
//          中解像度1024x848  1/24699*16/176*1e6=3.68μs  36クロック
//          VGA 640x480       1/31469*12/100*1e6=3.81μs  38クロック
//      水平同期パルスを待つときは見逃さないように割り込みを禁止してなるべく短い間隔でGPIPデータを監視しなければならない
//      普通の方法
//                  lea.l   GPIPデータ,a0
//                  割り込みを禁止する
//          1:      tst.b   (a0)
//                  bpl.s   1b
//      ショートの条件分岐命令は通過する方が速い
//                  lea.l   GPIPデータ,a0
//                  割り込みを禁止する
//          1:      .rept   9
//                  tst.b   (a0)
//                  bmi.s   2f
//                  .endm
//                  tst.b   (a0)
//                  bpl.s   1b
//          2:
//      水平同期パルスの先頭を通過したかどうかが分かればよいので、多少進み過ぎても問題ない
//                  lea.l   GPIPデータ,a0
//                  割り込みを禁止する
//          1:      move.b  (a0),d0
//                  .rept   9
//                  or.b    (a0),d0
//                  .endm
//                  bpl.s   1b
//
//    ラスタコピーの手順
//      10MHz機ではGPIPデータの監視の条件分岐とR22の更新とループの分岐が水平同期パルスに収まらないので水平パルス間を待つ必要がほとんど無い
//      しかし、水平同期パルスだけを待つ方法ではMPUが速いとき1回の水平同期パルスにR22を2回更新してしまいラスタコピーに失敗する可能性がある
//      MPUがどんなに速くても問題なく動作させるには水平同期パルスと水平パルス間を両方待たなければならない
//                  move.w  #コピーするラスタブロック数-1,d3
//                  bmi.s   5f
//                  割り込みを禁止する
//                  水平同期パルスを待つ
//                  水平パルス間を待つ
//                  R21を設定する
//                  R22を設定する
//                  動作ポートのbit3をセットする
//                  bra.s   4f
//          3:      割り込みを禁止する
//                  水平同期パルスを待つ
//                  水平パルス間を待つ
//                  R22を更新する
//          4:      割り込みを許可する
//                  dbra.w  d3.3b
//                  割り込みを禁止する
//                  水平同期パルスを待つ
//                  水平パルス間を待つ
//                  動作ポートのbit3をクリアする
//                  割り込みを許可する
//          5:
//
//    画面クリア
//        ラスタブロック63が余っているとき
//          同時アクセスを使ってラスタブロック63をクリア
//          ラスタブロック63からラスタブロック(開始行*4+0)へラスタコピー
//          ラスタブロック63からラスタブロック(開始行*4+1)へラスタコピー
//                  :
//          ラスタブロック63からラスタブロック(終了行*4+2)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+3)へラスタコピー
//
//    16ドットスムーススクロールアップ
//        ラスタブロック63が余っているとき
//          同時アクセスを使ってラスタブロック63をクリア
//          垂直映像期間を待つ
//          垂直空白期間を待つ
//          ラスタブロック(開始行*4+4)からラスタブロック(開始行*4)へラスタコピー
//          ラスタブロック(開始行*4+5)からラスタブロック(開始行*4+1)へラスタコピー
//                  :
//          ラスタブロック(終了行*4+2)からラスタブロック(終了行*4-2)へラスタコピー
//          ラスタブロック(終了行*4+3)からラスタブロック(終了行*4-1)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+1)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+1)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+2)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+3)へラスタコピー
//
//    8ドットスムーススクロールアップ
//        ラスタブロック63が余っているとき
//          同時アクセスを使ってラスタブロック63をクリア
//        以下を2回繰り返す
//          垂直映像期間を待つ
//          垂直空白期間を待つ
//          ラスタブロック(開始行*4+2)からラスタブロック(開始行*4)へラスタコピー
//          ラスタブロック(開始行*4+3)からラスタブロック(開始行*4+1)へラスタコピー
//                  :
//          ラスタブロック(終了行*4+2)からラスタブロック(終了行*4)へラスタコピー
//          ラスタブロック(終了行*4+3)からラスタブロック(終了行*4+1)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+2)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+3)へラスタコピー
//
//    4ドットスムーススクロールアップ
//        ラスタブロック63が余っているとき
//          同時アクセスを使ってラスタブロック63をクリア
//        以下を4回繰り返す
//          垂直映像期間を待つ
//          垂直空白期間を待つ
//          ラスタブロック(開始行*4+1)からラスタブロック(開始行*4)へラスタコピー
//          ラスタブロック(開始行*4+2)からラスタブロック(開始行*4+1)へラスタコピー
//                  :
//          ラスタブロック(終了行*4+2)からラスタブロック(終了行*4+1)へラスタコピー
//          ラスタブロック(終了行*4+3)からラスタブロック(終了行*4+2)へラスタコピー
//          ラスタブロック63からラスタブロック(終了行*4+3)へラスタコピー
//
//    16ドットスムーススクロールダウン
//      スクロールダウンのときmemcpyの要領で下からコピーすると途中で走査線と衝突して画面に亀裂が入ってしまい見苦しくなる
//      スクロールダウンのときも上からコピーする
//      画面外の余っているラスタブロックをバッファに使う
//      Aをクリアしておいて0→B,A→0,1→A,B→1,2→B,A→2,3→A,B→3,…の順にコピーする
//      スクロールアップは走査線の4倍の速さで進むがスクロールダウンは2倍なので水平同期パルスを頻繁に見逃すと走査線に追い付かれてしまう
//      ラスタブロック56～63をバッファとして使う
//          同時アクセスを使ってラスタブロック56をクリア
//          ラスタブロック56からラスタブロック57へラスタコピー
//          ラスタブロック56からラスタブロック58へラスタコピー
//          ラスタブロック56からラスタブロック59へラスタコピー
//          垂直映像期間を待つ
//          垂直空白期間を待つ
//          ラスタブロック(開始行*4)からラスタブロック60へラスタコピー
//          ラスタブロック(開始行*4+1)からラスタブロック61へラスタコピー
//          ラスタブロック(開始行*4+2)からラスタブロック62へラスタコピー
//          ラスタブロック(開始行*4+3)からラスタブロック63へラスタコピー
//          ラスタブロック56からラスタブロック(開始行*4)へラスタコピー
//          ラスタブロック57からラスタブロック(開始行*4+1)へラスタコピー
//          ラスタブロック58からラスタブロック(開始行*4+2)へラスタコピー
//          ラスタブロック59からラスタブロック(開始行*4+3)へラスタコピー
//          ラスタブロック(開始行*4+4)からラスタブロック56へラスタコピー
//          ラスタブロック(開始行*4+5)からラスタブロック57へラスタコピー
//          ラスタブロック(開始行*4+6)からラスタブロック58へラスタコピー
//          ラスタブロック(開始行*4+7)からラスタブロック59へラスタコピー
//          ラスタブロック60からラスタブロック(開始行*4+4)へラスタコピー
//          ラスタブロック61からラスタブロック(開始行*4+5)へラスタコピー
//          ラスタブロック62からラスタブロック(開始行*4+6)へラスタコピー
//          ラスタブロック63からラスタブロック(開始行*4+7)へラスタコピー
//                  :
//          ラスタブロック56/60からラスタブロック(終了行*4)へラスタコピー
//          ラスタブロック57/61からラスタブロック(終了行*4+1)へラスタコピー
//          ラスタブロック58/62からラスタブロック(終了行*4+2)へラスタコピー
//          ラスタブロック59/63からラスタブロック(終了行*4+3)へラスタコピー
//      画面外のラスタブロックを利用できない場合は最下行になる行の一部をメモリに退避させてから最下行とその上のラスタブロックをバッファに使う
//
//    8ドットスムーススクロールダウン
//        以下を2回繰り返す
//          同時アクセスを使ってラスタブロック60をクリア
//          ラスタブロック60からラスタブロック61へラスタコピー
//          垂直映像期間を待つ
//          垂直空白期間を待つ
//          ラスタブロック(開始行*4)からラスタブロック62へラスタコピー
//          ラスタブロック(開始行*4+1)からラスタブロック63へラスタコピー
//          ラスタブロック60からラスタブロック(開始行*4)へラスタコピー
//          ラスタブロック61からラスタブロック(開始行*4+1)へラスタコピー
//          ラスタブロック(開始行*4+2)からラスタブロック60へラスタコピー
//          ラスタブロック(開始行*4+3)からラスタブロック61へラスタコピー
//          ラスタブロック62からラスタブロック(開始行*4+2)へラスタコピー
//          ラスタブロック63からラスタブロック(開始行*4+3)へラスタコピー
//                  :
//          ラスタブロック60/62からラスタブロック(終了行*4+2)へラスタコピー
//          ラスタブロック61/63からラスタブロック(終了行*4+3)へラスタコピー
//
//    4ドットスムーススクロールダウン
//        以下を4回繰り返す
//          同時アクセスを使ってラスタブロック62をクリア
//          垂直映像期間を待つ
//          垂直空白期間を待つ
//          ラスタブロック(開始行*4)からラスタブロック63へラスタコピー
//          ラスタブロック62からラスタブロック(開始行*4)へラスタコピー
//          ラスタブロック(開始行*4+1)からラスタブロック62へラスタコピー
//          ラスタブロック63からラスタブロック(開始行*4+1)へラスタコピー
//                  :
//          ラスタブロック62/63からラスタブロック(終了行*4+3)へラスタコピー
//
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class CRTC {

  //拡張グラフィック画面
  //  メモリモード5  1024ドット512色(拡張)
  //  メモリモード7  1024ドット65536色(拡張)
  public static final boolean CRT_EXTENDED_GRAPHIC = true;  //true=拡張グラフィック画面をONにできる
  public static boolean crtExtendedGraphicRequest;  //true=次回起動時に拡張グラフィック画面をONにする
  public static boolean crtExtendedGraphicOn;  //true=拡張グラフィック画面がON

  //画面モードに変更があったとき描画をリスタートさせるまでの遅延(XEiJ.TMR_FREQ単位)
  public static final long CRT_RESTART_DELAY = XEiJ.TMR_FREQ / 5;  //0.2秒

  //レジスタ
  //  R00-R19/R22-R23はwrite onlyでreadすると常に0が返るが読めないと不便なのでreadできるようにする
  //  動作ポートはreadできるが1を書き込んだビットは0を書き込むまで1のまま
  //  未定義ビットは常に0
  public static final int CRT_R00_H_FRONT_END = 0x00e80000;  //R00  bit7-0   水平フロントポーチ終了カラム
  public static final int CRT_R01_H_SYNC_END  = 0x00e80002;  //R01  bit7-0   水平同期パルス終了カラム
  public static final int CRT_R02_H_BACK_END  = 0x00e80004;  //R02  bit7-0   水平バックポーチ終了カラム
  public static final int CRT_R03_H_DISP_END  = 0x00e80006;  //R03  bit7-0   水平映像期間終了カラム
  public static final int CRT_R04_V_FRONT_END = 0x00e80008;  //R04  bit9-0   垂直フロントポーチ終了ラスタ
  public static final int CRT_R05_V_SYNC_END  = 0x00e8000a;  //R05  bit9-0   垂直同期パルス終了ラスタ
  public static final int CRT_R06_V_BACK_END  = 0x00e8000c;  //R06  bit9-0   垂直バックポーチ終了ラスタ
  public static final int CRT_R07_V_DISP_END  = 0x00e8000e;  //R07  bit9-0   垂直映像期間終了ラスタ
  public static final int CRT_R08_ADJUST      = 0x00e80010;  //R08  bit7-0   外部同期アジャスト。TVとX68000の水平同期パルスの立下りの時間差(39MHz)
  public static final int CRT_R09_IRQ_RASTER  = 0x00e80012;  //R09  bit9-0   IRQラスタ。0=垂直同期パルス開始ラスタ
  public static final int CRT_R10_TX_X        = 0x00e80014;  //R10  bit9-0   テキストX方向スクロール
  public static final int CRT_R11_TX_Y        = 0x00e80016;  //R11  bit9-0   テキストY方向スクロール
  public static final int CRT_R12_GR_X_0      = 0x00e80018;  //R12  bit9-0   グラフィックX方向スクロール4bitページ0
  public static final int CRT_R13_GR_Y_0      = 0x00e8001a;  //R13  bit9-0   グラフィックY方向スクロール4bitページ0
  public static final int CRT_R14_GR_X_1      = 0x00e8001c;  //R14  bit8-0   グラフィックX方向スクロール4bitページ1
  public static final int CRT_R15_GR_Y_1      = 0x00e8001e;  //R15  bit8-0   グラフィックY方向スクロール4bitページ1
  public static final int CRT_R16_GR_X_2      = 0x00e80020;  //R16  bit8-0   グラフィックX方向スクロール4bitページ2
  public static final int CRT_R17_GR_Y_2      = 0x00e80022;  //R17  bit8-0   グラフィックY方向スクロール4bitページ2
  public static final int CRT_R18_GR_X_3      = 0x00e80024;  //R18  bit8-0   グラフィックX方向スクロール4bitページ3
  public static final int CRT_R19_GR_Y_3      = 0x00e80026;  //R19  bit8-0   グラフィックY方向スクロール4bitページ3
  public static final int CRT_R20_MODE        = 0x00e80028;  //R20  bit12    テキストストレージ
  //                                                                bit11    グラフィックストレージ
  //                                                                bit10-8  メモリモード  0=512ドット16色
  //                                                                                       1=512ドット256色
  //                                                                                       2=メモリモード2
  //                                                                                       3=512ドット65536色
  //                                                                                       4=1024ドット16色
  //                                                                                       5=1024ドット16色/1024ドット256色(拡張)
  //                                                                                       6=1024ドット16色
  //                                                                                       7=1024ドット16色/1024ドット65536色(拡張)
  //                                                                bit4     解像度  0=低解像度,1=高解像度
  //                                                                bit3-2   垂直解像度  0=256(ラスタ2度読み),1=512,2=1024,3=1024
  //                                                                bit1-0   水平解像度  0=256,1=512,2=768,3=640
  public static final int CRT_R21_SELECT      = 0x00e8002a;  //R21  bit9     1=ビットマスクON
  //                                                                bit8     1=同時アクセスON
  //                                                                bit7     1=プレーン3を同時アクセスする
  //                                                                bit6     1=プレーン2を同時アクセスする
  //                                                                bit5     1=プレーン1を同時アクセスする
  //                                                                bit4     1=プレーン0を同時アクセスする
  //                                                                bit3     1=プレーン3をラスタコピー/高速クリアする
  //                                                                bit2     1=プレーン2をラスタコピー/高速クリアする
  //                                                                bit1     1=プレーン1をラスタコピー/高速クリアする
  //                                                                bit0     1=プレーン0をラスタコピー/高速クリアする
  public static final int CRT_R22_BLOCK       = 0x00e8002c;  //R22  bit15-8  0～255 ソースラスタブロック番号
  //                                                                bit7-0   0～255 デスティネーションラスタブロック番号
  public static final int CRT_R23_MASK        = 0x00e8002e;  //R23  bit15-0  ビットマスク。1のビットに書き込まない
  public static final int CRT_R24             = 0x00e80030;  //R24
  public static final int CRT_ACTION          = 0x00e80480;  //動作ポート  bit0  1=画像入力開始
  //                                                                       bit1  1=次の垂直表示開始で高速クリア開始
  //                                                                       bit3  1=ラスタコピー実行

  //ドットクロックオシレータ
  //  int k = crtHRLCurr << 3 | crtHighResoCurr << 2 | crtHResoCurr;
  //  crtColumnTime = (int) ((double) (XEiJ.TMR_FREQ * 8 * CRT_DIVS[k]) / (double) crtFreqs[CRT_OSCS[k]] + 0.5);
  //  10**12/(4*10**6)*8*1024=2048000000。4MHzを下回ると8分周で1024ドットの水平映像期間がintに収まらなくなる
  public static final int[] CRT_OSCS = { 0, 0, 0, 0, 1, 1, 1, 2, 0, 0, 0, 0, 1, 1, 1, 2 };  //HRLR20410→オシレータの番号
  public static final int[] CRT_DIVS = { 8, 4, 8, 8, 6, 3, 2, 2, 8, 4, 8, 8, 8, 4, 2, 2 };  //HRLR20410→分周比
  public static final int CRT_MIN_FREQ =  10000000;  //オシレータの周波数の下限(Hz)
  public static final int CRT_MAX_FREQ = 400000000;  //オシレータの周波数の上限(Hz)
  public static final int[] CRT_DEFAULT_FREQS = { 38863632, 69551900, 50349800 };  //デフォルトのオシレータの周波数(Hz)
  public static final int[] crtFreqsRequest = new int[3];  //リセット後のオシレータの周波数(Hz)
  public static final int[] crtFreqs = new int[3];  //現在のオシレータの周波数(Hz)

  //レジスタ
  //  ゼロ拡張
  public static int crtR00HFrontEndPort;                 //R00 7-0 水平フロントポーチ終了カラム
  public static int crtR00HFrontEndMask;
  public static int crtR00HFrontEndTest;
  public static int crtR00HFrontEndCurr;
  public static int crtR01HSyncEndPort;                  //R01 7-0 水平同期パルス終了カラム
  public static int crtR01HSyncEndMask;
  public static int crtR01HSyncEndTest;
  public static int crtR01HSyncEndCurr;
  public static int crtR02HBackEndPort;                  //R02 7-0 水平バックポーチ終了カラム
  public static int crtR02HBackEndMask;
  public static int crtR02HBackEndTest;
  public static int crtR02HBackEndCurr;
  public static int crtR03HDispEndPort;                  //R03 7-0 水平映像期間終了カラム
  public static int crtR03HDispEndMask;
  public static int crtR03HDispEndTest;
  public static int crtR03HDispEndCurr;
  public static int crtR04VFrontEndPort;                 //R04 9-0 垂直フロントポーチ終了ラスタ
  public static int crtR04VFrontEndMask;
  public static int crtR04VFrontEndTest;
  public static int crtR04VFrontEndCurr;
  public static int crtR05VSyncEndPort;                  //R05 9-0 垂直同期パルス終了ラスタ
  public static int crtR05VSyncEndMask;
  public static int crtR05VSyncEndTest;
  public static int crtR05VSyncEndCurr;
  public static int crtR06VBackEndPort;                  //R06 9-0 垂直バックポーチ終了ラスタ
  public static int crtR06VBackEndMask;
  public static int crtR06VBackEndTest;
  public static int crtR06VBackEndCurr;
  public static int crtVDispStart;                       //        垂直映像期間開始ラスタ。crtR06VBackEndCurr+1
  public static int crtR07VDispEndPort;                  //R07 9-0 垂直映像期間終了ラスタ
  public static int crtR07VDispEndMask;
  public static int crtR07VDispEndTest;
  public static int crtR07VDispEndCurr;
  public static int crtVIdleStart;                       //        垂直空白期間開始ラスタ。crtR07VDispEndCurr+1
  public static int crtR08Adjust;                        //R08 7-0 外部同期水平アジャスト
  public static int crtR09IRQRasterPort;                 //R09 9-0 IRQラスタ。0=垂直同期パルス開始ラスタ
  public static int crtR09IRQRasterMask;
  public static int crtR09IRQRasterTest;
  public static int crtR09IRQRasterCurr;
  public static int crtR10TxXPort;                       //R10 9-0 テキストX方向スクロール
  public static int crtR10TxXMask;
  public static int crtR10TxXTest;
  public static int crtR10TxXCurr;
  public static int crtR11TxYPort;                       //R11 9-0 テキストY方向スクロール
  public static int crtR11TxYMask;
  public static int crtR11TxYTest;
  public static int crtR11TxYCurr;
  public static int crtR11TxYZero;                       //垂直映像期間開始時のテキストY方向スクロール
  public static int crtR11TxYZeroLast;
  public static final int[] crtR12GrXPort = new int[4];  //[0] R12 9-0 グラフィックX方向スクロール0
  //                                                       [1] R14 8-0 グラフィックX方向スクロール1
  //                                                       [2] R16 8-0 グラフィックX方向スクロール2
  //                                                       [3] R18 8-0 グラフィックX方向スクロール3
  public static final int[] crtR12GrXMask = new int[4];
  public static final int[] crtR12GrXTest = new int[4];
  public static final int[] crtR12GrXCurr = new int[4];
  public static final int[] crtR13GrYPort = new int[4];  //[0] R13 9-0 グラフィックY方向スクロール0
  //                                                       [1] R15 8-0 グラフィックY方向スクロール1
  //                                                       [2] R17 8-0 グラフィックY方向スクロール2
  //                                                       [3] R19 8-0 グラフィックY方向スクロール3
  public static final int[] crtR13GrYMask = new int[4];
  public static final int[] crtR13GrYTest = new int[4];
  public static final int[] crtR13GrYCurr = new int[4];
  public static final int[] crtR13GrYZero = new int[4];  //垂直映像期間開始時のグラフィックY方向スクロール
  public static final int[] crtR13GrYZeroLast = new int[4];
  public static int crtTextStorage;                      //R20 12 テキストストレージ 0=OFF,1=ON
  public static int crtGraphicStorage;                   //R20 11 グラフィックストレージ 0=OFF,1=ON
  public static int crtMemoryModePort;                   //R20 10-8 0=512ドット16色,1=512ドット256色,3=512ドット65536色,4=1024ドット16色,5=1024ドット256色(拡張),7=1024ドット65536色(拡張)
  public static int crtMemoryModeMask;
  public static int crtMemoryModeTest;
  public static int crtMemoryModeCurr;
  public static int crtHighResoPort;                     //R20 4    0=低解像度,1=高解像度
  public static int crtHighResoMask;
  public static int crtHighResoTest;
  public static int crtHighResoCurr;
  public static int crtVResoPort;                        //R20 3-2  垂直解像度
  public static int crtVResoMask;
  public static int crtVResoTest;
  public static int crtVResoCurr;
  public static int crtHResoPort;                        //R20 1-0  水平解像度
  public static int crtHResoMask;
  public static int crtHResoTest;
  public static int crtHResoCurr;
  public static boolean crtCCPlane0;                     //R21 0 true=プレーン0をラスタコピー/高速クリアする
  public static boolean crtCCPlane1;                     //    1 true=プレーン1をラスタコピー/高速クリアする
  public static boolean crtCCPlane2;                     //    2 true=プレーン2をラスタコピー/高速クリアする
  public static boolean crtCCPlane3;                     //    3 true=プレーン3をラスタコピー/高速クリアする
  public static boolean crtSimPlane0;                    //    4 true=プレーン0を同時アクセスする
  public static boolean crtSimPlane1;                    //    5 true=プレーン1を同時アクセスする
  public static boolean crtSimPlane2;                    //    6 true=プレーン2を同時アクセスする
  public static boolean crtSimPlane3;                    //    7 true=プレーン3を同時アクセスする
  public static boolean crtSimAccess;                    //    8 true=同時アクセス有効
  public static boolean crtBitMask;                      //    9 true=ビットマスク有効
  public static int crtR22SrcBlock;                      //R22 15-8 ソースラスタブロック番号
  public static int crtR22DstBlock;                      //    7-0  デスティネーションラスタブロック番号
  public static int crtR23Mask;                          //R23 15-0 ビットマスク。1のビットに書き込まない
  public static boolean crtRasterCopyOn;                 //動作ポート 2        true=次の水平フロントポーチでラスタコピー実行
  public static boolean crtClearStandby;                 //動作ポート 1(write) true=次の垂直表示開始で高速クリア開始
  public static int crtClearFrames;                      //  実行中の高速クリアの残りフレーム数。インターレースのとき2、それ以外は1から始めてデクリメントする

  public static int crtHRLPort;  //0または1。1のとき69.552MHzの3分周と6分周が4分周と8分周に変わる
  public static int crtHRLMask;
  public static int crtHRLTest;
  public static int crtHRLCurr;

  public static boolean crtDuplication;  //true=ラスタ2度読み。crtHighResoCurr==1&&crtVResoCurr==0&&((SpriteScreen.sprReg8ResoCurr&12)!=4)
  public static boolean crtInterlace;  //true=インターレース。crtHighResoCurr+1<=crtVResoCurr
  public static boolean crtSlit;  //true=スリット。crtHighResoCurr==0&&crtVResoCurr==0
  public static boolean crtDupExceptSp;  //true=ラスタ2度読み(スプライトを除く)。crtHighResoCurr==1&&crtVResoCurr==0&&((SpriteScreen.sprReg8ResoCurr&12)==4)
  public static int crtHSyncColumn;  //水平同期パルスカラム数(修正後)
  public static int crtHBackColumn;  //水平バックポーチカラム数(修正後)
  public static int crtHDispColumn;  //水平映像期間カラム数(修正後)
  public static int crtHFrontColumn;  //水平フロントポーチカラム数(修正後)
  public static int crtHTotalColumn;  //水平トータルカラム数(修正後)
  public static double crtVsyncMultiplier;  //垂直同期周波数に掛ける数
  public static int crtColumnTime;  //水平カラム時間(XEiJ.TMR_FREQ単位)
  public static int crtHSyncLength;  //水平同期パルスの長さ(XEiJ.TMR_FREQ単位)。crtColumnTime*(crtR01HSyncEndCurr+1)
  public static int crtHBackLength;  //水平バックポーチの長さ(XEiJ.TMR_FREQ単位)。crtColumnTime*(crtR02HBackEndCurr-crtR01HSyncEndCurr)
  public static int crtHDispLength;  //水平映像期間の長さ(XEiJ.TMR_FREQ単位)。crtColumnTime*(crtR03HDispEndCurr-crtR02HBackEndCurr)
  public static int crtHFrontLength;  //水平フロントポーチの長さ(XEiJ.TMR_FREQ単位)。crtColumnTime*(crtR00HFrontEndCurr-crtR03HDispEndCurr)
  public static int crtHBackDispLength;  //水平バックポーチと水平映像期間の長さ(XEiJ.TMR_FREQ単位)。crtColumnTime*(crtR03HDispEndCurr-crtR01HSyncEndCurr)
  public static long crtTotalLength;  //垂直周期(ミリ)。0=未確定
  public static long crtTotalLengthMNP;  //垂直周期(マイクロナノピコ)。0=未確定

  //  描画のルール
  //    更新されていないラスタのビットマップへの変換を省略する
  //    更新されたラスタを含む矩形をクリッピングエリアとしてペイントする
  //    更新されたラスタは水平映像期間に入った瞬間に1ラスタ分変換する
  //      水平映像期間の途中でパレットレジスタやスクロールレジスタを操作してもそのラスタには反映されない
  //        768x512で256色の画面は作れない
  //      スプライト画面も水平バックポーチが終わる前に書き換えればそのラスタに反映される
  //  更新されたラスタだけを描画する手順
  //    初期化
  //      crtAllStamp=1。2ずつ増やすので0になることはない
  //      for all y
  //        crtRasterStamp[y]=0
  //    画面モードが変更されたりスクロールレジスタやパレットレジスタが操作されて画面の全体が変化した可能性があるとき
  //      crtAllStamp+=2。常に奇数
  //    VRAMやスプライトレジスタが操作されて画面の一部が変化した可能性があるとき
  //      crtRasterStamp[y]=0
  //    垂直映像期間開始ラスタに入るとき
  //      crtDirtyY0=-1。更新なし
  //      crtScreenY=0
  //    描画フレームの垂直映像期間の水平映像期間に入ったとき
  //      crtRasterStamp[crtScreenY]!=crtAllStamp。再描画が必要
  //        crtRasterStamp[crtScreenY]=crtAllStamp
  //        crtDirtyY0<0
  //          crtDirtyY0=crtScreenY。更新あり
  //        crtDirtyY1=crtScreenY
  //        drawRaster(crtScreenY)
  //      crtScreenY++
  //    垂直映像期間終了ラスタから出たとき
  //      crtDirtyY0>=0。更新されたとき
  //        crtDirtyY0からcrtDirtyY1までをrepaintする
  public static int crtDirtyY0;  //垂直映像期間にビットマップが更新された範囲の上端のスクリーンY座標。-1=更新されていない
  public static int crtDirtyY1;  //垂直映像期間にビットマップが更新された範囲の下端のスクリーンY座標。更新された範囲の高さはcrtDirtyY1-crtDirtyY0+1

  public static final int[] crtRasterStamp = new int[1024 + 15];  //ラスタスタンプ。0=VRAMが操作されてこのラスタを再描画する必要がある
  public static int crtAllStamp;  //全再描画スタンプ

  //  768x512ドット256色
  //    作り方
  //      画面モードを768x512ドットにしてグラフィック画面だけ512x512ドット256色にする
  //      512x512ドット256色ページ0に768x512ドット256色の画像の左1/3と中央1/3を描く
  //      512x512ドット256色ページ1に768x512ドット256色の画像の右1/3と中央1/3を描く
  //      0<=y<=511のすべてのラスタについて
  //        256<=x<=511でページ0をOFF、ページ1をON、768<=xでページ0をON、ページ1をOFFにする
  //        256<=x<=511の範囲はページ0とページ1の両方に画像が描かれているのでどこで切り替えてもよいが、
  //        10MHzのとき256<=x<=511の期間は73サイクル、NOP命令18個分しかないので、かなりシビアな処理になる
  //        (768x512ドットの画面は69.552MHzを2分周して作られるので28.755ns/dot)
  //    方針
  //      描画する必要のないラスタをラスタスタンプと全再描画スタンプが一致するかどうかで見分けているが、
  //      この全再描画スタンプを流用する
  //      水平映像期間開始時から終了時までの間に全再描画スタンプが変化したとき、
  //      水平映像期間終了時にラスタの後半を再描画する
  //      分割する必要がないときのオーバーヘッドをなるべく減らす
  //        分割する必要がないときの1ラスタあたりのオーバーヘッド
  //          static変数のコピーが1回
  //          static変数同士の比較が2回
  //          if分岐が2回
  //    手順
  //      水平映像期間開始時
  //        いろいろ
  //        crtBeginningAllStamp=crtAllStamp;
  //        ラスタ描画(src,dst);
  //      水平映像期間終了時
  //        if crtBeginningAllStamp!=crtAllStamp;
  //          ラスタ描画(src,dst);
  //        ラスタ番号++
  //        いろいろ
  //      ラスタ描画(src,dst);
  //        int da=dst<<XEiJ.PNL_BM_OFFSET_BITS;
  //        int db=da+XEiJ.pnlScreenWidth;
  //        if crtBeginningAllStamp!=crtAllStamp
  //          int half=XEiJ.pnlScreenWidth>>4<<3;
  //          sx+=half;
  //          gx1st+=half<<1;
  //          gx2nd+=half<<1;
  //          gx3rd+=half<<1;
  //          gx4th+=half<<1;
  //          tc=tc+(half>>3)&127;
  //          gx+=half;
  //          da+=half;
  //    参考
  //      https://twitter.com/kugimoto0715/status/800231367699116032
  //      ArimacさんのBMPL.X/LPICL.Xの添付ドキュメントX256.DOC
  public static int crtBeginningAllStamp;  //水平映像期間開始時の全再描画スタンプ

  public static int crtRasterNumber;  //ラスタ番号。0=垂直同期パルス開始ラスタ
  public static int crtDataY;  //データY座標
  public static int crtScreenY;  //スクリーンY座標
  public static int crtFrameParity;  //フレームパリティ

  //  水平フロントポーチでアクションを起こすべきラスタかどうかの判別を高速化する
  //  垂直空白期間
  //  crtRasterHashIdle = ((crtRasterCopyOn ? ~CRT_RASTER_HASH_ZERO : CRT_RASTER_HASH_ZERO) |
  //                       (RasterBreakPoint.RBP_ON && RasterBreakPoint.rbpActiveBreakRaster >= 0 ?
  //                        CRT_RASTER_HASH_MSB >>> RasterBreakPoint.rbpActiveBreakRaster : CRT_RASTER_HASH_ZERO) |
  //                       (crtR09IRQRasterCurr <= crtR04VFrontEndCurr ?
  //                        CRT_RASTER_HASH_MSB >>> crtR09IRQRasterCurr |
  //                        CRT_RASTER_HASH_MSB >>> (crtR09IRQRasterCurr < crtR04VFrontEndCurr ? crtR09IRQRasterCurr + 1 : 0) :
  //                        CRT_RASTER_HASH_ZERO);
  //                       CRT_RASTER_HASH_MSB >>> crtVDispStart |
  //                       CRT_RASTER_HASH_MSB >>> crtR04VFrontEndCurr + 1);
  //  垂直映像期間
  //  crtRasterHashDisp = ((crtRasterCopyOn ? ~CRT_RASTER_HASH_ZERO : CRT_RASTER_HASH_ZERO) |
  //                       (RasterBreakPoint.RBP_ON && RasterBreakPoint.rbpActiveBreakRaster >= 0 ?
  //                        CRT_RASTER_HASH_MSB >>> RasterBreakPoint.rbpActiveBreakRaster : CRT_RASTER_HASH_ZERO) |
  //                       (crtR09IRQRasterCurr <= crtR04VFrontEndCurr ?
  //                        CRT_RASTER_HASH_MSB >>> crtR09IRQRasterCurr |
  //                        CRT_RASTER_HASH_MSB >>> (crtR09IRQRasterCurr < crtR04VFrontEndCurr ? crtR09IRQRasterCurr + 1 : 0) :
  //                        CRT_RASTER_HASH_ZERO);
  //                       CRT_RASTER_HASH_MSB >>> crtVIdleStart);
  public static final boolean CRT_RASTER_HASH_ON = true;
  public static final long CRT_RASTER_HASH_ZERO = 0x0000000000000000L;  //crtRasterHashと同じ型の0
  public static final long CRT_RASTER_HASH_MSB  = 0x8000000000000000L;  //crtRasterHashと同じ型のMSBだけセットした値
  public static long crtRasterHashIdle;  //垂直空白期間。intまたはlong
  public static long crtRasterHashDisp;  //垂直映像期間。intまたはlong
  //public static final int CRT_RASTER_HASH_ZERO = 0x00000000;  //crtRasterHashと同じ型の0
  //public static final int CRT_RASTER_HASH_MSB  = 0x80000000;  //crtRasterHashと同じ型のMSBだけセットした値
  //public static int crtRasterHashIdle;  //垂直空白期間。intまたはlong
  //public static int crtRasterHashDisp;  //垂直映像期間。intまたはlong

  public static TickerQueue.Ticker crtTicker;
  public static long crtClock;

  public static long crtContrastClock;  //次にコントラストを変更する時刻
  public static long crtCaptureClock;  //次に画面をキャプチャする時刻
  public static long crtFrameTaskClock;  //Math.min(crtContrastClock,crtCaptureClock)

  //間欠描画
  //  描画するフレームの間隔を空けることで描画の負荷を減らす
  //  間欠間隔 interval>=0
  //    interval=0    デフォルト。すべてのフレームが描画フレーム
  //    interval=1..  描画フレームの後の少なくともintervalフレームを省略フレームとすることで描画フレームの割合を1/(interval+1)以下に抑える
  //                  インターレースの場合は間欠間隔を偶数に切り上げることで偶数フレームと奇数フレームが交互に更新されるようにする
  //  間欠カウンタ counter>=0
  //    counter=0     描画フレーム。画面が更新されたときは描画するフレーム
  //    counter=1～n  省略フレーム。常に描画しないフレーム
  //  描画フレームの垂直映像期間の水平映像期間に入ったとき
  //    ラスタが更新されたとき
  //      描画フレーム(counter==0)のとき
  //        画面の合成と16bit→32bitの変換を行う
  //        更新されたラスタの範囲を記録する
  //      省略フレーム(counter!=0)のとき
  //        何もしない
  //  垂直映像期間終了ラスタから出たとき
  //    描画フレーム(counter==0)のとき
  //      更新されたラスタがあったとき
  //        更新されたラスタの範囲を描画する
  //        counter=interval
  //      更新されたラスタがなかったとき
  //        何もしない
  //    省略フレーム(counter!=0)のとき
  //      counter--
  //!!! XEiJ.PNL_USE_THREADとCRTC.CRT_ENABLE_INTERMITTENTを同時にtrueにしないこと
  public static final boolean CRT_ENABLE_INTERMITTENT = false;  //true=間欠描画を有効にする
  public static int crtIntermittentInterval;  //間欠間隔。描画フレームの間に挟む省略フレームの数の下限
  public static int crtIntermittentCounter;  //間欠カウンタ。0=描画フレーム,1..interval=省略フレーム

  //走査線エフェクト
  enum ScanlineEffect {
    OFF {  //なし。そのままコピー
      @Override public void drawRaster (int screenY) {
        int da = screenY << XEiJ.PNL_BM_OFFSET_BITS;
        System.arraycopy (XEiJ.pnlBM, da - XEiJ.PNL_BM_WIDTH,  //from
                          XEiJ.pnlBM, da,  //to
                          XEiJ.pnlScreenWidth);  //length
      }
    },
    WEAK {  //弱。7/8倍してコピー
      @Override public void drawRaster (int screenY) {
        int da = screenY << XEiJ.PNL_BM_OFFSET_BITS;
        int db = da + XEiJ.pnlScreenWidth;
        while (da < db) {
          int t;
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH    )];
          XEiJ.pnlBM[da    ] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 1)];
          XEiJ.pnlBM[da + 1] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 2)];
          XEiJ.pnlBM[da + 2] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 3)];
          XEiJ.pnlBM[da + 3] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 4)];
          XEiJ.pnlBM[da + 4] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 5)];
          XEiJ.pnlBM[da + 5] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 6)];
          XEiJ.pnlBM[da + 6] = t - ((t >> 3) & 0x001f1f1f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 7)];
          XEiJ.pnlBM[da + 7] = t - ((t >> 3) & 0x001f1f1f);
          da += 8;
        }
      }
    },
    MEDIUM {  //中。3/4倍してコピー
      @Override public void drawRaster (int screenY) {
        int da = screenY << XEiJ.PNL_BM_OFFSET_BITS;
        int db = da + XEiJ.pnlScreenWidth;
        while (da < db) {
          int t;
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH    )];
          XEiJ.pnlBM[da    ] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 1)];
          XEiJ.pnlBM[da + 1] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 2)];
          XEiJ.pnlBM[da + 2] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 3)];
          XEiJ.pnlBM[da + 3] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 4)];
          XEiJ.pnlBM[da + 4] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 5)];
          XEiJ.pnlBM[da + 5] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 6)];
          XEiJ.pnlBM[da + 6] = t - ((t >> 2) & 0x003f3f3f);
          t = XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 7)];
          XEiJ.pnlBM[da + 7] = t - ((t >> 2) & 0x003f3f3f);
          da += 8;
        }
      }
    },
    STRONG {  //強。1/2倍してコピー
      @Override public void drawRaster (int screenY) {
        int da = screenY << XEiJ.PNL_BM_OFFSET_BITS;
        int db = da + XEiJ.pnlScreenWidth;
        while (da < db) {
          XEiJ.pnlBM[da    ] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH    )] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 1] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 1)] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 2] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 2)] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 3] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 3)] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 4] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 4)] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 5] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 5)] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 6] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 6)] >> 1) & 0xff7f7f7f;
          XEiJ.pnlBM[da + 7] = (XEiJ.pnlBM[da - (XEiJ.PNL_BM_WIDTH - 7)] >> 1) & 0xff7f7f7f;
          da += 8;
        }
      }
    },
    BLACK {  //黒
      @Override public void drawRaster (int screenY) {
        int da = screenY << XEiJ.PNL_BM_OFFSET_BITS;
        int db = da + XEiJ.pnlScreenWidth;
        Arrays.fill (XEiJ.pnlBM,  //array
                     da,  //from
                     db,  //to
                     0xff000000);  //value
      }
    };
    public abstract void drawRaster (int screenY);
  }  //enum ScanlineEffect
  public static ScanlineEffect crtScanlineEffect;

  //1024ドットノンインターレース
  //  R04/R05/R06/R07/R09の幅を10ビットから11ビットに拡張する
  //  垂直映像期間のラスタ数R07-R06が1024を超えてはならない
  public static boolean crtEleventhBitRequest;
  public static boolean crtEleventhBit;
  public static int crtVerticalMask;  //0x03ffまたは0x07ff

  //テキスト画面のスクロール
  //  グラフィック画面は球面スクロールでデータのアドレスが1ラスタ毎にループするが、
  //  テキスト画面は円筒スクロールでデータのアドレスが4ラスタ(=1ラスタブロック)毎にループするので、
  //  X方向のスクロール位置に範囲外の値を指定すると画面が乱れる
  //  改造メニューで球面スクロールを選択できる
  public static boolean crtSphericalScrolling;  //false=円筒スクロール,true=球面スクロール
  public static int crtMask3;
  public static int crtMaskMinus4;
  public static int crtMask511;
  public static void crtSetSphericalScrolling (boolean spherical) {
    crtSphericalScrolling = spherical;
    crtMask3 = spherical ? 0 : 3;  //0:3
    crtMaskMinus4 = ~crtMask3;  //-1:-4
    crtMask511 = crtMask3 << 7 | 127;  //127:511
    crtAllStamp += 2;
  }

  //CRTC R00のビット0
  public static boolean crtR00Bit0Zero;  //false=1に固定する,true=0を書き込める

  //crtInit ()
  //  CRTコントローラを初期化する
  public static void crtInit () {
    //if (CRT_EXTENDED_GRAPHIC) {
    //  crtExtendedGraphicRequest = false;
    //}
    //crtR12GrXPort = new int[4];
    //crtR12GrXMask = new int[4];
    //crtR12GrXTest = new int[4];
    //crtR12GrXCurr = new int[4];
    //crtR13GrYPort = new int[4];
    //crtR13GrYMask = new int[4];
    //crtR13GrYTest = new int[4];
    //crtR13GrYCurr = new int[4];
    //crtR13GrYZero = new int[4];
    //crtR13GrYZeroLast = new int[4];
    //crtRasterStamp = new int[1024 + 15];  //1024以降はスプライトコントローラが更新してしまうので追加したダミー

    //走査線エフェクト
    switch (Settings.sgsGetString ("scanline").toLowerCase ()) {
    case "off":
      crtScanlineEffect = ScanlineEffect.OFF;
      break;
    case "weak":
      crtScanlineEffect = ScanlineEffect.WEAK;
      break;
    case "medium":
      crtScanlineEffect = ScanlineEffect.MEDIUM;
      break;
    case "strong":
      crtScanlineEffect = ScanlineEffect.STRONG;
      break;
    case "black":
      crtScanlineEffect = ScanlineEffect.BLACK;
      break;
    }

    //ドットクロックオシレータ
    {
      String[] a = (Settings.sgsGetString ("dotclock") + ",,,0").split (",");
      for (int i = 0; i < 3; i++) {
        int freq = -1;
        try {
          freq = Integer.parseInt (a[i], 10);
        } catch (NumberFormatException nfe) {
        }
        crtFreqsRequest[i] = CRT_MIN_FREQ <= freq && freq <= CRT_MAX_FREQ ? freq : CRT_DEFAULT_FREQS[i];
      }
    }

    //1024ドットノンインターレース
    crtEleventhBitRequest = Settings.sgsGetOnOff ("eleventhbit");

    //テキスト画面のスクロール
    crtSphericalScrolling = Settings.sgsGetOnOff ("sphericalscrolling");
    crtMask3 = 3;
    crtMaskMinus4 = -4;
    crtMask511 = 511;

    //CRTC R00のビット0
    crtR00Bit0Zero = Settings.sgsGetOnOff ("r00bit0zero");

    if (true) {
      crtCCPlane0 = false;
      crtCCPlane1 = false;
      crtCCPlane2 = false;
      crtCCPlane3 = false;
      crtSimPlane0 = false;
      crtSimPlane1 = false;
      crtSimPlane2 = false;
      crtSimPlane3 = false;
      crtSimAccess = false;
      crtBitMask = false;
    }
    crtReset ();
  }  //crtInit()

  //crtTini ()
  //  後始末
  public static void crtTini () {

    //走査線エフェクト
    Settings.sgsPutString ("scanline",
                           crtScanlineEffect == ScanlineEffect.OFF ? "off" :
                           crtScanlineEffect == ScanlineEffect.WEAK ? "weak" :
                           crtScanlineEffect == ScanlineEffect.MEDIUM ? "medium" :
                           crtScanlineEffect == ScanlineEffect.STRONG ? "strong" :
                           crtScanlineEffect == ScanlineEffect.BLACK ? "black" :
                           "");

    //ドットクロックオシレータ
    {
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < 3; i++) {
        if (0 < i) {
          sb.append (',');
        }
        if (crtFreqsRequest[i] != CRT_DEFAULT_FREQS[i]) {
          sb.append (crtFreqsRequest[i]);
        }
      }
      Settings.sgsPutString ("dotclock", sb.toString ());
    }

    //1024ドットノンインターレース
    Settings.sgsPutOnOff ("eleventhbit", crtEleventhBitRequest);

    //テキスト画面のスクロール
    Settings.sgsPutOnOff ("sphericalscrolling", crtSphericalScrolling);

    //CRTC R00のビット0
    Settings.sgsPutOnOff ("r00bit0zero", crtR00Bit0Zero);

  }  //crtTini

  //crtReset ()
  //  リセット
  //  CRTCのレジスタを初期化する
  //  レジスタが設定されてCRT_RESTART_DELAYが経過するまでCRTCの動作を停止する
  //  以下で呼び出される
  //    初期化
  //    MPUのreset命令
  public static void crtReset () {
    if (CRT_EXTENDED_GRAPHIC) {
      crtExtendedGraphicOn = crtExtendedGraphicRequest;
      if (crtExtendedGraphicOn) {
        System.out.println (Multilingual.mlnJapanese ?
                            "拡張グラフィック画面が有効になりました" :
                            "Extended graphic screen has been activated");
      }
    }

    //ドットクロックオシレータ
    for (int i = 0; i < 3; i++) {
      crtFreqs[i] = crtFreqsRequest[i];
    }

    //1024ドットノンインターレース
    crtEleventhBit = crtEleventhBitRequest;
    crtVerticalMask = crtEleventhBit ? 0x07ff : 0x03ff;

    crtR00HFrontEndPort = 0;
    crtR00HFrontEndMask = 0;
    crtR00HFrontEndTest = 0;
    crtR00HFrontEndCurr = 0;
    crtR01HSyncEndPort = 0;
    crtR01HSyncEndMask = 0;
    crtR01HSyncEndTest = 0;
    crtR01HSyncEndCurr = 0;
    crtR02HBackEndPort = 0;
    crtR02HBackEndMask = 0;
    crtR02HBackEndTest = 0;
    crtR02HBackEndCurr = 0;
    crtR03HDispEndPort = 0;
    crtR03HDispEndMask = 0;
    crtR03HDispEndTest = 0;
    crtR03HDispEndCurr = 0;
    crtR04VFrontEndPort = 0;
    crtR04VFrontEndMask = 0;
    crtR04VFrontEndTest = 0;
    crtR04VFrontEndCurr = 0;
    crtR05VSyncEndPort = 0;
    crtR05VSyncEndMask = 0;
    crtR05VSyncEndTest = 0;
    crtR05VSyncEndCurr = 0;
    crtR06VBackEndPort = 0;
    crtR06VBackEndMask = 0;
    crtR06VBackEndTest = 0;
    crtR06VBackEndCurr = 0;
    crtVDispStart = 0;
    crtR07VDispEndPort = 0;
    crtR07VDispEndMask = 0;
    crtR07VDispEndTest = 0;
    crtR07VDispEndCurr = 0;
    crtVIdleStart = 0;
    crtR08Adjust = 0;
    crtR09IRQRasterPort = 1023;
    crtR09IRQRasterMask = 0;
    crtR09IRQRasterTest = 1023;
    crtR09IRQRasterCurr = 1023;
    crtR10TxXPort = 0;
    crtR10TxXMask = 0;
    crtR10TxXTest = 0;
    crtR10TxXCurr = 0;
    crtR11TxYPort = 0;
    crtR11TxYMask = 0;
    crtR11TxYTest = 0;
    crtR11TxYCurr = 0;
    crtR11TxYZero = 0;
    crtR11TxYZeroLast = -1;
    for (int i = 0; i < 4; i++) {
      crtR12GrXPort[i] = 0;
      crtR12GrXMask[i] = 0;
      crtR12GrXTest[i] = 0;
      crtR12GrXCurr[i] = 0;
      crtR13GrYPort[i] = 0;
      crtR13GrYMask[i] = 0;
      crtR13GrYTest[i] = 0;
      crtR13GrYCurr[i] = 0;
      crtR13GrYZero[i] = 0;
      crtR13GrYZeroLast[i] = -1;
    }
    crtTextStorage = 0;
    crtGraphicStorage = 0;
    crtMemoryModePort = 0;
    crtMemoryModeMask = 0;
    crtMemoryModeTest = 0;
    crtMemoryModeCurr = 0;
    crtHighResoPort = 0;
    crtHighResoMask = 0;
    crtHighResoTest = 0;
    crtHighResoCurr = 0;
    crtVResoPort = 0;
    crtVResoMask = 0;
    crtVResoTest = 0;
    crtVResoCurr = 0;
    crtHResoPort = 0;
    crtHResoMask = 0;
    crtHResoTest = 0;
    crtHResoCurr = 0;
    if (false) {
      crtCCPlane0 = false;
      crtCCPlane1 = false;
      crtCCPlane2 = false;
      crtCCPlane3 = false;
      crtSimPlane0 = false;
      crtSimPlane1 = false;
      crtSimPlane2 = false;
      crtSimPlane3 = false;
      crtSimAccess = false;
      crtBitMask = false;
    }
    crtR22SrcBlock = 0;
    crtR22DstBlock = 0;
    crtR23Mask = 0x0000;
    crtRasterCopyOn = false;
    crtClearStandby = false;
    crtClearFrames = 0;

    crtHRLPort = 0;
    crtHRLMask = 0;
    crtHRLTest = 0;
    crtHRLCurr = 0;
    XEiJ.pnlStereoscopicShutter = 0;  //左右OPEN
    crtDuplication = false;
    crtInterlace = false;
    crtSlit = false;
    crtDupExceptSp = false;
    crtHSyncColumn = 2;
    crtHBackColumn = 2;
    crtHDispColumn = 2;
    crtHFrontColumn = 2;
    crtHTotalColumn = 8;
    crtVsyncMultiplier = 1.0;
    crtColumnTime = 0;
    crtHSyncLength = 0;
    crtHBackLength = 0;
    crtHDispLength = 0;
    crtHFrontLength = 0;
    crtHBackDispLength = 0;
    crtTotalLength = 0L;
    crtTotalLengthMNP = 0L;

    if (!XEiJ.PNL_USE_THREAD) {
      crtDirtyY0 = -1;
      crtDirtyY1 = -1;
    }

    Arrays.fill (crtRasterStamp, 0);
    crtAllStamp = 1;  //初回は全再描画

    crtBeginningAllStamp = 1;

    crtRasterNumber = 0;
    crtDataY = 0;
    crtScreenY = 0;
    crtFrameParity = 0;

    crtRasterHashIdle = CRT_RASTER_HASH_ZERO;
    crtRasterHashDisp = CRT_RASTER_HASH_ZERO;

    crtContrastClock = XEiJ.FAR_FUTURE;
    crtCaptureClock = XEiJ.FAR_FUTURE;
    crtFrameTaskClock = Math.min (crtContrastClock, crtCaptureClock);

    if (CRT_ENABLE_INTERMITTENT) {  //間欠描画
      //crtIntermittentInterval = 0;
      crtIntermittentCounter = 0;
    }

    if (crtTicker != null) {
      TickerQueue.tkqRemove (crtTicker);
      crtTicker = null;
    }
    crtClock = XEiJ.FAR_FUTURE;  //停止

  }  //crtReset()

  //crtRestart ()
  //  CRTCのレジスタが設定されたのでCRT_RESTART_DELAY後にInitialStageを開始する
  public static void crtRestart () {
    if (crtTicker != null) {
      TickerQueue.tkqRemove (crtTicker);
    }
    TickerQueue.tkqAdd (crtTicker = InitialStage, crtClock = XEiJ.mpuClockTime + CRT_RESTART_DELAY);  //スクリーンの初期化へ
  }  //crtRestart()

  //crtStereoscopicStart ()
  //  垂直映像開始で
  public static void crtStereoscopicStart () {
    if (XEiJ.PNL_USE_THREAD) {
      XEiJ.pnlBM = (XEiJ.pnlStereoscopicShutter != 1 ? XEiJ.pnlBMLeftArray[XEiJ.pnlBMWrite & 3] :  //0=3=左右OPENまたは2=左OPENのとき左に描く
                    XEiJ.pnlBMRightArray[XEiJ.pnlBMWrite & 3]);  //1=右OPENのとき右に描く
    } else {
      XEiJ.pnlBM = (XEiJ.pnlStereoscopicShutter != 1 ? XEiJ.pnlBMLeft :  //0=3=左右OPENまたは2=左OPENのとき左に描く
                    XEiJ.pnlBMRight);  //1=右OPENのとき右に描く
    }
    crtAllStamp += 2;
  }

  //crtStereoscopicDrawRaster (screenY)
  //  drawRasterの後で
  public static void crtStereoscopicDrawRaster (int screenY) {
    if (XEiJ.pnlStereoscopicShutter == 0 ||
        XEiJ.pnlStereoscopicShutter == 3) {  //0=3=左右OPENのとき
      if (XEiJ.PNL_USE_THREAD) {
        System.arraycopy (XEiJ.pnlBMLeftArray[XEiJ.pnlBMWrite & 3], screenY << XEiJ.PNL_BM_OFFSET_BITS,
                          XEiJ.pnlBMRightArray[XEiJ.pnlBMWrite & 3], screenY << XEiJ.PNL_BM_OFFSET_BITS,
                          XEiJ.pnlScreenWidth);  //左から右へコピー
      } else {
        System.arraycopy (XEiJ.pnlBMLeft, screenY << XEiJ.PNL_BM_OFFSET_BITS,
                          XEiJ.pnlBMRight, screenY << XEiJ.PNL_BM_OFFSET_BITS,
                          XEiJ.pnlScreenWidth);  //左から右へコピー
      }
    }
  }

  //crtUpdateScreen ()
  //  スクリーンを更新する
  public static void crtUpdateScreen () {
    if (!XEiJ.PNL_USE_THREAD) {
      if (XEiJ.pnlZoomRatioOutY == 1 << 16) {  //拡大なし
        XEiJ.pnlPanel.repaint (0L, XEiJ.pnlScreenX1, XEiJ.pnlScreenY1 + crtDirtyY0, XEiJ.pnlScreenX4 - XEiJ.pnlScreenX1, crtDirtyY1 - crtDirtyY0 + 1);
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn && XEiJ.pnlStereoscopicMethod == XEiJ.PNL_TOP_AND_BOTTOM) {
          XEiJ.pnlPanel.repaint (0L, XEiJ.pnlScreenX1, XEiJ.pnlScreenY3 + crtDirtyY0, XEiJ.pnlScreenX4 - XEiJ.pnlScreenX1, crtDirtyY1 - crtDirtyY0 + 1);
        }
      } else {  //拡大あり
        int y0 = (crtDirtyY0 - 1) * XEiJ.pnlZoomRatioOutY >> 16;
        int y1 = (crtDirtyY1 + 2) * (XEiJ.pnlZoomRatioOutY + 1) >> 16;
        XEiJ.pnlPanel.repaint (0L, XEiJ.pnlScreenX1, XEiJ.pnlScreenY1 + y0, XEiJ.pnlScreenX4 - XEiJ.pnlScreenX1, y1 - y0);
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn && XEiJ.pnlStereoscopicMethod == XEiJ.PNL_TOP_AND_BOTTOM) {
          XEiJ.pnlPanel.repaint (0L, XEiJ.pnlScreenX1, XEiJ.pnlScreenY3 + y0, XEiJ.pnlScreenX4 - XEiJ.pnlScreenX1, y1 - y0);
        }
      }
      crtDirtyY0 = -1;
    }
  }

  //crtRepaint ()
  //  再描画
  //  MPUが止まっていても再描画する
  //  ダーティラスタのマークはそのままでMFPとのやりとりも行わない
  //  ラスタ割り込みを使用して作られている画面は再現できない
  public static void crtRepaint () {
    crtBeginningAllStamp = crtAllStamp;
    int l = Math.max (0, Math.min (1024, crtR07VDispEndCurr - crtR06VBackEndCurr));
    if (crtDuplication) {  //ラスタ2度読み
      for (int screenY = 0; screenY < l; screenY += 2) {
        if (SpriteScreen.SPR_THREE_STEPS) {
          SpriteScreen.sprStep1 (screenY >> 1);
          SpriteScreen.sprStep2 (screenY >> 1);
        }
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (screenY >> 1, screenY, false);  //スクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (screenY >> 1, screenY, false);  //スクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (screenY);
        }
        //偶数ラスタを奇数ラスタにコピーする
        System.arraycopy (XEiJ.pnlBM, screenY << XEiJ.PNL_BM_OFFSET_BITS, XEiJ.pnlBM, screenY + 1 << XEiJ.PNL_BM_OFFSET_BITS, XEiJ.pnlScreenWidth);
      }
    } else if (crtSlit) {  //スリット
      for (int screenY = 0; screenY < l; screenY += 2) {
        if (SpriteScreen.SPR_THREE_STEPS) {
          SpriteScreen.sprStep1 (screenY >> 1);
          SpriteScreen.sprStep2 (screenY >> 1);
        }
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (screenY >> 1, screenY, false);  //スクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (screenY >> 1, screenY, false);  //スクリーンY座標へ描画
        }
        crtScanlineEffect.drawRaster (screenY + 1);  //走査線エフェクト
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (screenY);
          crtStereoscopicDrawRaster (screenY + 1);
        }
      }
    } else if (crtDupExceptSp) {  //ラスタ2度読み(スプライトを除く)
      for (int screenY = 0; screenY < l; screenY++) {
        if (SpriteScreen.SPR_THREE_STEPS) {
          SpriteScreen.sprStep1 (screenY);
          SpriteScreen.sprStep2 (screenY);
        }
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (screenY >> 1, screenY, false);  //スクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (screenY >> 1, screenY, false);  //スクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (screenY);
        }
      }
    } else {  //ノーマル,インターレース
      for (int screenY = 0; screenY < l; screenY++) {
        if (SpriteScreen.SPR_THREE_STEPS) {
          SpriteScreen.sprStep1 (screenY);
          SpriteScreen.sprStep2 (screenY);
        }
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (screenY, screenY, false);  //スクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (screenY, screenY, false);  //スクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (screenY);
        }
      }
    }
    XEiJ.pnlPanel.repaint (0L, XEiJ.pnlScreenX1, XEiJ.pnlScreenY1, XEiJ.pnlScreenX4 - XEiJ.pnlScreenX1, l);
    if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn && XEiJ.pnlStereoscopicMethod == XEiJ.PNL_TOP_AND_BOTTOM) {
      XEiJ.pnlPanel.repaint (0L, XEiJ.pnlScreenX1, XEiJ.pnlScreenY3, XEiJ.pnlScreenX4 - XEiJ.pnlScreenX1, l);
    }
  }  //crtRepaint()

  //crtDoFrameTask ()
  //  垂直同期パルスに行う処理
  //  垂直同期パルスに入ったときXEiJ.mpuClockTime>=crtFrameTaskClockならば呼び出す
  //  コントラストの調整
  //  画面キャプチャ
  public static void crtDoFrameTask () {
    if (XEiJ.mpuClockTime >= crtContrastClock) {
      if (VideoController.vcnUpdateContrast ()) {
        crtContrastClock += VideoController.VCN_CONTRAST_DELAY;
      } else {
        crtContrastClock = XEiJ.FAR_FUTURE;
      }
    }
    if (XEiJ.mpuClockTime >= crtCaptureClock) {
      GIFAnimation.gifCaptureFrame ();
    }
    crtFrameTaskClock = Math.min (crtContrastClock, crtCaptureClock);
  }  //crtDoFrameTask()

  //crtSetMemoryMode (textStorage, graphicStorage, memoryMode)
  //  テキストストレージ(R20 12)
  //  グラフィックストレージ(R20 11)
  //  メモリモード(R20 10-8)を変更する
  public static void crtSetMemoryMode (int textStorage, int graphicStorage, int memoryMode) {
    boolean updateMemoryMap = false;  //メモリマップを更新するか
    textStorage &= 1;
    if (crtTextStorage != textStorage) {  //テキストストレージを変更する
      crtTextStorage = textStorage;
    }
    graphicStorage &= 1;
    if (crtGraphicStorage != graphicStorage) {  //グラフィックストレージを変更する
      crtGraphicStorage = graphicStorage;
      updateMemoryMap = true;  //メモリマップを更新する
    }
    memoryMode &= 7;
    crtMemoryModePort = memoryMode;
    int curr = crtMemoryModeMask == 0 ? crtMemoryModePort : crtMemoryModeTest;
    if (crtMemoryModeCurr != curr) {  //メモリモードを変更する
      crtMemoryModeCurr = curr;
      updateMemoryMap = true;  //メモリマップを更新する
    }
    if (updateMemoryMap) {  //メモリマップを更新する
      if (crtGraphicStorage != 0) {  //グラフィックストレージON
        if (CRT_EXTENDED_GRAPHIC && crtExtendedGraphicOn &&  //拡張グラフィック画面がONかつ
            (crtMemoryModeCurr == 5 || crtMemoryModeCurr == 7)) {  //メモリモード5または7のとき
          //メモリモード7相当
          XEiJ.busSuper (MemoryMappedDevice.MMD_GJ0, 0x00c00000, 0x00e00000);
        } else {
          //メモリモード3相当
          XEiJ.busSuper (MemoryMappedDevice.MMD_GG0, 0x00c00000, 0x00c80000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_NUL, 0x00c80000, 0x00e00000);
        }
      } else {  //グラフィックストレージOFF
        switch (crtMemoryModeCurr) {
        case 0:  //メモリモード0
          //512ドット16色
          XEiJ.busSuper (MemoryMappedDevice.MMD_GE0, 0x00c00000, 0x00c80000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_GE1, 0x00c80000, 0x00d00000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_GE2, 0x00d00000, 0x00d80000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_GE3, 0x00d80000, 0x00e00000);
          break;
        case 1:  //メモリモード1
          //512ドット256色
          XEiJ.busSuper (MemoryMappedDevice.MMD_GF0, 0x00c00000, 0x00c80000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_GF1, 0x00c80000, 0x00d00000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_NUL, 0x00d00000, 0x00e00000);
          break;
        case 2:  //メモリモード2
          //メモリモード2
          XEiJ.busSuper (MemoryMappedDevice.MMD_GM2, 0x00c00000, 0x00e00000);
          break;
        case 3:  //メモリモード3
          //512ドット65536色
          XEiJ.busSuper (MemoryMappedDevice.MMD_GG0, 0x00c00000, 0x00c80000);
          XEiJ.busSuper (MemoryMappedDevice.MMD_NUL, 0x00c80000, 0x00e00000);
          break;
        case 4:  //メモリモード4
          //1024ドット16色
          XEiJ.busSuper (MemoryMappedDevice.MMD_GH0, 0x00c00000, 0x00e00000);
          break;
        case 5:  //メモリモード5
          if (CRT_EXTENDED_GRAPHIC && crtExtendedGraphicOn) {
            //1024ドット256色(拡張)
            XEiJ.busSuper (MemoryMappedDevice.MMD_GI0, 0x00c00000, 0x00e00000);
          } else {
            //1024ドット16色
            XEiJ.busSuper (MemoryMappedDevice.MMD_GH0, 0x00c00000, 0x00e00000);
          }
          break;
        case 6:  //メモリモード6
          //1024ドット16色
          XEiJ.busSuper (MemoryMappedDevice.MMD_GH0, 0x00c00000, 0x00e00000);
          break;
        case 7:  //メモリモード7
          if (CRT_EXTENDED_GRAPHIC && crtExtendedGraphicOn) {
            //1024ドット65536色(拡張)
            XEiJ.busSuper (MemoryMappedDevice.MMD_GJ0, 0x00c00000, 0x00e00000);
          } else {
            //1024ドット16色
            XEiJ.busSuper (MemoryMappedDevice.MMD_GH0, 0x00c00000, 0x00e00000);
          }
          break;
        }
      }
    }
  }  //crtSetMemoryMode

  //crtUpdateRasterHash ()
  //  crtRasterHashIdle,crtRasterHashDispを設定する
  //    crtRasterCopyOn
  //    RasterBreakPoint.rbpActiveBreakRaster
  //    crtR09IRQRasterCurr
  //    crtVDispStart
  //    crtR04VFrontEndCurr
  //    crtVIdleStart
  //  が更新されたときに呼び出す
  //  crtR04VFrontEndCurrがcrtRasterNumberよりも小さい値に変更されると通り過ぎてしまうので必ずcrtRasterNumber=0からリスタートさせること
  public static void crtUpdateRasterHash () {
    if (CRT_RASTER_HASH_ON) {
      long t = crtRasterCopyOn ? ~CRT_RASTER_HASH_ZERO : CRT_RASTER_HASH_ZERO;  //intまたはlong
      //int t = crtRasterCopyOn ? ~CRT_RASTER_HASH_ZERO : CRT_RASTER_HASH_ZERO;  //intまたはlong
      if (RasterBreakPoint.RBP_ON && RasterBreakPoint.rbpActiveBreakRaster >= 0) {
        t |= CRT_RASTER_HASH_MSB >>> RasterBreakPoint.rbpActiveBreakRaster;
      }
      if (crtR09IRQRasterCurr <= crtR04VFrontEndCurr) {
        t |= (CRT_RASTER_HASH_MSB >>> crtR09IRQRasterCurr |
              CRT_RASTER_HASH_MSB >>> (crtR09IRQRasterCurr < crtR04VFrontEndCurr ? crtR09IRQRasterCurr + 1 : 0));
      }
      crtRasterHashIdle = (t |
                           CRT_RASTER_HASH_MSB >>> crtVDispStart |
                           CRT_RASTER_HASH_MSB >>> crtR04VFrontEndCurr + 1);
      crtRasterHashDisp = (t |
                           CRT_RASTER_HASH_MSB >>> crtVIdleStart);
    }
  }  //crtUpdateRasterHash()

  //高速クリア
  //  手順
  //    (1)$00E8002Aを保存する
  //    (2)クリアページを設定する。$00E8002Aに#$000Fなどを書き込む
  //    (3)高速クリアを予約する。$00E80480に#$0002を書き込む
  //    (4)高速クリアの開始を待つ。$00E80480と#$0002のANDが#$0000でなくなるまで待つ
  //    (5)高速クリアの終了を待つ。$00E80480と#$0002のANDが#$0000になるまで待つ
  //    (6)$00E8002Aを復元する
  //  動作ポートのビット1
  //    書き込み
  //      0のとき1を書き込むと高速クリアが予約され、自動で開始、終了する
  //      0を書き込んでもキャンセルや中断はできない
  //    読み出し
  //      高速クリアが予約されているとき(インターレースのときは偶数フレームの)VDISP=0→1(の一瞬前)で1になる
  //      1フレーム(インターレースのときは2フレーム)後のVDISP=0→1(の一瞬前)で0に戻る
  //      0に戻った直後に1を書き込んだとき次のフレームの高速クリアは(ほとんど)失敗する
  //  ページ選択
  //    実画面サイズが512x512のとき
  //      高速クリアページセレクトの各ビットが4ビットページに対応する
  //      垂直映像期間の途中で変更できる
  //    実画面サイズが1024x1024のとき
  //      高速クリアページセレクトは無効で常にすべてのページが消去される
  //  範囲
  //    高速クリアされる範囲は、画面サイズ、実画面サイズ、ページ0のスクロール位置で決まる
  //      ページ0は表示されている範囲がクリアされるが、ページ1,2,3は表示されている範囲がクリアされるとは限らない
  //    水平方向のスクロール位置が奇数のときは偶数に切り捨てられる
  //      256x256で水平方向のスクロール位置が奇数のとき、画面の右端にクリアされないドットが残る
  //    クリアされる範囲の幅は画面の幅と512の小さい方
  //      x=512を跨ぐときx=0に戻る
  //      実画面サイズが1024ドットのとき2箇所クリアする
  //    クリアされる範囲の高さは画面の高さ
  //      画面の高さが256ドットのときラスタ2度読みでもクリアされる範囲の高さは256ドット

  //crtRapidClear (y)
  //  高速クリア実行
  public static void crtRapidClear (int y) {
    y += crtR13GrYZero[0];
    int x = crtR12GrXCurr[0] & (512 - 2);  //x座標開始位置
    int w = 8 * crtHDispColumn;  //画面の幅
    if (512 <= w) {
      x = 0;
      w = 512;
    }
    if (crtMemoryModeCurr < 4) {  //実画面サイズ512ドット
      int i = (y & 511) << 9;  //(0,y)のインデックス
      if (x + w <= 512) {  //x=512を跨がない
        //    i  i+x          i+x+w    i+512
        //    |   <------------->       |
        if (crtCCPlane0) {
          Arrays.fill (GraphicScreen.graM4,           i + x,           i + x + w, (byte) 0);
        }
        if (crtCCPlane1) {
          Arrays.fill (GraphicScreen.graM4, 0x40000 + i + x, 0x40000 + i + x + w, (byte) 0);
        }
        if (crtCCPlane2) {
          Arrays.fill (GraphicScreen.graM4, 0x80000 + i + x, 0x80000 + i + x + w, (byte) 0);
        }
        if (crtCCPlane3) {
          Arrays.fill (GraphicScreen.graM4, 0xc0000 + i + x, 0xc0000 + i + x + w, (byte) 0);
        }
      } else {  //x=512を跨ぐ
        //    i    i+x+w-512     i+x   i+512  i+x+w
        //    |------->           <-----|------->
        if (crtCCPlane0) {
          Arrays.fill (GraphicScreen.graM4,           i + x,           i +         512, (byte) 0);
          Arrays.fill (GraphicScreen.graM4,           i    ,           i + x + w - 512, (byte) 0);
        }
        if (crtCCPlane1) {
          Arrays.fill (GraphicScreen.graM4, 0x40000 + i + x, 0x40000 + i +         512, (byte) 0);
          Arrays.fill (GraphicScreen.graM4, 0x40000 + i    , 0x40000 + i + x + w - 512, (byte) 0);
        }
        if (crtCCPlane2) {
          Arrays.fill (GraphicScreen.graM4, 0x80000 + i + x, 0x80000 + i +         512, (byte) 0);
          Arrays.fill (GraphicScreen.graM4, 0x80000 + i    , 0x80000 + i + x + w - 512, (byte) 0);
        }
        if (crtCCPlane3) {
          Arrays.fill (GraphicScreen.graM4, 0xc0000 + i + x, 0xc0000 + i +         512, (byte) 0);
          Arrays.fill (GraphicScreen.graM4, 0xc0000 + i    , 0xc0000 + i + x + w - 512, (byte) 0);
        }
      }
    } else {  //実画面サイズ1024ドット
      int i = (y & 512) << 10 | (y & 511) << 9;  //(0,y)のインデックス
      if (x + w <= 512) {  //x=512を跨がない
        //    i  i+x          i+x+w    i+512
        //    |   <------------->       |
        for (int k = 0; k < 2; k++) {  //2箇所クリアする
          Arrays.fill (GraphicScreen.graM4,            i + x,            i + x + w, (byte) 0);
          if (crtMemoryModeCurr == 5 || crtMemoryModeCurr == 7) {
            Arrays.fill (GraphicScreen.graM4, 0x100000 + i + x, 0x100000 + i + x + w, (byte) 0);
            if (crtMemoryModeCurr == 7) {
              Arrays.fill (GraphicScreen.graM4, 0x200000 + i + x, 0x200000 + i + x + w, (byte) 0);
              Arrays.fill (GraphicScreen.graM4, 0x300000 + i + x, 0x300000 + i + x + w, (byte) 0);
            }
          }
          i += 512 << 9;  //(512,y)のインデックス
        }
      } else {  //x=512を跨ぐ
        //    i    i+x+w-512     i+x   i+512  i+x+w
        //    |------->           <-----|------->
        for (int k = 0; k < 2; k++) {  //2箇所クリアする
          Arrays.fill (GraphicScreen.graM4,            i + x,            i +         512, (byte) 0);
          Arrays.fill (GraphicScreen.graM4,            i    ,            i + x + w - 512, (byte) 0);
          if (crtMemoryModeCurr == 5 || crtMemoryModeCurr == 7) {
            Arrays.fill (GraphicScreen.graM4, 0x100000 + i + x, 0x100000 + i +         512, (byte) 0);
            Arrays.fill (GraphicScreen.graM4, 0x100000 + i    , 0x100000 + i + x + w - 512, (byte) 0);
            if (crtMemoryModeCurr == 7) {
              Arrays.fill (GraphicScreen.graM4, 0x200000 + i + x, 0x200000 + i +         512, (byte) 0);
              Arrays.fill (GraphicScreen.graM4, 0x200000 + i    , 0x200000 + i + x + w - 512, (byte) 0);
              Arrays.fill (GraphicScreen.graM4, 0x300000 + i + x, 0x300000 + i +         512, (byte) 0);
              Arrays.fill (GraphicScreen.graM4, 0x300000 + i    , 0x300000 + i + x + w - 512, (byte) 0);
            }
          }
          i += 512 << 9;  //(512,y)のインデックス
        }
      }
    }
  }  //crtRapidClear(int)

  //crtDoRasterCopy ()
  //  ラスタコピー実行
  public static void crtDoRasterCopy () {
    int srcOffset = crtR22SrcBlock << 9;
    int dstOffset = crtR22DstBlock << 9;
    if (crtCCPlane0) {
      System.arraycopy (MainMemory.mmrM8, 0x00e00000 + srcOffset, MainMemory.mmrM8, 0x00e00000 + dstOffset, 512);
    }
    if (crtCCPlane1) {
      System.arraycopy (MainMemory.mmrM8, 0x00e20000 + srcOffset, MainMemory.mmrM8, 0x00e20000 + dstOffset, 512);
    }
    if (crtCCPlane2) {
      System.arraycopy (MainMemory.mmrM8, 0x00e40000 + srcOffset, MainMemory.mmrM8, 0x00e40000 + dstOffset, 512);
    }
    if (crtCCPlane3) {
      System.arraycopy (MainMemory.mmrM8, 0x00e60000 + srcOffset, MainMemory.mmrM8, 0x00e60000 + dstOffset, 512);
    }
    int y = (dstOffset >> 7) - crtR11TxYZero;
    crtRasterStamp[y     & 1023] = 0;
    crtRasterStamp[y + 1 & 1023] = 0;
    crtRasterStamp[y + 2 & 1023] = 0;
    crtRasterStamp[y + 3 & 1023] = 0;
  }  //crtDoRasterCopy()



  //========================================================================================
  //
  //  CRTCのステージ
  //
  //    ラスタ(水平周期)
  //      水平フロントポーチと水平同期パルスと水平バックポーチと水平映像期間を合わせた期間
  //
  //    フレーム(垂直周期)
  //      垂直空白期間(垂直フロントポーチと垂直同期パルスと垂直バックポーチ)と垂直映像期間を合わせた期間
  //      垂直映像期間と垂直フロントポーチはそれぞれ少なくとも1ラスタ以上必要
  //        垂直フロントポーチが1ラスタ以上必要なのは垂直映像期間にラスタ番号のラップアラウンドを行なっていないため
  //        垂直映像期間にラスタ番号のラップアラウンドを行う場合でも垂直空白期間は少なくとも1ラスタ以上必要
  //
  //    ラスタ番号
  //      ラスタの番号。0からラスタ数-1まで。0は垂直同期パルス開始ラスタ、ラスタ数-1は垂直フロントポーチ終了ラスタ
  //      遷移とIRQ信号の生成に用いる
  //
  //    フレームパリティ
  //      フレーム番号の下位1bit。0または1
  //      インターレースのときにフレーム毎のデータY座標とスクリーンY座標の初期値として使う
  //      フレームの末尾で反転する
  //
  //    データY座標
  //      ラスタに供給されるデータのY座標
  //
  //    スクリーンY座標
  //      ラスタを表示するスクリーン上のY座標
  //
  //
  //    ノーマル
  //      フレームの先頭でデータY座標とスクリーンY座標を0で初期化する
  //      ラスタを描画してからスクリーンY座標を1増やす
  //      ラスタの末尾でデータY座標を1増やす
  //      垂直映像期間の先頭で高速クリアの要求があるとき高速クリアカウンタを1で初期化する
  //
  //    ラスタ2度読み
  //      フレームの先頭でデータY座標とスクリーンY座標を0で初期化する
  //      偶数ラスタを描画してからスクリーンY座標を1増やす
  //      偶数ラスタの末尾ではデータY座標を増やさない
  //      奇数ラスタを描画してからスクリーンY座標を1増やす
  //      奇数ラスタの末尾でデータY座標を1増やす
  //      垂直映像期間の先頭で高速クリアの要求があるとき高速クリアカウンタを1で初期化する
  //
  //    インターレース
  //      フレームの先頭でデータY座標とスクリーンY座標をフレームパリティで初期化する
  //      ラスタを描画してからスクリーンY座標を2増やす
  //      ラスタの末尾でデータY座標を2増やす
  //      (偶数フレームは偶数ラスタだけ、奇数フレームは奇数ラスタだけ描画する)
  //      偶数フレームの垂直映像期間の先頭で高速クリアの要求があるとき高速クリアカウンタを2で初期化する
  //
  //    スリット
  //      フレームの先頭でデータY座標とスクリーンY座標を0で初期化する
  //      ラスタを描画してから輝度を落としてコピーし、スクリーンY座標を2増やす
  //      ラスタの末尾でデータY座標を1増やす
  //      垂直映像期間の先頭で高速クリアの要求があるとき高速クリアカウンタを1で初期化する
  //
  //
  //                                         データY座標     スクリーンY座標
  //    ───────────────────────────────────
  //        ノーマル          初期化               0                 0
  //                       ラスタ描画後           +1                +1
  //    ───────────────────────────────────
  //      ラスタ2度読み       初期化               0                 0
  //                     偶数ラスタ描画後         +0                +1
  //                     奇数ラスタ描画後         +1                +1
  //    ───────────────────────────────────
  //     インターレース       初期化       フレームパリティ  フレームパリティ
  //                       ラスタ描画後           +2                +2
  //    ───────────────────────────────────
  //        スリット          初期化               0                 0
  //                       ラスタ描画後           +1                +2
  //    ───────────────────────────────────
  //
  //
  //    水平フロントポーチ
  //      ＋水平フロントポーチの長さ
  //      ラスタ番号を1増やす
  //      [垂直空白期間のとき]
  //        ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
  //          ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
  //      ラスタコピースイッチがONのとき
  //        ラスタコピー実行
  //      ブレークラスタのとき
  //        ラスタブレークをかける
  //      IRQ信号を更新
  //      IRQ信号が変化したとき
  //        IRQ信号が0になったとき
  //        | IRQラスタでラスタブレークをかけるとき
  //        |   ブレークラスタではないとき(ブレークラスタとIRQラスタが同じときは既にラスタブレークがかかっている)
  //        |      ラスタブレークをかける
  //        | IRQ開始
  //        IRQ信号が0でなくなったとき
  //          IRQ終了
  //      [垂直空白期間のとき]
  //      | 垂直映像期間開始ラスタではないとき
  //      | | [描画フレーム(間欠カウンタ==0)のとき]
  //      | | | →描画フレームの垂直空白期間の水平同期パルス
  //      | | [省略フレーム(間欠カウンタ!=0)のとき]
  //      | |   →省略フレームの垂直空白期間の水平同期パルス
  //      | 垂直映像期間開始ラスタのとき
  //      |   垂直映像期間開始
  //      |   テキストY方向スクロールを保存
  //      |   グラフィックY方向スクロールを保存
  //      |   [インターレースではないとき]
  //      |   | データY座標を0で初期化
  //      |   [インターレースのとき]
  //      |     データY座標をフレームパリティで初期化
  //      |   [高速クリアカウンタが0ではないとき]
  //      |   | 高速クリアカウンタを1減らす
  //      |   [インターレースではないとき]
  //      |   | 高速クリアの要求があるとき
  //      |   |   高速クリアカウンタを1で初期化
  //      |   [インターレースのとき]
  //      |     高速クリアの要求があり、偶数フレームのとき]
  //      |       高速クリアカウンタを2で初期化
  //      |   [描画フレーム(間欠カウンタ==0)のとき]
  //      |   | [インターレースではないとき]
  //      |   | | スクリーンY座標を0で初期化
  //      |   | [インターレースのとき]
  //      |   |   スクリーンY座標をフレームパリティで初期化
  //      |   | ダーティフラグをクリア
  //      |   | →描画フレームの垂直映像期間の水平同期パルス
  //      |   [省略フレーム(間欠カウンタ!=0)のとき]
  //      |     →省略フレームの垂直映像期間の水平同期パルス
  //      [垂直映像期間のとき]
  //        垂直空白期間開始ラスタではないとき
  //        | [描画フレーム(間欠カウンタ==0)のとき]
  //        | | [全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき]
  //        | | | データY座標からスクリーンY座標へ描画
  //        | | [ノーマルのとき]
  //        | | | データY座標を1増やす
  //        | | | スクリーンY座標を1増やす
  //        | | [ラスタ2度読みのとき]
  //        | | | スクリーンY座標を1増やす
  //        | | | [偶数ラスタのとき]
  //        | | |   データY座標を1増やす
  //        | | [インターレースのとき]
  //        | | | スクリーンY座標を2増やす
  //        | | | データY座標を2増やす
  //        | | [スリットのとき]
  //        | |   スクリーンY座標を2増やす
  //        | |   データY座標を1増やす
  //        | | →描画フレームの垂直映像期間の水平同期パルス
  //        | [省略フレーム(間欠カウンタ!=0)のとき]
  //        |   →省略フレームの垂直映像期間の水平同期パルス
  //        垂直空白期間開始ラスタのとき
  //          [全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき]
  //          | データY座標からスクリーンY座標へ描画
  //          垂直映像期間終了
  //          [描画フレーム(間欠カウンタ==0)のとき]
  //          | ダーティフラグがセットされているとき
  //          |   スクリーンを更新する
  //          |   間欠カウンタを間欠間隔に戻す
  //          [省略フレーム(間欠カウンタ!=0)のとき]
  //            間欠カウンタを1減らす
  //          [インターレースのとき]
  //            フレームパリティを反転
  //          フレームタスク(コントラスト調整など)
  //          書き込むビットマップを切り替える
  //          [描画フレーム(間欠カウンタ==0)のとき]
  //          | →描画フレームの垂直空白期間の水平同期パルス
  //          [省略フレーム(間欠カウンタ!=0)のとき]
  //            →省略フレームの垂直空白期間の水平同期パルス
  //
  //    水平同期パルス
  //      ＋水平同期パルスの長さ
  //      水平同期パルス開始
  //      [垂直空白期間のとき]
  //      | [描画フレーム(間欠カウンタ==0)のとき]
  //      | | →描画フレームの垂直空白期間の水平バックポーチと水平映像期間
  //      | [省略フレーム(間欠カウンタ!=0)のとき]
  //      |   →省略フレームの垂直空白期間の水平バックポーチと水平映像期間
  //      [垂直映像期間のとき]
  //        [ラスタ2度読みではないとき]
  //        | 高速クリアカウンタが0ではないとき
  //        |   データY座標を高速クリア
  //        [ラスタ2度読みのとき]
  //          [偶数ラスタのとき]
  //            高速クリアカウンタが0ではないとき
  //              データY座標を高速クリア
  //        [描画フレーム(間欠カウンタ==0)のとき]
  //        | →描画フレームの垂直映像期間の水平バックポーチ
  //        [省略フレーム(間欠カウンタ!=0)のとき]
  //          →省略フレームの垂直映像期間の水平バックポーチと水平映像期間
  //
  //    水平バックポーチ
  //      水平同期パルス終了
  //      [垂直空白期間のとき]
  //      | [描画フレーム(間欠カウンタ==0)のとき]
  //      | | ＋水平バックポーチと水平映像期間の長さ
  //      | | →描画フレームの垂直空白期間の水平フロントポーチ
  //      | [省略フレーム(間欠カウンタ!=0)のとき]
  //      |   ＋水平バックポーチと水平映像期間の長さ
  //      |   →省略フレームの垂直空白期間の水平フロントポーチ
  //      [垂直映像期間のとき]
  //        [描画フレーム(間欠カウンタ==0)のとき]
  //        | ＋水平バックポーチの長さ
  //        | →描画フレームの垂直映像期間の水平映像期間
  //        [省略フレーム(間欠カウンタ!=0)のとき]
  //          ＋水平バックポーチと水平映像期間の長さ
  //          →省略フレームの垂直映像期間の水平フロントポーチ
  //
  //    描画フレームの垂直映像期間の水平映像期間
  //      ＋水平水平映像期間の長さ
  //      ラスタの更新フラグがセットされているとき
  //        [ノーマルのとき]
  //        | ラスタの更新フラグをクリア
  //        [ラスタ2度読みのとき]
  //        | [奇数ラスタのとき]
  //        |   ラスタの更新フラグをクリア
  //        [インターレースのとき]
  //        | ラスタの更新フラグをクリア
  //        [スリットのとき]
  //          ラスタの更新フラグをクリア
  //        全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
  //        データY座標からスクリーンY座標へ描画
  //        [スリットのとき]
  //          輝度を半分にしてコピーする
  //        ダーティフラグをセット
  //      →描画フレームの垂直映像期間の水平フロントポーチ
  //
  //
  //    描画フレーム(間欠カウンタ==0)
  //      描画フレームの垂直空白期間(垂直フロントポーチと垂直同期パルスと垂直バックポーチ)
  //        (0)開始(描画フレームの垂直空白期間開始ラスタの水平フロントポーチ)            Start
  //        (1)描画フレームの垂直空白期間の水平フロントポーチ                              DrawIdleFront
  //        (2)描画フレームの垂直空白期間の水平同期パルス                                  DrawIdleSync
  //        (3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間                  DrawIdleBackDisp
  //      描画フレームの垂直映像期間
  //        奇偶共通
  //          (4)描画フレームの垂直映像期間の水平フロントポーチ                            DrawDispFront
  //          (5)描画フレームの垂直映像期間の水平同期パルス                                DrawDispSync
  //          (6)描画フレームの垂直映像期間の水平バックポーチ                              DrawDispBack
  //          (7)描画フレームの垂直映像期間の水平映像期間                                  DrawDispDisp
  //        偶数ラスタ
  //          (4e)描画フレームの垂直映像期間の偶数ラスタの水平フロントポーチ               DrawDispEvenFront
  //          (5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス                   DrawDispEvenSync
  //          (6e)描画フレームの垂直映像期間の偶数ラスタの水平バックポーチ                 DrawDispEvenBack
  //          (7e)描画フレームの垂直映像期間の偶数ラスタの水平映像期間                     DrawDispEvenDisp
  //        奇数ラスタ
  //          (4o)描画フレームの垂直映像期間の奇数ラスタの水平フロントポーチ               DrawDispOddFront
  //          (5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス                   DrawDispOddSync
  //          (6o)描画フレームの垂直映像期間の奇数ラスタの水平バックポーチ                 DrawDispOddBack
  //          (7o)描画フレームの垂直映像期間の奇数ラスタの水平映像期間                     DrawDispOddDisp
  //    省略フレーム(間欠カウンタ!=0)
  //      省略フレームの垂直空白期間(垂直フロントポーチと垂直同期パルスと垂直バックポーチ)
  //        (8)省略フレームの垂直空白期間の水平フロントポーチ                              OmitIdleFront
  //        (9)省略フレームの垂直空白期間の水平同期パルス                                  OmitIdleSync
  //        (10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間                 OmitIdleBackDisp
  //      省略フレームの垂直映像期間
  //        奇偶共通
  //          (11)省略フレームの垂直映像期間の水平フロントポーチ                           OmitDispFront
  //          (12)省略フレームの垂直映像期間の水平同期パルス                               OmitDispSync
  //          (13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間               OmitDispBackDisp
  //        偶数ラスタ
  //          (11e)省略フレームの垂直映像期間の偶数ラスタの水平フロントポーチ              OmitDispEvenFront
  //          (12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス                  OmitDispEvenSync
  //          (13e)省略フレームの垂直映像期間の偶数ラスタの水平バックポーチと水平映像期間  OmitDispEvenBackDisp
  //        奇数ラスタ
  //          (11o)省略フレームの垂直映像期間の奇数ラスタの水平フロントポーチ              OmitDispOddFront
  //          (12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス                  OmitDispOddSync
  //          (13o)省略フレームの垂直映像期間の奇数ラスタの水平バックポーチと水平映像期間  OmitDispOddBackDisp
  //

  public static void crtUpdateLength () {
    int k = crtHRLCurr << 3 | crtHighResoCurr << 2 | crtHResoCurr;
    double osc = (double) crtFreqs[CRT_OSCS[k]];  //オシレータ周波数
    int div = CRT_DIVS[k];  //分周比分母
    int vTotal = crtR04VFrontEndCurr + 1;
    double multiplier = 1.0;  //垂直同期周波数に掛ける数
    if (XEiJ.pnlAdjustVsync && XEiJ.pnlFixedRate != 0.0) {  //ホストのリフレッシュレートに合わせる、かつ、確定済み
      double hFreq = osc / (8 * div * crtHTotalColumn);  //水平同期周波数
      double vFreq = hFreq / vTotal;  //垂直同期周波数
      double rate = XEiJ.pnlFixedRate;  //ホストのリフレッシュレート
      if (vFreq * 0.75 <= rate && rate <= vFreq * 1.25) {  //ホストのリフレッシュレートが垂直同期周波数の0.75～1.25倍のとき
        multiplier = rate / vFreq;  //垂直同期周波数に掛ける数
      }
    }
    crtVsyncMultiplier = multiplier;
    crtColumnTime = (int) ((double) (XEiJ.TMR_FREQ * 8 * div) / (osc * multiplier) + 0.5);
    crtHSyncLength = crtColumnTime * crtHSyncColumn;
    crtHBackLength = crtColumnTime * crtHBackColumn;
    crtHDispLength = crtColumnTime * crtHDispColumn;
    crtHFrontLength = crtColumnTime * crtHFrontColumn;
    crtHBackDispLength = crtColumnTime * (crtHBackColumn + crtHDispColumn);
    long t = (long) crtColumnTime * (long) (crtHTotalColumn * vTotal);
    crtTotalLength = t / 1000000000L;  //ミリ
    crtTotalLengthMNP = t - 1000000000L * crtTotalLength;  //マイクロナノピコ
  }

  //----------------------------------------------------------------
  //  スクリーンの初期化
  public static final TickerQueue.Ticker InitialStage = new TickerQueue.Ticker () {
    @Override protected void tick () {
    ret:
      {
        //  水平映像期間は1カラム以上128カラム以下でなければならない
        //  垂直映像期間は1ラスタ以上1024ラスタ以下でなければならない
        //  垂直同期パルス、垂直バックポーチ、垂直フロントポーチはそれぞれ1ラスタ以上なければならない
        //  不正な値が設定されているときは停止して少し待つ
        //  同期信号が動かなくなるので不正な値を設定してから同期信号を待つようなプログラムは止まってしまう
        if (!(crtR02HBackEndCurr < crtR03HDispEndCurr && crtR03HDispEndCurr - crtR02HBackEndCurr <= 128 &&
              crtR07VDispEndCurr < crtR04VFrontEndCurr && crtR07VDispEndCurr - crtR06VBackEndCurr <= 1024 &&
              crtR05VSyncEndCurr < crtR06VBackEndCurr &&
              crtR06VBackEndCurr < crtR07VDispEndCurr)) {
          crtRestart ();
          break ret;
        }
        //
        if (CRT_RASTER_HASH_ON) {
          crtUpdateRasterHash ();
        }
        //  水平周期が水平映像期間よりも3カラム以上長くないとき
        //    水平周期を伸ばして水平同期パルスと水平バックポーチと水平フロントポーチをすべて1カラムにする
        //  水平周期が水平映像期間よりも3カラム以上長いとき
        //    必要ならば水平バックポーチと水平フロントポーチが1カラム以上なるように水平周期の中の水平映像期間の位置を調整する
        //    動作速度が変わってしまわないように、水平周期はなるべく変更しない
        //  調整するのは状態遷移の間隔だけで、CRTCの設定値は変更しない
        int hSync = crtR01HSyncEndCurr + 1;  //水平同期パルスカラム数。0以下は設定のしようがないので1以上
        int hBack = crtR02HBackEndCurr - crtR01HSyncEndCurr + 4;  //水平バックポーチカラム数。-1以上
        int hDisp = crtR03HDispEndCurr - crtR02HBackEndCurr;  //水平映像期間カラム数。0以下は除いてあるので1以上
        int hFront = crtR00HFrontEndCurr - crtR03HDispEndCurr - 4;  //水平フロントポーチカラム数。負数かも知れない
        if (hSync + hBack + hDisp + hFront < hDisp + 3) {  //水平周期が水平映像期間よりも3カラム以上長くないとき
          hSync = hBack = hFront = 1;  //水平周期を伸ばして水平同期パルスと水平バックポーチと水平フロントポーチをすべて1カラムにする
        } else {  //水平周期が水平映像期間よりも3カラム以上長いとき
          if (hBack <= 0) {  //左に寄り過ぎているとき
            hFront -= 1 - hBack;  //水平フロントポーチを削って
            hBack = 1;  //水平バックポーチを1にする
            if (hFront <= 0) {  //水平フロントポーチを削り過ぎたとき
              hSync -= 1 - hFront;  //水平同期パルスを削って
              hFront = 1;  //水平フロントポーチを1にする
            }
          } else if (hFront <= 0) {  //右に寄り過ぎているとき
            hBack -= 1 - hFront;  //水平バックポーチを削って
            hFront = 1;  //水平フロントポーチを1にする
            if (hBack <= 0) {  //水平バックポーチを削り過ぎたとき
              hSync -= 1 - hBack;  //水平同期パルスを削って
              hBack = 1;  //水平バックポーチを1にする
            }
          }
        }
        crtHSyncColumn = hSync;
        crtHBackColumn = hBack;
        crtHDispColumn = hDisp;
        crtHFrontColumn = hFront;
        crtHTotalColumn = hSync + hBack + hDisp + hFront;
        crtUpdateLength ();
        //
        crtDuplication = crtHighResoCurr == 1 && crtVResoCurr == 0 && ((SpriteScreen.sprReg8ResoCurr & 12) != 4);  //ラスタ2度読み
        crtInterlace = crtHighResoCurr + 1 <= crtVResoCurr;  //インターレース
        crtSlit = crtHighResoCurr == 0 && crtVResoCurr == 0;  //スリット
        crtDupExceptSp = crtHighResoCurr == 1 && crtVResoCurr == 0 && ((SpriteScreen.sprReg8ResoCurr & 12) == 4);  //ラスタ2度読み(スプライトを除く)
        //
        XEiJ.pnlUpdateArrangement ();
        //
        crtAllStamp += 2;
        (crtDuplication ? DuplicationStart :
         crtInterlace ? InterlaceStart :
         crtSlit ? SlitStart :
         crtDupExceptSp ? DupExceptSpStart :
         NormalStart).tick ();
      }  //ret
    }
  };

  //----------------------------------------------------------------
  //  ノーマル
  //    (0)開始(描画フレームの垂直空白期間開始ラスタの水平フロントポーチ)
  public static final TickerQueue.Ticker NormalStart = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MC68901.mfpGpipHsync != 0) {
        MC68901.mfpHsyncFall ();  //水平同期パルス終了
      }
      int n = crtRasterNumber = crtVIdleStart;  //ラスタ番号は垂直空白期間開始ラスタ
      if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
        crtDoRasterCopy ();  //ラスタコピー実行
      }
      if (RasterBreakPoint.RBP_ON) {
        if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
          RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
        }
      }
      int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
      if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
        if (irq == 0) {  //IRQ信号が0になったとき
          if (RasterBreakPoint.RBP_ON) {
            if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
              RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
            }
          }
          MC68901.mfpRintFall ();  //IRQ開始
        } else {  //IRQ信号が0でなくなったとき
          MC68901.mfpRintRise ();  //IRQ終了
        }
      }
      if (MC68901.mfpGpipVdisp != 0) {
        MC68901.mfpVdispFall ();  //垂直映像期間終了
      }
      crtClearFrames = 0;  //高速クリアカウンタを0で初期化
      if (CRT_ENABLE_INTERMITTENT) {
        crtIntermittentCounter = 0;  //間欠カウンタを0で初期化(描画フレーム)
      }
      if (!XEiJ.PNL_USE_THREAD) {
        if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
          crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
        }
      }
      TickerQueue.tkqAdd (crtTicker = NormalDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
    }
  };
  //    (1)描画フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker NormalDrawIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = NormalDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          crtScreenY = 0;  //スクリーンY座標を0で初期化
          if (!XEiJ.PNL_USE_THREAD) {
            crtDirtyY0 = -1;  //ダーティフラグをクリア
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-2,src=-2)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-1)を入れ換える
              //ラスタ(dst=-1,src=-1)
              //表(-1)を表(1)として再利用する
              SpriteScreen.sprStep1 (1);  //表(1)にスプライト(1)を並べる
              SpriteScreen.sprSwap ();  //表(1)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = NormalDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = NormalDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (2)描画フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker NormalDrawIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = NormalDrawIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker NormalDrawIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = NormalDrawIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(1)描画フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (4)描画フレームの垂直映像期間の水平フロントポーチ
  public static final TickerQueue.Ticker NormalDrawDispFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          crtScreenY++;  //スクリーンY座標を1増やす
          crtDataY++;  //データY座標を1増やす
          TickerQueue.tkqAdd (crtTicker = NormalDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = NormalDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
          }
        }
        crtScreenY++;  //スクリーンY座標を1増やす
        crtDataY++;  //データY座標を1増やす
        TickerQueue.tkqAdd (crtTicker = NormalDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
      }
    }
  };
  //    (5)描画フレームの垂直映像期間の水平同期パルス
  public static final TickerQueue.Ticker NormalDrawDispSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = NormalDrawDispBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6)描画フレームの垂直映像期間の水平バックポーチ
    }
  };
  //    (6)描画フレームの垂直映像期間の水平バックポーチ
  public static final TickerQueue.Ticker NormalDrawDispBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = NormalDrawDispDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7)描画フレームの垂直映像期間の水平映像期間
    }
  };
  //    (7)描画フレームの垂直映像期間の水平映像期間
  public static final TickerQueue.Ticker NormalDrawDispDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtRasterStamp[crtDataY] = crtAllStamp;  //ラスタの更新フラグをクリア
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット
          }
          crtDirtyY1 = crtScreenY;
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //ラスタ(dst=src)
          //表(dst)を表(dst+2)として再利用する
          SpriteScreen.sprStep1 (crtDataY + 2);  //表(dst+2)にスプライト(src+2)を並べる
          SpriteScreen.sprSwap ();  //表(dst+2)と裏(dst+1)を入れ換える
          SpriteScreen.sprStep2 (crtDataY + 1);  //表(dst+1)にバックグラウンド(src+1)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = NormalDrawDispFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4)描画フレームの垂直映像期間の水平フロントポーチ
    }
  };
  //    (8)省略フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker NormalOmitIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = NormalOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-2,src=-2)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-1)を入れ換える
              //ラスタ(dst=-1,src=-1)
              //表(-1)を表(1)として再利用する
              SpriteScreen.sprStep1 (1);  //表(1)にスプライト(1)を並べる
              SpriteScreen.sprSwap ();  //表(1)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = NormalOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = NormalOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (9)省略フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker NormalOmitIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = NormalOmitIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker NormalOmitIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = NormalOmitIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(8)省略フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (11)省略フレームの垂直映像期間の水平フロントポーチ
  public static final TickerQueue.Ticker NormalOmitDispFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = NormalOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = NormalOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = NormalOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
      }
    }
  };
  //    (12)省略フレームの垂直映像期間の水平同期パルス
  public static final TickerQueue.Ticker NormalOmitDispSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = NormalOmitDispBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間
    }
  };
  //    (13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker NormalOmitDispBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = NormalOmitDispFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11)省略フレームの垂直映像期間の水平フロントポーチ
    }
  };

  //----------------------------------------------------------------
  //  ラスタ2度読み
  //    (0)開始(描画フレームの垂直空白期間開始ラスタの水平フロントポーチ)
  public static final TickerQueue.Ticker DuplicationStart = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MC68901.mfpGpipHsync != 0) {
        MC68901.mfpHsyncFall ();  //水平同期パルス終了
      }
      int n = crtRasterNumber = crtVIdleStart;  //ラスタ番号は垂直空白期間開始ラスタ
      if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
        crtDoRasterCopy ();  //ラスタコピー実行
      }
      if (RasterBreakPoint.RBP_ON) {
        if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
          RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
        }
      }
      int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
      if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
        if (irq == 0) {  //IRQ信号が0になったとき
          if (RasterBreakPoint.RBP_ON) {
            if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
              RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
            }
          }
          MC68901.mfpRintFall ();  //IRQ開始
        } else {  //IRQ信号が0でなくなったとき
          MC68901.mfpRintRise ();  //IRQ終了
        }
      }
      if (MC68901.mfpGpipVdisp != 0) {
        MC68901.mfpVdispFall ();  //垂直映像期間終了
      }
      crtClearFrames = 0;  //高速クリアカウンタを0で初期化
      if (CRT_ENABLE_INTERMITTENT) {
        crtIntermittentCounter = 0;  //間欠カウンタを0で初期化(描画フレーム)
      }
      if (!XEiJ.PNL_USE_THREAD) {
        if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
          crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
        }
      }
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
    }
  };
  //    (1)描画フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker DuplicationDrawIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          crtScreenY = 0;  //スクリーンY座標を0で初期化
          if (!XEiJ.PNL_USE_THREAD) {
            crtDirtyY0 = -1;  //ダーティフラグをクリア
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //偶数ラスタ(dst=-2,src=-1)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-1)を入れ換える
              //奇数ラスタ(dst=-1,src=-1)
              //表(-1)を表(1)として再利用する
              SpriteScreen.sprStep1 (0);  //表(1)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(1)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (2)描画フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker DuplicationDrawIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DuplicationDrawIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(1)描画フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (4e)描画フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DuplicationDrawDispEvenFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          crtScreenY++;  //スクリーンY座標を1増やす
          crtDataY++;  //データY座標を1増やす
          TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
          }
        }
        crtScreenY++;  //スクリーンY座標を1増やす
        crtDataY++;  //データY座標を1増やす
        TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
      }
    }
  };
  //    (4o)描画フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DuplicationDrawDispOddFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          crtScreenY++;  //スクリーンY座標を1増やす
          TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DuplicationDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
          }
        }
        crtScreenY++;  //スクリーンY座標を1増やす
        TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス
      }
    }
  };
  //    (5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DuplicationDrawDispEvenSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispEvenBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6e)描画フレームの垂直映像期間の偶数ラスタの水平バックポーチ
    }
  };
  //    (5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DuplicationDrawDispOddSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispOddBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6o)描画フレームの垂直映像期間の奇数ラスタの水平バックポーチ
    }
  };
  //    (6e)描画フレームの垂直映像期間の偶数ラスタの水平バックポーチ
  public static final TickerQueue.Ticker DuplicationDrawDispEvenBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispEvenDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7e)描画フレームの垂直映像期間の偶数ラスタの水平映像期間
    }
  };
  //    (6o)描画フレームの垂直映像期間の奇数ラスタの水平バックポーチ
  public static final TickerQueue.Ticker DuplicationDrawDispOddBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispOddDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7o)描画フレームの垂直映像期間の奇数ラスタの水平映像期間
    }
  };
  //    (7e)描画フレームの垂直映像期間の偶数ラスタの水平映像期間
  public static final TickerQueue.Ticker DuplicationDrawDispEvenDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット
          }
          crtDirtyY1 = crtScreenY;  //偶数ラスタで終了する可能性があるので偶数ラスタでもcrtDirtyY1を更新する
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //偶数ラスタ(dst=src*2)
          //表(dst)を表(dst+2)として再利用する
          SpriteScreen.sprStep1 (crtDataY + 1);  //表(dst+2)にスプライト(src+1)を並べる
          SpriteScreen.sprSwap ();  //表(dst+2)と裏(dst+1)を入れ換える
          SpriteScreen.sprStep2 (crtDataY);  //表(dst+1)にバックグラウンド(src)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispOddFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4o)描画フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
    }
  };
  //    (7o)描画フレームの垂直映像期間の奇数ラスタの水平映像期間
  public static final TickerQueue.Ticker DuplicationDrawDispOddDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtRasterStamp[crtDataY] = crtAllStamp;  //ラスタの更新フラグをクリア
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット。奇数ラスタの直前でパレットやスクロール位置が変化したとき偶数ラスタでcrtDirtyY0が更新されていない可能性があるので奇数ラスタでもcrtDirtyY0を更新する
          }
          crtDirtyY1 = crtScreenY;
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //奇数ラスタ(dst=src*2+1)
          //表(dst)を表(dst+2)として再利用する
          SpriteScreen.sprStep1 (crtDataY + 1);  //表(dst+2)にスプライト(src+1)を並べる
          SpriteScreen.sprSwap ();  //表(dst+2)と裏(dst+1)を入れ換える
          SpriteScreen.sprStep2 (crtDataY + 1);  //表(dst+1)にバックグラウンド(src+1)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = DuplicationDrawDispEvenFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4e)描画フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
    }
  };
  //    (8)省略フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker DuplicationOmitIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DuplicationOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //偶数ラスタ(dst=-2,src=-1)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-1)を入れ換える
              //奇数ラスタ(dst=-1,src=-1)
              //表(-1)を表(1)として再利用する
              SpriteScreen.sprStep1 (0);  //表(1)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(1)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DuplicationOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (9)省略フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker DuplicationOmitIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DuplicationOmitIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DuplicationOmitIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DuplicationOmitIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(8)省略フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (11e)省略フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DuplicationOmitDispEvenFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DuplicationOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
      }
    }
  };
  //    (11o)省略フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DuplicationOmitDispOddFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DuplicationOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス
      }
    }
  };
  //    (12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DuplicationOmitDispEvenSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispEvenBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13e)省略フレームの垂直映像期間の偶数ラスタの水平バックポーチと水平映像期間
    }
  };
  //    (12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DuplicationOmitDispOddSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispOddBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13o)省略フレームの垂直映像期間の奇数ラスタの水平バックポーチと水平映像期間
    }
  };
  //    (13e)省略フレームの垂直映像期間の偶数ラスタの水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DuplicationOmitDispEvenBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispOddFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11o)省略フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
    }
  };
  //    (13o)省略フレームの垂直映像期間の奇数ラスタの水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DuplicationOmitDispOddBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DuplicationOmitDispEvenFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11e)省略フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
    }
  };

  //----------------------------------------------------------------
  //  インターレース
  //    (0)開始(描画フレームの垂直空白期間開始ラスタの水平フロントポーチ)
  public static final TickerQueue.Ticker InterlaceStart = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MC68901.mfpGpipHsync != 0) {
        MC68901.mfpHsyncFall ();  //水平同期パルス終了
      }
      int n = crtRasterNumber = crtVIdleStart;  //ラスタ番号は垂直空白期間開始ラスタ
      if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
        crtDoRasterCopy ();  //ラスタコピー実行
      }
      if (RasterBreakPoint.RBP_ON) {
        if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
          RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
        }
      }
      int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
      if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
        if (irq == 0) {  //IRQ信号が0になったとき
          if (RasterBreakPoint.RBP_ON) {
            if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
              RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
            }
          }
          MC68901.mfpRintFall ();  //IRQ開始
        } else {  //IRQ信号が0でなくなったとき
          MC68901.mfpRintRise ();  //IRQ終了
        }
      }
      if (MC68901.mfpGpipVdisp != 0) {
        MC68901.mfpVdispFall ();  //垂直映像期間終了
      }
      crtFrameParity = 0;  //フレームパリティを0で初期化
      crtClearFrames = 0;  //高速クリアカウンタを0で初期化
      if (CRT_ENABLE_INTERMITTENT) {
        crtIntermittentCounter = 0;  //間欠カウンタを0で初期化(描画フレーム)
      }
      if (!XEiJ.PNL_USE_THREAD) {
        if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
          crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
        }
      }
      TickerQueue.tkqAdd (crtTicker = InterlaceDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
    }
  };
  //    (1)描画フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker InterlaceDrawIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = InterlaceDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = crtFrameParity;  //データY座標をフレームパリティで初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby &&  //高速クリアの要求があり、
                     crtFrameParity == 0) {  //偶数フレームのとき
            crtClearStandby = false;
            crtClearFrames = 2;  //高速クリアカウンタを2で初期化
          }
          crtScreenY = crtFrameParity;  //スクリーンY座標をフレームパリティで初期化
          if (!XEiJ.PNL_USE_THREAD) {
            crtDirtyY0 = -1;  //ダーティフラグをクリア
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-4,src=-4)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-2)を入れ換える
              //ラスタ(dst=-2,src=-2)
              //表(-2)を表(2)として再利用する
              SpriteScreen.sprStep1 (2);  //表(2)にスプライト(2)を並べる
              SpriteScreen.sprSwap ();  //表(2)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = InterlaceDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = InterlaceDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (2)描画フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker InterlaceDrawIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = InterlaceDrawIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker InterlaceDrawIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = InterlaceDrawIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(1)描画フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (4)描画フレームの垂直映像期間の水平フロントポーチ
  public static final TickerQueue.Ticker InterlaceDrawDispFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
              if (XEiJ.PNL_USE_THREAD) {
                crtStereoscopicDrawRaster (crtScreenY ^ 1);
              }
            }
          }
          crtScreenY += 2;  //スクリーンY座標を2増やす
          crtDataY += 2;  //データY座標を2増やす
          TickerQueue.tkqAdd (crtTicker = InterlaceDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
              if (XEiJ.PNL_USE_THREAD) {
                crtStereoscopicDrawRaster (crtScreenY ^ 1);
              }
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          crtFrameParity ^= 1;  //フレームパリティを反転
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = InterlaceDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
            if (XEiJ.PNL_USE_THREAD) {
              crtStereoscopicDrawRaster (crtScreenY ^ 1);
            }
          }
        }
        crtScreenY += 2;  //スクリーンY座標を2増やす
        crtDataY += 2;  //データY座標を2増やす
        TickerQueue.tkqAdd (crtTicker = InterlaceDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
      }
    }
  };
  //    (5)描画フレームの垂直映像期間の水平同期パルス
  public static final TickerQueue.Ticker InterlaceDrawDispSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = InterlaceDrawDispBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6)描画フレームの垂直映像期間の水平バックポーチ
    }
  };
  //    (6)描画フレームの垂直映像期間の水平バックポーチ
  public static final TickerQueue.Ticker InterlaceDrawDispBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = InterlaceDrawDispDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7)描画フレームの垂直映像期間の水平映像期間
    }
  };
  //    (7)描画フレームの垂直映像期間の水平映像期間
  public static final TickerQueue.Ticker InterlaceDrawDispDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtRasterStamp[crtDataY] = crtAllStamp;  //ラスタの更新フラグをクリア
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        if (XEiJ.PNL_USE_THREAD) {
          int[] prevBM = XEiJ.pnlBMLeftArray[(XEiJ.pnlBMWrite - 1) & 3];  //直前のフレームのビットマップ
          int da = (crtScreenY ^ 1) << XEiJ.PNL_BM_OFFSET_BITS;  //コピーするラスタの先頭
          System.arraycopy (prevBM, da,  //from
                            XEiJ.pnlBM, da,  //to
                            XEiJ.pnlScreenWidth);  //length
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
          if (XEiJ.PNL_USE_THREAD) {
            crtStereoscopicDrawRaster (crtScreenY ^ 1);
          }
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット
          }
          crtDirtyY1 = crtScreenY;
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //ラスタ(dst=src)
          //表(dst)を表(dst+4)として再利用する
          SpriteScreen.sprStep1 (crtDataY + 4);  //表(dst+4)にスプライト(src+4)を並べる
          SpriteScreen.sprSwap ();  //表(dst+4)と裏(dst+2)を入れ換える
          SpriteScreen.sprStep2 (crtDataY + 2);  //表(dst+2)にバックグラウンド(src+2)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = InterlaceDrawDispFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4)描画フレームの垂直映像期間の水平フロントポーチ
    }
  };
  //    (8)省略フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker InterlaceOmitIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = InterlaceOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = crtFrameParity;  //データY座標をフレームパリティで初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby &&  //高速クリアの要求があり、
                     crtFrameParity == 0) {  //偶数フレームのとき
            crtClearStandby = false;
            crtClearFrames = 2;  //高速クリアカウンタを2で初期化
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-4,src=-4)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-2)を入れ換える
              //ラスタ(dst=-2,src=-2)
              //表(-2)を表(2)として再利用する
              SpriteScreen.sprStep1 (2);  //表(2)にスプライト(2)を並べる
              SpriteScreen.sprSwap ();  //表(2)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = InterlaceOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = InterlaceOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (9)省略フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker InterlaceOmitIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = InterlaceOmitIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker InterlaceOmitIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = InterlaceOmitIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(8)省略フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (11)省略フレームの垂直映像期間の水平フロントポーチ
  public static final TickerQueue.Ticker InterlaceOmitDispFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = InterlaceOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          crtFrameParity ^= 1;  //フレームパリティを反転
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = InterlaceOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = InterlaceOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
      }
    }
  };
  //    (12)省略フレームの垂直映像期間の水平同期パルス
  public static final TickerQueue.Ticker InterlaceOmitDispSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = InterlaceOmitDispBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間
    }
  };
  //    (13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker InterlaceOmitDispBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = InterlaceOmitDispFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11)省略フレームの垂直映像期間の水平フロントポーチ
    }
  };

  //----------------------------------------------------------------
  //  スリット
  //    (0)開始(描画フレームの垂直空白期間開始ラスタの水平フロントポーチ)
  public static final TickerQueue.Ticker SlitStart = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MC68901.mfpGpipHsync != 0) {
        MC68901.mfpHsyncFall ();  //水平同期パルス終了
      }
      int n = crtRasterNumber = crtVIdleStart;  //ラスタ番号は垂直空白期間開始ラスタ
      if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
        crtDoRasterCopy ();  //ラスタコピー実行
      }
      if (RasterBreakPoint.RBP_ON) {
        if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
          RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
        }
      }
      int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
      if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
        if (irq == 0) {  //IRQ信号が0になったとき
          if (RasterBreakPoint.RBP_ON) {
            if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
              RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
            }
          }
          MC68901.mfpRintFall ();  //IRQ開始
        } else {  //IRQ信号が0でなくなったとき
          MC68901.mfpRintRise ();  //IRQ終了
        }
      }
      if (MC68901.mfpGpipVdisp != 0) {
        MC68901.mfpVdispFall ();  //垂直映像期間終了
      }
      crtClearFrames = 0;  //高速クリアカウンタを0で初期化
      if (CRT_ENABLE_INTERMITTENT) {
        crtIntermittentCounter = 0;  //間欠カウンタを0で初期化(描画フレーム)
      }
      if (!XEiJ.PNL_USE_THREAD) {
        if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
          crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
        }
      }
      TickerQueue.tkqAdd (crtTicker = SlitDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
    }
  };
  //    (1)描画フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker SlitDrawIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = SlitDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          crtScreenY = 0;  //スクリーンY座標を0で初期化
          if (!XEiJ.PNL_USE_THREAD) {
            crtDirtyY0 = -1;  //ダーティフラグをクリア
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-4,src=-2)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-2)を入れ換える
              //ラスタ(dst=-2,src=-1)
              //表(-2)を表(2)として再利用する
              SpriteScreen.sprStep1 (1);  //表(2)にスプライト(1)を並べる
              SpriteScreen.sprSwap ();  //表(2)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = SlitDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = SlitDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (2)描画フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker SlitDrawIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = SlitDrawIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker SlitDrawIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = SlitDrawIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(1)描画フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (4)描画フレームの垂直映像期間の水平フロントポーチ
  public static final TickerQueue.Ticker SlitDrawDispFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            crtScanlineEffect.drawRaster (crtScreenY + 1);  //走査線エフェクト
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
              crtStereoscopicDrawRaster (crtScreenY + 1);
            }
          }
          crtScreenY += 2;  //スクリーンY座標を2増やす
          crtDataY++;  //データY座標を1増やす
          TickerQueue.tkqAdd (crtTicker = SlitDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            crtScanlineEffect.drawRaster (crtScreenY + 1);  //走査線エフェクト
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
              crtStereoscopicDrawRaster (crtScreenY + 1);
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = SlitDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          crtScanlineEffect.drawRaster (crtScreenY + 1);  //走査線エフェクト
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
            crtStereoscopicDrawRaster (crtScreenY + 1);
          }
        }
        crtScreenY += 2;  //スクリーンY座標を2増やす
        crtDataY++;  //データY座標を1増やす
        TickerQueue.tkqAdd (crtTicker = SlitDrawDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5)描画フレームの垂直映像期間の水平同期パルス
      }
    }
  };
  //    (5)描画フレームの垂直映像期間の水平同期パルス
  public static final TickerQueue.Ticker SlitDrawDispSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = SlitDrawDispBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6)描画フレームの垂直映像期間の水平バックポーチ
    }
  };
  //    (6)描画フレームの垂直映像期間の水平バックポーチ
  public static final TickerQueue.Ticker SlitDrawDispBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = SlitDrawDispDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7)描画フレームの垂直映像期間の水平映像期間
    }
  };
  //    (7)描画フレームの垂直映像期間の水平映像期間
  public static final TickerQueue.Ticker SlitDrawDispDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtRasterStamp[crtDataY] = crtAllStamp;  //ラスタの更新フラグをクリア
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        crtScanlineEffect.drawRaster (crtScreenY + 1);  //走査線エフェクト
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
          crtStereoscopicDrawRaster (crtScreenY + 1);
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット
          }
          crtDirtyY1 = crtScreenY + 1;
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //ラスタ(dst=src*2)
          //表(dst)を表(dst+4)として再利用する
          SpriteScreen.sprStep1 (crtDataY + 2);  //表(dst+4)にスプライト(src+2)を並べる
          SpriteScreen.sprSwap ();  //表(dst+4)と裏(dst+2)を入れ換える
          SpriteScreen.sprStep2 (crtDataY + 1);  //表(dst+2)にバックグラウンド(src+1)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = SlitDrawDispFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4)描画フレームの垂直映像期間の水平フロントポーチ
    }
  };
  //    (8)省略フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker SlitOmitIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = SlitOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-4,src=-2)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-2)を入れ換える
              //ラスタ(dst=-2,src=-1)
              //表(-2)を表(2)として再利用する
              SpriteScreen.sprStep1 (1);  //表(2)にスプライト(1)を並べる
              SpriteScreen.sprSwap ();  //表(2)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = SlitOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = SlitOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (9)省略フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker SlitOmitIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = SlitOmitIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker SlitOmitIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = SlitOmitIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(8)省略フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (11)省略フレームの垂直映像期間の水平フロントポーチ
  public static final TickerQueue.Ticker SlitOmitDispFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = SlitOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = SlitOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = SlitOmitDispSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12)省略フレームの垂直映像期間の水平同期パルス
      }
    }
  };
  //    (12)省略フレームの垂直映像期間の水平同期パルス
  public static final TickerQueue.Ticker SlitOmitDispSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = SlitOmitDispBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間
    }
  };
  //    (13)省略フレームの垂直映像期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker SlitOmitDispBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = SlitOmitDispFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11)省略フレームの垂直映像期間の水平フロントポーチ
    }
  };

  //----------------------------------------------------------------
  //  ラスタ2度読み(スプライトを除く)
  //    (0)開始(描画フレームの垂直空白期間開始ラスタの水平フロントポーチ)
  public static final TickerQueue.Ticker DupExceptSpStart = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MC68901.mfpGpipHsync != 0) {
        MC68901.mfpHsyncFall ();  //水平同期パルス終了
      }
      int n = crtRasterNumber = crtVIdleStart;  //ラスタ番号は垂直空白期間開始ラスタ
      if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
        crtDoRasterCopy ();  //ラスタコピー実行
      }
      if (RasterBreakPoint.RBP_ON) {
        if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
          RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
        }
      }
      int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
      if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
        if (irq == 0) {  //IRQ信号が0になったとき
          if (RasterBreakPoint.RBP_ON) {
            if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
              RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
            }
          }
          MC68901.mfpRintFall ();  //IRQ開始
        } else {  //IRQ信号が0でなくなったとき
          MC68901.mfpRintRise ();  //IRQ終了
        }
      }
      if (MC68901.mfpGpipVdisp != 0) {
        MC68901.mfpVdispFall ();  //垂直映像期間終了
      }
      crtClearFrames = 0;  //高速クリアカウンタを0で初期化
      if (CRT_ENABLE_INTERMITTENT) {
        crtIntermittentCounter = 0;  //間欠カウンタを0で初期化(描画フレーム)
      }
      if (!XEiJ.PNL_USE_THREAD) {
        if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
          crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
        }
      }
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
    }
  };
  //    (1)描画フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker DupExceptSpDrawIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          crtScreenY = 0;  //スクリーンY座標を0で初期化
          if (!XEiJ.PNL_USE_THREAD) {
            crtDirtyY0 = -1;  //ダーティフラグをクリア
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-2,src=-2)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-1)を入れ換える
              //ラスタ(dst=-1,src=-1)
              //表(-1)を表(1)として再利用する
              SpriteScreen.sprStep1 (1);  //表(1)にスプライト(1)を並べる
              SpriteScreen.sprSwap ();  //表(1)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (2)描画フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker DupExceptSpDrawIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (3)描画フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DupExceptSpDrawIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(1)描画フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (4e)描画フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DupExceptSpDrawDispEvenFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          crtScreenY++;  //スクリーンY座標を1増やす
          crtDataY++;  //データY座標を1増やす
          TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
          }
        }
        crtScreenY++;  //スクリーンY座標を1増やす
        crtDataY++;  //データY座標を1増やす
        TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
      }
    }
  };
  //    (4o)描画フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DupExceptSpDrawDispOddFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          crtScreenY++;  //スクリーンY座標を1増やす
          TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
            VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
            if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
              crtStereoscopicDrawRaster (crtScreenY);
            }
          }
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (!XEiJ.PNL_USE_THREAD) {
            if (crtDirtyY0 >= 0) {  //ダーティフラグがセットされているとき
              crtUpdateScreen ();  //スクリーンを更新する
              if (CRT_ENABLE_INTERMITTENT) {
                crtIntermittentCounter = crtIntermittentInterval;  //間欠カウンタを間欠間隔に戻す
              }
            }
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(2)描画フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        if (crtBeginningAllStamp != crtAllStamp) {  //全再描画スタンプが水平映像期間開始時の全再描画スタンプと異なるとき
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, true);  //データY座標からスクリーンY座標へ描画
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicDrawRaster (crtScreenY);
          }
        }
        crtScreenY++;  //スクリーンY座標を1増やす
        TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス
      }
    }
  };
  //    (5e)描画フレームの垂直映像期間の偶数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DupExceptSpDrawDispEvenSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispEvenBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6e)描画フレームの垂直映像期間の偶数ラスタの水平バックポーチ
    }
  };
  //    (5o)描画フレームの垂直映像期間の奇数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DupExceptSpDrawDispOddSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispOddBack, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(6o)描画フレームの垂直映像期間の奇数ラスタの水平バックポーチ
    }
  };
  //    (6e)描画フレームの垂直映像期間の偶数ラスタの水平バックポーチ
  public static final TickerQueue.Ticker DupExceptSpDrawDispEvenBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispEvenDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7e)描画フレームの垂直映像期間の偶数ラスタの水平映像期間
    }
  };
  //    (6o)描画フレームの垂直映像期間の奇数ラスタの水平バックポーチ
  public static final TickerQueue.Ticker DupExceptSpDrawDispOddBack = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispOddDisp, crtClock += crtHBackLength);  //＋水平バックポーチの長さ→(7o)描画フレームの垂直映像期間の奇数ラスタの水平映像期間
    }
  };
  //    (7e)描画フレームの垂直映像期間の偶数ラスタの水平映像期間
  public static final TickerQueue.Ticker DupExceptSpDrawDispEvenDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット
          }
          crtDirtyY1 = crtScreenY;  //偶数ラスタで終了する可能性があるので偶数ラスタでもcrtDirtyY1を更新する
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //ラスタ(dst=src)
          //表(dst)を表(dst+2)として再利用する
          SpriteScreen.sprStep1 (crtScreenY + 2);  //表(dst+2)にスプライト(src+2)を並べる
          SpriteScreen.sprSwap ();  //表(dst+2)と裏(dst+1)を入れ換える
          SpriteScreen.sprStep2 (crtScreenY + 1);  //表(dst+1)にバックグラウンド(src+1)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispOddFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4o)描画フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
    }
  };
  //    (7o)描画フレームの垂直映像期間の奇数ラスタの水平映像期間
  public static final TickerQueue.Ticker DupExceptSpDrawDispOddDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (crtRasterStamp[crtDataY] != crtAllStamp) {  //ラスタの更新フラグがセットされているとき
        crtRasterStamp[crtDataY] = crtAllStamp;  //ラスタの更新フラグをクリア
        crtBeginningAllStamp = crtAllStamp;  //全再描画スタンプを水平映像期間開始時の全再描画スタンプにコピー
        if (Malfunction.MLF_ON &&
            Malfunction.mlfChipMask != 0) {
          Malfunction.mlfRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        } else {
          VideoController.vcnMode.drawRaster (crtDataY, crtScreenY, false);  //データY座標からスクリーンY座標へ描画
        }
        if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
          crtStereoscopicDrawRaster (crtScreenY);
        }
        if (!XEiJ.PNL_USE_THREAD) {
          if (crtDirtyY0 < 0) {
            crtDirtyY0 = crtScreenY;  //ダーティフラグをセット。奇数ラスタの直前でパレットやスクロール位置が変化したとき偶数ラスタでcrtDirtyY0が更新されていない可能性があるので奇数ラスタでもcrtDirtyY0を更新する
          }
          crtDirtyY1 = crtScreenY;
        }
      }
      if (SpriteScreen.SPR_THREE_STEPS) {
        if (SpriteScreen.sprActive) {
          //ラスタ(dst=src)
          //表(dst)を表(dst+2)として再利用する
          SpriteScreen.sprStep1 (crtScreenY + 2);  //表(dst+2)にスプライト(src+2)を並べる
          SpriteScreen.sprSwap ();  //表(dst+2)と裏(dst+1)を入れ換える
          SpriteScreen.sprStep2 (crtScreenY + 1);  //表(dst+1)にバックグラウンド(src+1)を並べる
        }
      }
      TickerQueue.tkqAdd (crtTicker = DupExceptSpDrawDispEvenFront, crtClock += crtHDispLength);  //＋水平水平映像期間の長さ→(4e)描画フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
    }
  };
  //    (8)省略フレームの垂直空白期間の水平フロントポーチ
  public static final TickerQueue.Ticker DupExceptSpOmitIdleFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashIdle << n < CRT_RASTER_HASH_ZERO) {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtR04VFrontEndCurr < n) {  //ラスタ番号が垂直フロントポーチ終了ラスタを超えたとき
          n = crtRasterNumber = 0;  //ラスタ番号を0に戻す(垂直空白期間は0を跨ぐ)
        }
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVDispStart) {  //垂直映像期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        } else {  //垂直映像期間開始ラスタのとき
          MC68901.mfpVdispRise ();  //垂直映像期間開始
          crtR11TxYZero = crtR11TxYCurr;  //テキストY方向スクロールを保存
          crtR13GrYZero[0] = crtR13GrYCurr[0];  //グラフィックY方向スクロールを保存
          crtR13GrYZero[1] = crtR13GrYCurr[1];
          crtR13GrYZero[2] = crtR13GrYCurr[2];
          crtR13GrYZero[3] = crtR13GrYCurr[3];
          if (crtR11TxYZeroLast != crtR11TxYZero ||
              crtR13GrYZeroLast[0] != crtR13GrYZero[0] ||
              crtR13GrYZeroLast[1] != crtR13GrYZero[1] ||
              crtR13GrYZeroLast[2] != crtR13GrYZero[2] ||
              crtR13GrYZeroLast[3] != crtR13GrYZero[3]) {
            crtR11TxYZeroLast = crtR11TxYZero;
            crtR13GrYZeroLast[0] = crtR13GrYZero[0];
            crtR13GrYZeroLast[1] = crtR13GrYZero[1];
            crtR13GrYZeroLast[2] = crtR13GrYZero[2];
            crtR13GrYZeroLast[3] = crtR13GrYZero[3];
            crtAllStamp += 2;
          }
          crtDataY = 0;  //データY座標を0で初期化
          if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
            crtClearFrames--;  //高速クリアカウンタを1減らす
          } else if (crtClearStandby) {  //高速クリアの要求があるとき
            crtClearStandby = false;
            crtClearFrames = 1;  //高速クリアカウンタを1で初期化
          }
          if (XEiJ.PNL_USE_THREAD) {
            crtAllStamp += 2;
          }
          if (SpriteScreen.SPR_THREE_STEPS) {
            SpriteScreen.sprActive = SpriteScreen.sprLatched && SpriteScreen.sprAccessible;
            if (SpriteScreen.sprActive) {
              if ((SpriteScreen.sprReg4BgCtrlCurr & 512) == 0) {
                SpriteScreen.sprLatched = false;
              }
              if (!XEiJ.PNL_USE_THREAD) {
                crtAllStamp += 2;
              }
              //ラスタ(dst=-2,src=-2)
              SpriteScreen.sprStep1 (0);  //表(0)にスプライト(0)を並べる
              SpriteScreen.sprSwap ();  //表(0)と裏(-1)を入れ換える
              //ラスタ(dst=-1,src=-1)
              //表(-1)を表(1)として再利用する
              SpriteScreen.sprStep1 (1);  //表(1)にスプライト(1)を並べる
              SpriteScreen.sprSwap ();  //表(1)と裏(0)を入れ換える
              SpriteScreen.sprStep2 (0);  //表(0)にバックグラウンド(0)を並べる
            }
          }
          if (XEiJ.PNL_STEREOSCOPIC_ON && XEiJ.pnlStereoscopicOn) {
            crtStereoscopicStart ();
          }
          TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
        }
      } else {  //垂直空白期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直映像期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
      }
    }
  };
  //    (9)省略フレームの垂直空白期間の水平同期パルス
  public static final TickerQueue.Ticker DupExceptSpOmitIdleSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitIdleBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
    }
  };
  //    (10)省略フレームの垂直空白期間の水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DupExceptSpOmitIdleBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitIdleFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(8)省略フレームの垂直空白期間の水平フロントポーチ
    }
  };
  //    (11e)省略フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DupExceptSpOmitDispEvenFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispEvenSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
      }
    }
  };
  //    (11o)省略フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
  public static final TickerQueue.Ticker DupExceptSpOmitDispOddFront = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int n = ++crtRasterNumber;  //ラスタ番号を1増やす
      if (!CRT_RASTER_HASH_ON || crtRasterHashDisp << n < CRT_RASTER_HASH_ZERO) {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性があるとき
        if (crtRasterCopyOn) {  //ラスタコピースイッチがONのとき
          crtDoRasterCopy ();  //ラスタコピー実行
        }
        if (RasterBreakPoint.RBP_ON) {
          if (n == RasterBreakPoint.rbpActiveBreakRaster) {  //ブレークラスタのとき
            RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
          }
        }
        int irq = n == crtR09IRQRasterCurr ? 0 : MC68901.MFP_GPIP_RINT_MASK;  //IRQ信号を更新
        if (irq != MC68901.mfpGpipRint) {  //IRQ信号が変化したとき
          if (irq == 0) {  //IRQ信号が0になったとき
            if (RasterBreakPoint.RBP_ON) {
              if (RasterBreakPoint.rbpIRQBreakEnabled) {  //IRQラスタでラスタブレークをかけるとき
                RasterBreakPoint.rbpFire ();  //ラスタブレークをかける
              }
            }
            MC68901.mfpRintFall ();  //IRQ開始
          } else {  //IRQ信号が0でなくなったとき
            MC68901.mfpRintRise ();  //IRQ終了
          }
        }
        if (n != crtVIdleStart) {  //垂直空白期間開始ラスタではないとき
          TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス
        } else {  //垂直空白期間開始ラスタのとき
          MC68901.mfpVdispFall ();  //垂直映像期間終了
          if (CRT_ENABLE_INTERMITTENT) {
            crtIntermittentCounter--;  //間欠カウンタを1減らす
          }
          if (XEiJ.mpuClockTime >= crtFrameTaskClock) {
            crtDoFrameTask ();  //フレームタスク(コントラスト調整など)
          }
          if (XEiJ.PNL_USE_THREAD) {
            if (FlashingLights.FLR_ON &&
                FlashingLights.flrActive) {
              FlashingLights.flrVsync ();
            }
            XEiJ.pnlBM = XEiJ.pnlBMLeftArray[++XEiJ.pnlBMWrite & 3];  //書き込むビットマップを切り替える
          }
          TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitIdleSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(9)省略フレームの垂直空白期間の水平同期パルス
        }
      } else {  //垂直映像期間の水平フロントポーチでアクションを起こすべきラスタの可能性がないとき
        //垂直空白期間開始ラスタではないとき
        TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispOddSync, crtClock += crtHFrontLength);  //＋水平フロントポーチの長さ→(12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス
      }
    }
  };
  //    (12e)省略フレームの垂直映像期間の偶数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DupExceptSpOmitDispEvenSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      if (crtClearFrames != 0) {  //高速クリアカウンタが0ではないとき
        crtRapidClear (crtDataY);  //データY座標を高速クリア
        crtRasterStamp[crtDataY] = 0;
      }
      TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispEvenBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13e)省略フレームの垂直映像期間の偶数ラスタの水平バックポーチと水平映像期間
    }
  };
  //    (12o)省略フレームの垂直映像期間の奇数ラスタの水平同期パルス
  public static final TickerQueue.Ticker DupExceptSpOmitDispOddSync = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncRise ();  //水平同期パルス開始
      TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispOddBackDisp, crtClock += crtHSyncLength);  //＋水平同期パルスの長さ→(13o)省略フレームの垂直映像期間の奇数ラスタの水平バックポーチと水平映像期間
    }
  };
  //    (13e)省略フレームの垂直映像期間の偶数ラスタの水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DupExceptSpOmitDispEvenBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispOddFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11o)省略フレームの垂直映像期間の奇数ラスタの水平フロントポーチ
    }
  };
  //    (13o)省略フレームの垂直映像期間の奇数ラスタの水平バックポーチと水平映像期間
  public static final TickerQueue.Ticker DupExceptSpOmitDispOddBackDisp = new TickerQueue.Ticker () {
    @Override protected void tick () {
      MC68901.mfpHsyncFall ();  //水平同期パルス終了
      TickerQueue.tkqAdd (crtTicker = DupExceptSpOmitDispEvenFront, crtClock += crtHBackDispLength);  //＋水平バックポーチと水平映像期間の長さ→(11e)省略フレームの垂直映像期間の偶数ラスタの水平フロントポーチ
    }
  };



}  //class CRTC



