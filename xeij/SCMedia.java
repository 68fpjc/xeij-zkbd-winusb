//========================================================================================
//  SCMedia.java
//    en:SCSI hard disk media
//    ja:SCSIハードディスクメディア
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  SCSIハードディスクのメディアの種類
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class SCMedia extends HumanMedia {

  public int scmBytesShiftRecord;  //1レコードあたりのバイト数のシフトカウント
  public int scmBytesPerRecord;  //1レコードあたりのバイト数(2の累乗)
  public int scmDiskEndRecord;  //ディスクのレコード数
  public int scmPartitionStartSector;  //ディスクの先頭からパーティションの先頭までのセクタ数
  public int scmDiskEndSector;  //ディスクのセクタ数

  public SCMedia (int bytesPerRecord, int diskEndRecord) {
    super (1024,  //bytesPerSector
           -1,    //sectorsPerCluster
           2,     //fatCount
           1,     //reservedSectors
           512,   //rootEntries
           0xf7,  //mediaByte
           0xf7,  //fatID
           -1,    //fatSectors
           (long) bytesPerRecord * diskEndRecord,   //diskEndByte
           32768L,                                  //partitionStartByte
           (long) bytesPerRecord * diskEndRecord);  //partitionEndByte
    scmBytesShiftRecord = Integer.numberOfTrailingZeros (bytesPerRecord);  //1レコードあたりのバイト数のシフトカウント
    scmBytesPerRecord = 1 << scmBytesShiftRecord;  //1レコードあたりのバイト数(2の累乗)
    scmDiskEndRecord = diskEndRecord;  //ディスクのレコード数
    scmPartitionStartSector = (int) (humPartitionStartByte >> humBytesShiftSector);  //ディスクの先頭からパーティションの先頭までのセクタ数
    scmDiskEndSector = (int) (humDiskEndByte >> humBytesShiftSector);  //ディスクのセクタ数
  }  //new SCMedia()

  //success = media.scmMakeFormatData (bb, partitioningOn, copySystemFiles)
  //  SCSIディスクのフォーマットデータを作る
  public boolean scmMakeFormatData (byte[] bb, boolean partitioningOn, boolean copyHumanSysOn, boolean copyCommandXOn) {
    Arrays.fill (bb, (byte) 0);
    //SCSIディスクIDを作る
    System.arraycopy (SPC.SPC_DISK_ID_1, 0, bb, 0x00000000, SPC.SPC_DISK_ID_1.length);
    ByteArray.byaWw (bb, 0x00000008, scmBytesPerRecord);  //1レコードあたりのバイト数(2の累乗)
    ByteArray.byaWl (bb, 0x0000000a, scmDiskEndRecord);  //ディスクのレコード数
    ByteArray.byaWw (bb, 0x0000000e, 0x0100);
    System.arraycopy (SPC.SPC_DISK_ID_2, 0, bb, 0x00000010, SPC.SPC_DISK_ID_2.length);
    //SCSIディスクIPLを書き込む
    System.arraycopy (SPC.SPC_DISK_IPL, 0, bb, 0x00000400, SPC.SPC_DISK_IPL.length);
    //パーティションテーブルを作る
    ByteArray.byaWl (bb, 0x00000800, 'X' << 24 | '6' << 16 | '8' << 8 | 'K');  //X68Kマジック
    if (partitioningOn) {
      ByteArray.byaWl (bb, 0x00000804, scmDiskEndSector);  //ディスクの先頭から確保されている最後のパーティションの末尾までのセクタ数
    } else {
      ByteArray.byaWl (bb, 0x00000804, scmPartitionStartSector);  //ディスクの先頭から確保されている最後のパーティションの末尾までのセクタ数
    }
    ByteArray.byaWl (bb, 0x00000808, scmDiskEndSector);  //ディスクのセクタ数
    ByteArray.byaWl (bb, 0x0000080c, scmDiskEndSector);  //ディスクのセクタ数
    if (partitioningOn) {
      ByteArray.byaWl (bb, 0x00000810, 'H' << 24 | 'u' << 16 | 'm' << 8 | 'a');  //Humaマジック
      ByteArray.byaWl (bb, 0x00000814, 'n' << 24 | '6' << 16 | '8' << 8 | 'k');  //n68kマジック
      ByteArray.byaWl (bb, 0x00000818, scmPartitionStartSector);  //(0=自動起動,1=使用不可,2=使用可能)<<24|ディスクの先頭からパーティションの先頭までのセクタ数
      ByteArray.byaWl (bb, 0x0000081c, humPartitionSectors);  //パーティションのセクタ数
    }
    //SCSIデバイスドライバを書き込む
    System.arraycopy (SPC.SPC_DEVICE_DRIVER, 0, bb, 0x00000c00, SPC.SPC_DEVICE_DRIVER.length);
    if (partitioningOn) {
      //SCSIパーティションIPLを書き込む
      System.arraycopy (SPC.SPC_PARTITION_IPL, 0, bb, (int) humPartitionStartByte, SPC.SPC_PARTITION_IPL.length);
      //SCSIパーティションIPLにBPBを埋め込む
      ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x12, humBytesPerSector);        //7bd4  0000.w  1セクタあたりのバイト数(2の累乗)
      ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x14, humSectorsPerCluster);     //7bd6  0002.b  1クラスタあたりのセクタ数(2の累乗)
      ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x15, humFatCount);              //7bd7  0003.b  FAT領域の個数
      ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x16, humReservedSectors);       //7bd8  0004.w  予約領域のセクタ数(FAT領域の先頭セクタ番号)
      ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x18, humRootEntries);           //7bda  0006.w  ルートディレクトリに入るエントリ数
      ByteArray.byaWw (bb, (int) humPartitionStartByte + 0x1a, 0);                        //7bdc  0008.w  全領域のセクタ数(0のとき000c.lを使う)
      ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x1c, humFatID);                 //7bde  000a.b  FATID
      ByteArray.byaWb (bb, (int) humPartitionStartByte + 0x1d, humFatSectors);            //7bdf  000b.b  1個のFAT領域に使用するセクタ数
      ByteArray.byaWl (bb, (int) humPartitionStartByte + 0x1e, humPartitionSectors);      //7be0  000c.l  パーティションのセクタ数
      ByteArray.byaWl (bb, (int) humPartitionStartByte + 0x22, scmPartitionStartSector);  //7be4  0010.l  ディスクの先頭からパーティションの先頭までのセクタ数
      //FAT領域の先頭にFATマーカーを書き込む
      humWriteFatMarker (bb);
      //システムファイルを転送する
      //if (copySystemFiles) {
      //  if (!humCopyHumanSys (bb) ||
      //      !humCopyCommandX (bb)) {
      //    return false;
      //  }
      //}
      if (copyHumanSysOn) {
        if (!humCopyHumanSys (bb)) {
          return false;
        }
        if (copyCommandXOn) {
          if (!humCopyCommandX (bb)) {
            return false;
          }
        }
      }
      if (false) {
        humDumpFat (bb);
      }
    }
    return true;
  }  //media.scmMakeFormatData(byte[],boolean,boolean)

}  //class SCMedia



