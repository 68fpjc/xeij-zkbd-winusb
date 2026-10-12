//========================================================================================
//  HumanMedia.java
//    en:Human68k media -- It initializes a Human68k file system.
//    ja:Human68kメディア -- Human68kのファイルシステムを初期化します。
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class HumanMedia {

  //ディレクトリのエントリの属性
  protected static final int HUM_EXECUTABLE = 0b10000000;  //実行可能
  protected static final int HUM_LINK       = 0b01000000;  //シンボリックリンク
  protected static final int HUM_ARCHIVE    = 0b00100000;  //ファイル
  protected static final int HUM_DIRECTORY  = 0b00010000;  //ディレクトリ
  protected static final int HUM_VOLUME     = 0b00001000;  //ボリューム名
  protected static final int HUM_SYSTEM     = 0b00000100;  //システム
  protected static final int HUM_HIDDEN     = 0b00000010;  //不可視
  protected static final int HUM_READONLY   = 0b00000001;  //書き込み不可

  public int humBytesShiftSector;     //        1セクタあたりのバイト数のシフトカウント
  public int humBytesPerSector;       //0000.w  1セクタあたりのバイト数(2の累乗)
  public int humSectorsShiftCluster;  //        1クラスタあたりのセクタ数のシフトカウント
  public int humSectorsPerCluster;    //0002.b  1クラスタあたりのセクタ数(2の累乗)
  public int humBytesShiftCluster;    //        1クラスタあたりのバイト数のシフトカウント
  public int humBytesPerCluster;      //        1クラスタあたりのバイト数(2の累乗)
  public int humFatCount;             //0003.b  FAT領域の個数
  public int humReservedSectors;      //0004.w  予約領域のセクタ数(FAT領域の先頭セクタ番号)
  public int humRootEntries;          //0006.w  ルートディレクトリに入るエントリ数
  public int humEntriesPerSector;     //        1セクタあたりのエントリ数
  public int humRootSectors;          //        ルートディレクトリのセクタ数
  public int humMediaByte;            //        メディアバイト
  public int humFatID;                //000a.b  FATID
  public boolean humFat12;            //        FATの種類(true=FAT12,false=FAT16)
  public int humFatMarker;            //        FATマーカー(FATID<<24|(humFat12?0x00ffff00|0x00ffffff))
  public int humFatTailCode;          //        FATの終端コード(humFat12?0xfff:0xffff)
  public int humFatSectors;           //000b.b  1個のFAT領域に使用するセクタ数
  public int humPartitionSectors;     //000c.l  パーティションのセクタ数
  public int humFatAndDataSectors;    //        FAT領域とデータ領域を合わせたセクタ数。ルートディレクトリを跨いでいるので連続しているわけではない
  public int humDataClustersPlus2;    //        データ領域のクラスタ数+2
  public long humPartitionStartByte;  //        ディスクの先頭からパーティションの先頭までのバイト数
  public long humFatStartByte;        //        ディスクの先頭からFAT領域の先頭までのバイト数
  public int humRootStartSector;      //        ルートディレクトリの先頭セクタ番号
  public long humRootStartByte;       //        ディスクの先頭からルートディレクトリの先頭までのバイト数
  public int humDataStartSector;      //        データ領域の先頭セクタ番号
  public long humDataStartByte;       //        ディスクの先頭からデータ領域の先頭までのバイト数
  public long humDataEndByte;         //        ディスクの先頭からデータ領域の末尾までのバイト数
  public long humPartitionEndByte;    //        ディスクの先頭からパーティションの末尾までのバイト数
  public long humDiskEndByte;         //        ディスクのバイト数

  public HumanMedia (int bytesPerSector, int sectorsPerCluster, int fatCount,
                     int reservedSectors, int rootEntries, int mediaByte, int fatID, int fatSectors,
                     long diskEndByte) {
    this (bytesPerSector, sectorsPerCluster, fatCount,
          reservedSectors, rootEntries, mediaByte, fatID, fatSectors,
          diskEndByte, 0L, diskEndByte);
  }  //new HumanMedia(int,int,int,int,int,int,int,long)
  public HumanMedia (int bytesPerSector, int sectorsPerCluster, int fatCount,
                     int reservedSectors, int rootEntries, int mediaByte, int fatID, int fatSectors,
                     long diskEndByte, long partitionStartByte) {
    this (bytesPerSector, sectorsPerCluster, fatCount,
          reservedSectors, rootEntries, mediaByte, fatID, fatSectors,
          diskEndByte, partitionStartByte, diskEndByte);
  }  //new HumanMedia(int,int,int,int,int,int,int,long,long)
  public HumanMedia (int bytesPerSector, int sectorsPerCluster, int fatCount,
                     int reservedSectors, int rootEntries, int mediaByte, int fatID, int fatSectors,
                     long diskEndByte, long partitionStartByte, long partitionEndByte) {
    //定数で与えるもの
    humBytesPerSector = bytesPerSector;  //1セクタあたりのバイト数(2の累乗)
    humFatCount = fatCount;  //FAT領域の個数
    humReservedSectors = reservedSectors;  //予約領域のセクタ数(FAT領域の先頭セクタ番号)
    humRootEntries = rootEntries;  //ルートディレクトリに入るエントリ数
    humMediaByte = mediaByte;  //メディアバイト
    humFatID = fatID;  //FATID
    humPartitionStartByte = partitionStartByte;  //ディスクの先頭からパーティションの先頭までのバイト数
    humPartitionEndByte = partitionEndByte;  //ディスクの先頭からパーティションの末尾までのバイト数
    humDiskEndByte = diskEndByte;  //ディスクのバイト数
    //定数から求まるもの
    humBytesShiftSector = Integer.numberOfTrailingZeros (humBytesPerSector);  //1セクタあたりのバイト数のシフトカウント
    humPartitionSectors = (int) (humPartitionEndByte - humPartitionStartByte >> humBytesShiftSector);  //パーティションのセクタ数
    humFatStartByte = humPartitionStartByte + (long) (humReservedSectors << humBytesShiftSector);  //ディスクの先頭からFAT領域の先頭までのバイト数
    humEntriesPerSector = humBytesPerSector >> 5;  //1セクタあたりのエントリ数
    humRootSectors = humBytesToSectors (humRootEntries << 5);  //ルートディレクトリのセクタ数。端数を切り上げる
    //1クラスタあたりのセクタ数を決める
    //  データ領域のクラスタ数の上限を65520(0xfff0)として1クラスタあたりのセクタ数がなるべく小さくなるようにする
    //    1クラスタあたりのセクタ数は必ずしも小さければよいとは限らないが、ここではなるべく小さくなるようにする
    //  FAT16で65520クラスタでFAT領域の末尾に隙間を空けなかった場合のセクタ数がFAT領域とデータ領域を合わせたセクタ数に満たないとき1クラスタあたりのセクタ数を大きくする
    //    65520クラスタのときFAT領域は128KBなのでFAT領域とデータ領域を合わせて128+65520=65648KBまでは1クラスタ=1セクタでよい
    //    65649KB以上のときもFAT領域の末尾に隙間を空ければ1クラスタ=1セクタにすることは可能だが、そのような割り当て方はしないことにする
    //  メモ
    //    FORMAT.Xは全領域のセクタ数を65520以下になるまで右にシフトしている
    //    FORMAT.X  00005024
    humFatAndDataSectors = humPartitionSectors - humReservedSectors - humRootSectors;  //FAT領域とデータ領域を合わせたセクタ数。ルートディレクトリを跨いでいるので連続しているわけではない
    if (sectorsPerCluster >= 0) {  //1クラスタあたりのセクタ数が指定されている
      humSectorsShiftCluster = Integer.numberOfTrailingZeros (sectorsPerCluster);  //1クラスタあたりのセクタ数のシフトカウント
      humSectorsPerCluster = sectorsPerCluster;  //1クラスタあたりのセクタ数(2の累乗)
    } else {  //1クラスタあたりのセクタ数が指定されていない
      humSectorsShiftCluster = 0;  //1クラスタあたりのセクタ数のシフトカウント
      humSectorsPerCluster = 1;  //1クラスタあたりのセクタ数(2の累乗)
      while (humBytesToSectors (2 * (65520 + 2)) * humFatCount + (65520 << humSectorsShiftCluster) < humFatAndDataSectors) {  //FAT16で65520クラスタでFAT領域の末尾に隙間を空けなかった場合のセクタ数がFAT領域とデータ領域を合わせたセクタ数に満たないとき
        humSectorsShiftCluster++;  //1クラスタあたりのセクタ数を2倍にする
      }
      humSectorsPerCluster = 1 << humSectorsShiftCluster;  //1クラスタあたりのセクタ数
    }
    humBytesShiftCluster = humBytesShiftSector + humSectorsShiftCluster;  //1クラスタあたりのバイト数のシフトカウント
    humBytesPerCluster = 1 << humBytesShiftCluster;  //1クラスタあたりのバイト数(2の累乗)
    //FATの種類を決める
    //  FAT12とFAT16を見分ける唯一の方法はデータ領域のクラスタ数が4085(0xff5)未満か以上かを確認することである
    //    human302  0000ede4  指定されたクラスタ番号のFATを得る
    //    FAT12とFAT16はFATマーカーの長さが異なるが、FAT領域の4バイト目が0xffかどうかでFAT12とFAT16を見分けることはできない
    //    FAT12でも先頭のクラスタが使用中(終端)ならばFAT領域の4バイト目は0xffである
    //  FAT領域とデータ領域を合わせたセクタ数がFAT16で4085クラスタのときのセクタ数に満たなければFAT12、それ以外はFAT16とする
    humFat12 = humFatAndDataSectors < humBytesToSectors (2 * (4085 + 2)) * humFatCount + (4085 << humSectorsShiftCluster);  //FATの種類(true=FAT12,false=FAT16)
    humFatMarker = humFatID << 24 | (humFat12 ? 0x00ffff00 : 0x00ffffff);  //FATマーカー
    humFatTailCode = humFat12 ? 0xfff : 0xffff;  //FATの終端コード
    //1個のFAT領域に使用するセクタ数とデータ領域のクラスタ数を決める
    //  FAT12のときはデータ領域のクラスタ数が4085未満になるように1個のFAT領域に使用するセクタ数を調整する
    //  このときFAT領域を必要最小限に切り詰めてはならない場合があることに注意する
    //  例:4096KBのメモリブロックに予約領域のセクタ数=0、FATの個数=1、ルートディレクトリのエントリ数=128のRAMディスクを作る場合
    //    ルートディレクトリのセクタ数は128/32=4
    //    FAT領域とデータ領域を合わせたセクタ数は4096-0-4=4092
    //    FAT領域とデータ領域を合わせたセクタ数が4085以上なのでFAT16と仮定する
    //    1個のFAT領域に使用するセクタ数はceil(4092*2/1024)=8
    //    データ領域のクラスタ数は4096-0-8*1-4=4084
    //    データ領域のクラスタ数が4085未満なので、FAT16と仮定したにも関わらず実際にはFAT12でアクセスされることになる
    //    FAT12の場合に使用されるFAT領域のセクタ数はceil(ceil(4084*1.5)/1024)=6
    //    ここでFAT領域のセクタ数を8から6に切り詰めると、データ領域のクラスタ数は4096-0-6*1-4=4086
    //    今度はデータ領域のクラスタ数が4085以上なので、FAT領域がFAT12の分しかないのにFAT16でアクセスされることになる
    //    それではFAT領域とルートディレクトリが重なって破壊されてしまう
    //    この場合はFAT12を選択してFAT領域を切り詰めずに2セクタ余らせた状態にしておかなければならない
    if (fatSectors >= 0) {  //1個のFAT領域に使用するセクタ数が指定されている
      humFatSectors = fatSectors;  //1個のFAT領域に使用するセクタ数
      humDataClustersPlus2 = humFatAndDataSectors - humFatSectors * humFatCount >> humSectorsShiftCluster;  //データ領域のクラスタ数+2
    } else {
      humDataClustersPlus2 = (humFatAndDataSectors >> humSectorsShiftCluster) + 2;  //データ領域のクラスタ数+2の最大値
      humDataClustersPlus2 = (
        humFat12 ?
        Math.min (4084,
                  (humFatAndDataSectors - humBytesToSectors (3 * humDataClustersPlus2 + 1 >>> 1) * humFatCount) >> humSectorsShiftCluster) :
        (humFatAndDataSectors - humBytesToSectors (2 * humDataClustersPlus2) * humFatCount) >> humSectorsShiftCluster
        ) + 2;  //データ領域のクラスタ数+2
      humFatSectors = (humFatAndDataSectors - (humDataClustersPlus2 - 2 << humSectorsShiftCluster)) / humFatCount;  //1個のFAT領域に使用するセクタ数
    }
    //ルートディレクトリ
    humRootStartSector = humReservedSectors + humFatSectors * humFatCount;  //ルートディレクトリの先頭セクタ番号
    //humRootStartByte = humFatStartByte + (long) (humFatSectors * humFatCount << humBytesShiftSector);  //ディスクの先頭からルートディレクトリの先頭までのバイト数
    humRootStartByte = humPartitionStartByte + (long) (humRootStartSector << humBytesShiftSector);  //ディスクの先頭からルートディレクトリの先頭までのバイト数
    //データ領域
    humDataStartSector = humRootStartSector + humRootSectors;  //データ領域の先頭セクタ番号
    //humDataStartByte = humRootStartByte + (long) (humRootSectors << humBytesShiftSector);  //ディスクの先頭からデータ領域の先頭までのバイト数
    humDataStartByte = humPartitionStartByte + (long) (humDataStartSector << humBytesShiftSector);  //ディスクの先頭からデータ領域の先頭までのバイト数
    humDataEndByte = humDataStartByte + ((long) (humDataClustersPlus2 - 2) << humBytesShiftCluster);  //ディスクの先頭からデータ領域の末尾までのバイト数
  }  //new HumanMedia(int,int,int,int,int,int,int,long,long,long)

  //humPrintInfo ()
  public final void humPrintInfo () {
    System.out.printf ("   humBytesShiftSector=  %12d\n", humBytesShiftSector);
    System.out.printf ("     humBytesPerSector=  %12d\n", humBytesPerSector);
    System.out.printf ("humSectorsShiftCluster=  %12d\n", humSectorsShiftCluster);
    System.out.printf ("  humSectorsPerCluster=  %12d\n", humSectorsPerCluster);
    System.out.printf ("  humBytesShiftCluster=  %12d\n", humBytesShiftCluster);
    System.out.printf ("    humBytesPerCluster=  %12d\n", humBytesPerCluster);
    System.out.printf ("           humFatCount=  %12d\n", humFatCount);
    System.out.printf ("    humReservedSectors=  %12d\n", humReservedSectors);
    System.out.printf ("        humRootEntries=  %12d\n", humRootEntries);
    System.out.printf ("   humEntriesPerSector=  %12d\n", humEntriesPerSector);
    System.out.printf ("        humRootSectors=  %12d\n", humRootSectors);
    System.out.printf ("          humMediaByte=    0x%08x\n", humMediaByte);
    System.out.printf ("              humFatID=    0x%08x\n", humFatID);
    System.out.printf ("              humFat12=  %12b\n", humFat12);
    System.out.printf ("          humFatMarker=    0x%08x\n", humFatMarker);
    System.out.printf ("        humFatTailCode=    0x%08x\n", humFatTailCode);
    System.out.printf ("         humFatSectors=  %12d\n", humFatSectors);
    System.out.printf ("   humPartitionSectors=  %12d\n", humPartitionSectors);
    System.out.printf ("  humFatAndDataSectors=  %12d\n", humFatAndDataSectors);
    System.out.printf ("  humDataClustersPlus2=  %12d\n", humDataClustersPlus2);
    System.out.printf (" humPartitionStartByte=0x%012x\n", humPartitionStartByte);
    System.out.printf ("       humFatStartByte=0x%012x\n", humFatStartByte);
    System.out.printf ("    humRootStartSector=  %12d\n", humRootStartSector);
    System.out.printf ("      humRootStartByte=0x%012x\n", humRootStartByte);
    System.out.printf ("    humDataStartSector=  %12d\n", humDataStartSector);
    System.out.printf ("      humDataStartByte=0x%012x\n", humDataStartByte);
    System.out.printf ("        humDataEndByte=0x%012x\n", humDataEndByte);
    System.out.printf ("   humPartitionEndByte=0x%012x\n", humPartitionEndByte);
    System.out.printf ("        humDiskEndByte=0x%012x\n", humDiskEndByte);
  }  //humPrintInfo()

  //sectors = media.humBytesToSectors (bytes)
  //  バイト数からセクタ数を求める
  public final int humBytesToSectors (int bytes) {
    return (bytes + (humBytesPerSector - 1) >> humBytesShiftSector);
  }  //media.humBytesToSectors(int)
  public final int humBytesToSectors (long bytes) {
    return (int) (bytes + (humBytesPerSector - 1) >> humBytesShiftSector);
  }  //media.humBytesToSectors(long)

  //media.humWriteFatMarker(bb)
  //  FAT領域の先頭にFATマーカーを書き込む
  //  FAT領域をゼロクリアしてから呼び出すこと
  public void humWriteFatMarker (byte[] bb) {
    //2HDEと2HSのIPLがFAT領域の未使用部分を使っている。IPLが破壊されてしまうのでここでFAT領域をゼロクリアしてはならない
    //Arrays.fill (bb, (int) humFatStartByte, (int) humRootStartByte, (byte) 0);
    for (int i = 0; i < humFatCount; i++) {
      ByteArray.byaWl (bb, (int) humFatStartByte + (humFatSectors * i << humBytesShiftSector), humFatMarker);
    }
  }  //media.humWriteFatMarker(byte[])

  //success = media.humCopyHumanSys (bb)
  //  ルートディレクトリにHUMAN.SYSをコピーする
  //  最初に見つけた空きクラスタから連続して書き込むだけなのでフォーマットの直後以外は使用できない
  public boolean humCopyHumanSys (byte[] bb) {
    //HUMAN.SYS
    //      a--s--  12:00:00  1993-09-15  58496  HUMAN.SYS
    byte[] rr = XEiJ.rscGetResource ("HUMAN.SYS");
    if (rr == null || rr.length != 58496) {
      return false;
    }
    humCopySystemFile (bb, "HUMAN   SYS", HUM_ARCHIVE | HUM_SYSTEM, 12 << 11 | 0 << 5 | 0 >> 1, 1993 - 1980 << 9 | 9 << 5 | 15, rr);
    return true;
  }  //media.humCopyHumanSys(byte[])

  //success = media.humCopyCommandX (bb)
  //  ルートディレクトリにCOMMAND.Xをコピーする
  //  最初に見つけた空きクラスタから連続して書き込むだけなのでフォーマットの直後以外は使用できない
  public boolean humCopyCommandX (byte[] bb) {
    //COMMAND.X
    //      a-----  12:00:00  1993-02-25  28382  COMMAND.X
    byte[] rr = XEiJ.rscGetResource ("COMMAND.X");
    if (rr == null || rr.length != 28382) {
      return false;
    }
    humCopySystemFile (bb, "COMMAND X  ", HUM_ARCHIVE, 12 << 11 | 0 << 5 | 0 >> 1, 1993 - 1980 << 9 | 2 << 5 | 25, rr);
    return true;
  }  //media.humCopyCommandX(byte[])

  //media.humCopySystemFile (bb, name, attr, time, date, rr)
  //  ルートディレクトリにファイルをコピーする
  //  最初に見つけた空きクラスタから連続してコピーするだけなのでフォーマットの直後以外は使用できない
  public void humCopySystemFile (byte[] bb, String name1ext, int attr, int time, int date, byte[] rr) {
    int entry;
    for (entry = (int) humRootStartByte; bb[entry] != 0x00; entry += 32) {  //空きエントリを探す
    }
    ByteArray.byaWstr (bb, entry, name1ext);  //ファイル名1と拡張子
    ByteArray.byaWb (bb, entry + 11, attr);  //属性
    ByteArray.byaWstr (bb, entry + 12, "\0\0\0\0\0\0\0\0\0\0");  //ファイル名2
    ByteArray.byaWiw (bb, entry + 22, time);  //時刻=時<<11|分<<5|秒/2
    ByteArray.byaWiw (bb, entry + 24, date);  //日付=(西暦年-1980)<<9|月<<5|月通日
    int start = 0;  //先頭クラスタ番号
    int size = rr == null ? 0 : rr.length;  //サイズ
    if (size > 0) {
      for (start = 2; humGetFat (bb, start) != 0; start++) {  //空きクラスタを探す
      }
      int end = start + (size - 1 >> humBytesShiftCluster);  //最終クラスタ番号
      for (int cluster = start; cluster < end; cluster++) {
        humSetFat (bb, cluster, cluster + 1);  //使用中
      }
      humSetFat (bb, end, humFatTailCode);  //末尾
      System.arraycopy (rr, 0, bb, (int) humDataStartByte + (start - 2 << humBytesShiftCluster), size);  //本体をコピーする
    }
    ByteArray.byaWiw (bb, entry + 26, start);  //先頭クラスタ番号
    ByteArray.byaWil (bb, entry + 28, size);  //サイズ
  }  //media.humCopySystemFile(byte[],String,int,String,int,int,byte[])

  //success = media.humUndel (bb, entry, letter)
  //  削除エントリを復元する
  public boolean humUndel (byte[] bb, int entry, int letter) {
    if ((bb[entry] & 255) != 0xe5) {  //削除エントリではない
      return false;
    }
    int size = ByteArray.byaRils (bb, entry + 28);  //ファイルサイズ
    if (size > 0) {  //ファイルサイズが0ではないので実体が存在する
      int start = ByteArray.byaRiwz (bb, entry + 26);  //先頭クラスタ番号
      int end = start + (size - 1 >> humBytesShiftCluster);  //最終クラスタ番号
      if (humDataClustersPlus2 <= end) {  //クラスタが連続していると仮定するとディスクの末尾からはみ出す
        return false;
      }
      //クラスタが未使用になっていることを確認する
      for (int cluster = start; cluster <= end; cluster++) {
        if (humGetFat (bb, cluster) != 0) {  //クラスタが未使用になっていない
          return false;
        }
      }
      //FATを再構築する
      for (int cluster = start; cluster < end; cluster++) {
        humSetFat (bb, cluster, cluster + 1);
      }
      humSetFat (bb, end, humFatTailCode);
    }
    //エントリを復活させる
    bb[entry] = (byte) letter;
    return true;
  }  //media.humUndel(byte[],int,int)

  //media.humDumpFat (bb)
  //  FAT領域をダンプする
  //            +0    +1    +2    +3    +4    +5    +6    +7    +8    +9   +10   +11   +12   +13   +14   +15   +16   +17   +18   +19
  //  #####| ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### ##### #####
  public void humDumpFat (byte[] bb) {
    final int cols = 20;
    StringBuilder sb = new StringBuilder ();
    sb.append ("       ");
    for (int i = 0; i < cols; i++) {
      sb.append (String.format (" %+5d", i));
    }
    sb.append ('\n');
    for (int cluster0 = 0, cluster1 = cols; cluster0 < humDataClustersPlus2; cluster0 = cluster1, cluster1 += cols) {
      sb.append (String.format ("%5d|", cluster0));
      for (int cluster = cluster0; cluster < cluster1; cluster++) {
        if (cluster < 2 || humDataClustersPlus2 <= cluster) {  //範囲外
          sb.append ("      ");
        } else {
          int next = humGetFat (bb, cluster);
          sb.append (next == 0 ? "   ---" :  //空き
                     next != humFatTailCode ? String.format (" %5d", next) :  //先頭または途中
                     "   END");  //終端
        }
      }
      sb.append ('\n');
    }
    System.out.println (sb.toString ());
  }  //media.humDumpFat(byte[])

  //next = media.humGetFat (bb, cluster)
  //  FATを読む
  public int humGetFat (byte[] bb, int cluster) {
    if (humFat12) {  //FAT12
      int i = (int) humFatStartByte + 3 * (cluster >> 1);
      return ((cluster & 1) == 0 ?
              bb[i + 1] << 8 & 0xf00 | bb[i    ]      & 0x0ff :  //偶数クラスタ(ML.H..→HML)
              bb[i + 2] << 4 & 0xff0 | bb[i + 1] >> 4 & 0x00f);  //奇数クラスタ(..l.hm→hml)
    } else {  //FAT16
      int i = (int) humFatStartByte + (cluster << 1);
      return (char) (bb[i    ] << 8 | bb[i + 1] & 255);
    }
  }  //media.humGetFat(byte[],int)

  //media.humSetFat (bb, cluster, next)
  //  FATに書く
  public void humSetFat (byte[] bb, int cluster, int next) {
    if (humFat12) {  //FAT12
      int i = (int) humFatStartByte + 3 * (cluster >> 1);
      if ((cluster & 1) == 0) {  //偶数クラスタ(HML→ML.H..)
        bb[i    ] = (byte) next;  //ML。next&0x0ff
        bb[i + 1] = (byte) (bb[i + 1] & 0xf0 | next >> 8 & 15);  //.H。(next&0xf00)>>8
      } else {  //奇数クラスタ(hml→..l.hm)
        bb[i + 1] = (byte) (next << 4 | bb[i + 1] & 15);  //l.。(next&0x00f)<<4
        bb[i + 2] = (byte) (next >> 4);  //hm。(next&0xff0)>>4
      }
    } else {  //FAT16
      int i = (int) humFatStartByte + (cluster << 1);
      bb[i    ] = (byte) (next >> 8);
      bb[i + 1] = (byte) next;
    }
  }  //media.humSetFat(byte[],int,int)

}  //class HumanMedia



