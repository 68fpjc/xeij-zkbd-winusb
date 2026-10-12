//========================================================================================
//  HDMedia.java
//    en:SASI hard disk media
//    ja:SASIハードディスクメディア
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//SASIハードディスクのメディアの種類
//
//  アドレス
//  00000000  SASIディスクIPL
//            IPLROMが$2000.wに読み込んで実行する
//            起動パーティションを自動または手動で選択し、選択されたパーティションのSASIパーティションIPLを$2400.wに読み込んで実行する
//
//  00000400  パーティションテーブル
//                 10MB        20MB        40MB
//  00000400    0x5836384b  0x5836384b  0x5836384b  X68Kマジック
//  00000404    0x00009f54  0x00013c98  0x00027930  レコード数(ディスクイメージではこのサイズで保存する)
//  00000408    0x00009f54  0x00013c98  0x00027930  代替レコード
//  0000040c    0x0000af50  0x00015660  0x0002acc0  ランプレコード
//  00000410  第1パーティションの情報
//                 10MB        20MB        40MB
//  00000410    0x48756d61  0x48756d61  0x48756d61  Humaマジック
//  00000414    0x6e36386b  0x6e36386b  0x6e36386b  n68kマジック
//  00000418    0x00000021  0x00000021  0x00000021  開始レコード
//  0000041c    0x00009f2e  0x00013c68  0x000278f8  レコード数
//  00000420  第2パーティションの情報
//      :
//  000004f0  第15パーティションの情報
//  00000500  空き
//      :
//
//  第1パーティション(10MBの装置に最大サイズのパーティションを確保した場合)
//  アドレス   セクタ
//  00002100  00000000  SASIパーティションIPL
//                      SASIディスクIPLが$2400.wに読み込んで実行する
//                      ルートディレクトリにあるHUMAN.SYSを$6800.wに読み込んで実行する
//  00002500  00000001  第1FAT
//  00007500  00000015  第2FAT
//  0000c500  00000029  ルートディレクトリ
//  00010500  00000039  データ領域
//
//  ドライブ情報(最大サイズのパーティションを確保した場合)
//     10MB   20MB   40MB
//     1024   1024   1024  バイト/セクタ
//        1      1      1  セクタ/クラスタ
//    10132  20155  40335  データ領域のクラスタ数+2
//        1      1      1  予約領域のセクタ数
//       20     40     80  1個のFAT領域に使用するセクタ数
//       41     81    161  ルートディレクトリの先頭セクタ番号
//      512    512    512  ルートディレクトリに入るエントリ数
//       57     97    177  データ領域の先頭セクタ番号
//
//  X68000のSASIハードディスクの物理的な構成に関する考察
//    FORMAT.XのSASIハードディスクの装置初期化で送られてくるFormatBlockコマンドのレコード番号は33ずつ増えている
//    フロッピーディスクと同様にトラック単位で初期化していると考えられる
//      00ff9858 hdcDoCommand() [0x06,0x00,0x00,0x00,0x01,0x00]
//      00ff9858 hdcDoCommand() [0x06,0x00,0x00,0x21,0x01,0x00]
//      00ff9858 hdcDoCommand() [0x06,0x00,0x00,0x42,0x01,0x00]
//                                 :
//      00ff9858 hdcDoCommand() [0x06,0x02,0x78,0xcd,0x01,0x00]
//      00ff9858 hdcDoCommand() [0x06,0x02,0x78,0xee,0x01,0x00]
//      00ff9858 hdcDoCommand() [0x06,0x02,0x79,0x0f,0x01,0x00]
//    実際、X68000で利用できるSASIハードディスクのレコード数はすべて33で割り切れる
//    IBM PC/AT(1984年)の20MBのハードディスクは512バイト/レコード*17レコード/トラック*4トラック/シリンダ*614シリンダという情報を参考にすると、
//    X68000 ACE-HD(1988年)の20MBのハードディスクは256バイト/レコード*33レコード/トラック*4トラック/シリンダ*614シリンダと考えると調度よい
//      https://en.wikipedia.org/wiki/Timeline_of_DOS_operating_systems
//    ドライブパラメータの詳細は不明だが4～5バイト目が10MB=[$03,$01],20MB=[$03,$02],40MB=[$07,$02]となっており、
//    10MB→20MBと20MB→40MBでは2倍になった項目が異なる可能性がある
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class HDMedia extends HumanMedia {

  public static final HDMedia[] HDM_ARRAY = {
    //                                         開始        終了        全体       ランプ
    new HDMedia (0, "SASI HDD (10MB)", 256, 0x00000021, 0x00009f4f, 0x00009f54, 0x0000af50),  //10441728=256*33*4*309
    new HDMedia (1, "SASI HDD (20MB)", 256, 0x00000021, 0x00013c89, 0x00013c98, 0x00015660),  //20748288=256*33*4*614
    new HDMedia (2, "SASI HDD (40MB)", 256, 0x00000021, 0x00027919, 0x00027930, 0x0002acc0),  //41496576=256*33*8*614
  };
  public static final HDMedia HDM_10MB = HDM_ARRAY[0];
  public static final HDMedia HDM_20MB = HDM_ARRAY[1];
  public static final HDMedia HDM_40MB = HDM_ARRAY[2];
  public static final int HDM_MAX_BYTES_PER_DISK = (int) HDM_40MB.humDiskEndByte;  //HDM_ARRAYにあるhumDiskEndByteの最大値

  //media = hdmLengthToMedia (longLength)
  //  ディスクイメージファイルのサイズに合うメディアを返す
  //  同じサイズでフォーマットの異なるメディアが複数あるときは最初に見つかったものを返す
  public static HDMedia hdmLengthToMedia (long longLength) {
    for (HDMedia media : HDM_ARRAY) {
      if (media.humDiskEndByte == longLength) {
        return media;
      }
    }
    return null;
  }  //hdmLengthToMedia(long)

  //media = hdmPathToMedia (length, bb)
  //  パスに合うメディアを返す。null=見つからない
  //  バッファを指定したときはそこにコピーする
  public static HDMedia hdmPathToMedia (String path, byte[] bb) {
    byte[] array = XEiJ.rscGetFile (path);
    if (array == null) {  //読み込めない
      return null;
    }
    HDMedia media = hdmLengthToMedia ((long) array.length);  //メディアの種類
    if (media == null) {  //不明
      System.out.println (Multilingual.mlnJapanese ?
                          path + " は SASI ハードディスクのイメージファイルではありません" :
                          path + " is not a SASI hard disk image file");
      return null;
    }
    System.out.println (Multilingual.mlnJapanese ?
                        path + " は " + media.hdmName + " です" :
                        path + " is " + media.hdmName);
/*
      if (ByteArray.byaRls (hduImage, 0x00000400) != 0x5836384b) {  //X68K
        //装置初期化されていない
        return true;
      }
      if (!(ByteArray.byaRls (hduImage, 0x00000410) == 0x48756d61 &&
            ByteArray.byaRls (hduImage, 0x00000414) == 0x6e36386b &&
            ByteArray.byaRls (hduImage, 0x00000418) == 0x00000021 &&
            ByteArray.byaRls (hduImage, 0x0000041c) == 0x000278f8)) {
        //先頭のパーティションがHuman68kの40MBパーティションでない
        return true;
      }
      if (false) {
        //パーティションテーブル
        ByteArray.byaWl (hduImage, 0x00000404, 0x00013c98);  //最大セクタ
        ByteArray.byaWl (hduImage, 0x00000408, 0x00013c98);  //代替セクタ
        ByteArray.byaWl (hduImage, 0x0000040c, 0x00015660);  //シッピングゾーン
        ByteArray.byaWl (hduImage, 0x0000041c, 0x00013c68);  //先頭のパーティションのブロック数
        //IPL内のBPBテーブル
        ByteArray.byaWl (hduImage, 0x00002112, 0x0400_01_02);  //バイト/セクタ,セクタ/クラスタ,FAT領域の個数
        ByteArray.byaWl (hduImage, 0x00002116, 0x0001_0200);  //予約領域のセクタ数,ルートディレクトリのエントリ数
        ByteArray.byaWl (hduImage, 0x0000211a, 0x4f1a_f8_28);  //総クラスタ数+2,FATID,1個のFAT領域のセクタ数
        //FATの20MBを超える部分に使用中の領域がないか確認してからルートディレクトリ以降を1024*80バイト手前にずらす
        //  後で読み込み方法を変更して使用中の領域が20MB以下であれば読み込めるようにしたい
        boolean error = false;
        for (int i = 256 * (33 + 4 * 1) + 4; i < 256 * (33 + 4 * 1) + 4 + 2 * (20155 - 2); i += 2) {
          int fat = (char) (hduImage[i] << 8 | hduImage[i + 1] & 255);
          error |= fat >= 20155 && fat < 40335;
        }
        for (int i = 256 * (33 + 4 * 1) + 4 + 2 * (20155 - 2); i < 256 * (33 + 4 * 1) + 4 + 2 * (40335 - 2); i += 2) {
          int fat = (char) (hduImage[i] << 8 | hduImage[i + 1] & 255);
          error |= fat != 0;
          hduImage[i] = 0;
          hduImage[i + 1] = 0;
        }
        for (int i = 256 * (33 + 4 * 161); i < 256 * (33 + 4 * (97 + 20155 - 2)); i++) {
          hduImage[i - 1024 * 80] = hduImage[i];
        }
        if (error) {
          System.out.println (Multilingual.mlnJapanese ?
                              "40MB から 20MB へ変換できませんでした" :
                              "Failed to convert from 40MB to 20MB");
        }
      }
*/
    if (bb != null) {
      System.arraycopy (array, 0, bb, 0, array.length);
    }
    return media;
  }  //hdmPathToMedia(String,byte[])


  public int hdmNumber;  //番号
  public String hdmName;  //名前
  public int hdmBytesShiftRecord;  //1レコードあたりのバイト数のシフトカウント
  public int hdmBytesPerRecord;  //1レコードあたりのバイト数(2の累乗)
  public int hdmPartitionStartRecord;  //ディスクの先頭からパーティションの先頭までのレコード数
  public int hdmPartitionEndRecord;  //ディスクの先頭からパーティションの末尾までのレコード数
  public int hdmDiskEndRecord;  //ディスクのレコード数
  public int hdmRampRecord;  //ディスクの先頭からランプレコードまでのレコード数

  public HDMedia (int number, String name,
                   int bytesPerRecord, int partitionStartRecord, int partitionEndRecord, int diskEndRecord, int rampRecord) {
    super (1024,  //bytesPerSector
           -1,    //sectorsPerCluster
           2,     //fatCount
           1,     //reservedSectors
           512,   //rootEntries
           0xf8,  //mediaByte
           0xf8,  //fatID
           -1,    //fatSectors
           (long) bytesPerRecord * diskEndRecord,         //diskEndByte
           (long) bytesPerRecord * partitionStartRecord,  //partitionStartByte
           (long) bytesPerRecord * partitionEndRecord);   //partitionEndByte
    //定数で与えるもの
    hdmNumber = number;  //番号
    hdmName = name;  //名前
    hdmBytesPerRecord = bytesPerRecord;  //1レコードあたりのバイト数(2の累乗)
    hdmPartitionStartRecord = partitionStartRecord;  //ディスクの先頭からパーティションの先頭までのレコード数
    hdmPartitionEndRecord = partitionEndRecord;  //ディスクの先頭からパーティションの先頭までのレコード数
    hdmDiskEndRecord = diskEndRecord;  //ディスクのレコード数
    hdmRampRecord = rampRecord;  //ディスクの先頭からランプレコードまでのレコード数
    //定数から求まるもの
    hdmBytesShiftRecord = Integer.numberOfTrailingZeros (hdmBytesPerRecord);  //1レコードあたりのバイト数のシフトカウント
  }  //HDMedia

  //success = media.hdmMakeFormatData (bb, copySystemFiles)
  //  SASIディスクのフォーマットデータを作る
  public boolean hdmMakeFormatData (byte[] bb, boolean copySystemFiles) {
    Arrays.fill (bb, (byte) 0);
    //SASIディスクIPLを書き込む
    System.arraycopy (HDC.HDC_DISK_IPL, 0, bb, 0x00000000, HDC.HDC_DISK_IPL.length);
    //パーティションテーブルを作る
    ByteArray.byaWl (bb, 0x00000400, 'X' << 24 | '6' << 16 | '8' << 8 | 'K');  //X68Kマジック
    ByteArray.byaWl (bb, 0x00000404, hdmDiskEndRecord);  //ディスクのレコード数。ディスクの先頭から確保されている領域の末尾までのレコード数ではない
    ByteArray.byaWl (bb, 0x00000408, hdmDiskEndRecord);  //ディスクのレコード数
    ByteArray.byaWl (bb, 0x0000040c, hdmRampRecord);  //ランプレコード
    ByteArray.byaWl (bb, 0x00000410, 'H' << 24 | 'u' << 16 | 'm' << 8 | 'a');  //Humaマジック
    ByteArray.byaWl (bb, 0x00000414, 'n' << 24 | '6' << 16 | '8' << 8 | 'k');  //n68kマジック。n68kの代わりにn/16などと書くと1クラスタが16セクタになったりする
    ByteArray.byaWl (bb, 0x00000418, hdmPartitionStartRecord);  //(0=自動起動,1=使用不可,2=使用可能)<<24|ディスクの先頭からパーティションの先頭までのレコード数
    ByteArray.byaWl (bb, 0x0000041c, hdmPartitionEndRecord - hdmPartitionStartRecord);  //パーティションのレコード数
    //SASIパーティションIPLを書き込む
    System.arraycopy (HDC.HDC_PARTITION_IPL, 0, bb, 0x00002100, HDC.HDC_PARTITION_IPL.length);
    //SASIパーティションIPLにBPBを埋め込む
    ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x12, humBytesPerSector);        //1138  0000.w  1セクタあたりのバイト数(2の累乗)
    ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x14, humSectorsPerCluster);     //113a  0002.b  1クラスタあたりのセクタ数(2の累乗)
    ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x15, humFatCount);              //113b  0003.b  FAT領域の個数
    ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x16, humReservedSectors);       //113c  0004.w  予約領域のセクタ数(FAT領域の先頭セクタ番号)
    ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x18, humRootEntries);           //113e  0006.w  ルートディレクトリに入るエントリ数
    ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x1a, humPartitionSectors);      //1140  0008.w  パーティションのセクタ数
    ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x1c, humFatID);                 //1142  000a.b  FATID
    ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x1d, humFatSectors);            //1143  000b.b  1個のFAT領域に使用するセクタ数
    ByteArray.byaWl (bb, (int) humPartitionStartByte + 0x1e, hdmPartitionStartRecord);  //1144  000c.l  ディスクの先頭からパーティションの先頭までのレコード数
    //FAT領域の先頭にFATマーカーを書き込む
    humWriteFatMarker (bb);
    //システムファイルを転送する
    if (copySystemFiles) {
      if (!humCopyHumanSys (bb) ||
          !humCopyCommandX (bb)) {
        return false;
      }
    }
    if (false) {
      humDumpFat (bb);
    }
    return true;
  }  //media.hdmMakeFormatData(byte[],boolean)

}  //class HDMedia



