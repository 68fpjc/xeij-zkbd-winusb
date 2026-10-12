//========================================================================================
//  InstructionBreakPoint.java
//    en:Instruction break point -- It stops the MPU when the instruction at the specified address is executed the specified number of times.
//    ja:命令ブレークポイント -- 指定されたアドレスの命令が指定された回数実行されたときMPUを止めます。
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  機能
//    命令ブレークポイントが設定されたアドレスから始まる命令の実行回数を数えて、閾値以上のとき命令の実行前に停止する
//    設定できる命令ブレークポイントの数は無制限
//    1つのアドレスに設定できる命令ブレークポイントは1つに限る
//      1つのアドレスに複数の命令ブレークポイントがあると表示しにくい
//      1つのアドレスに複数の命令ブレークポイントがあるとすべての命令ブレークポイントの処理が終わるまで停止させられないので処理が冗長になる
//      100回目で一旦止めてから200回目でもう一度止めるような作業を繰り返したいときは連続する命令にそれぞれ命令ブレークポイントを設定する
//  命令ブレークポイントの検出
//    命令ブレークポイントをデバイスで検出する
//      多数の命令ブレークポイントが設定されていても命令ブレークポイントが存在しないページの命令のオーバーヘッドは0になる
//      命令を実行する度にそのアドレスに命令ブレークポイントがあるかどうか確認する方法は無関係の命令のオーバーヘッドが大きくなる
//      命令ブレークポイントが存在しているときだけ命令を実行する度にそのアドレスに命令ブレークポイントがあるかどうか確認する方法は、
//      メインのループを2種類用意して命令ブレークポイント以外の部分を常に一致させ続けなければならないので面倒
//      そもそもコードを分けてしまったら命令ブレークポイントがないほうのコードをデバッグしたことにならない
//    停止するときデバイスがthrowする
//      特別な例外を用意する必要がある
//      停止するときデバイスが特別な命令を返す方法も考えられるが、停止するときだけなので命令コードを消費してまで高速化する必要はない
//    第1オペコードのフェッチだけ命令ブレークポイント用のメモリマップを用いる
//      アクセスが第1オペコードのフェッチかどうかを毎回確認する必要がない
//      命令ブレークポイントが設定されているページでも第1オペコードのフェッチ以外のアクセスのオーバーヘッドは0になる
//      デバイスにはリードワードゼロ拡張の処理だけあればよい
//      ユーザモードとスーパーバイザモードを切り替えるときに命令ブレークポイント用のメモリマップを切り替える処理が加わる
//        分岐ログの負荷と比べれば遥かに小さい
//    アドレスのハッシュテーブルを用いる
//      すべての命令ブレークポイントと比較してみるよりも高速
//      多数の命令ブレークポイントが設定されていても大部分の命令のオーバーヘッドは最小になる
//      すべての命令に命令ブレークポイントの有無のマークを付けるよりもメモリを食わない
//      ハッシュコードはアドレスの下位のビット列をそのまま使う。最下位ビットは常に0なので除く
//        プログラムカウンタはインクリメントが基本なので十分に分散するはず
//  待機ポイント
//    命令を待機命令にすり替える場所
//    命令ブレークポイントの仕組みを用いて設置する
//    待機ポイントが踏まれたら本来の命令をリードせず待機命令を返す
//    待機ポイントと命令ブレークポイントは共存できる
//  命令ブレークポイントのフィールド
//    address    アドレス
//    threshold  閾値。-1=インスタント。0x7fffffff=待機ポイントのみ
//    value      現在値
//    target     目標値
//                 停止条件は現在値>=閾値だが、命令の実行前に停止するため、
//                 続行しようとしたときに同じ閾値と比較したのでは最初の命令で再び止まってしまい続行することができない
//                 そこで、停止する度に停止条件を現在値==閾値から現在値==閾値+1,現在値==閾値+2,…と増やしていくことにする。この右辺が目標値
//    next       同じハッシュコードを持つ次の命令ブレークポイント
//    waitInstruction  待機命令。nullでないとき待機ポイント
//  リセットされたとき
//    すべての命令ブレークポイントについて
//      現在値を0にする
//      インスタントのとき
//        目標値を0にする
//      インスタントでないとき
//        目標値を閾値にする
//  命令ブレークポイントを作ったとき
//    ハッシュテーブルに加える
//    ページの命令ブレークポイントを有効にする
//  命令ブレークポイントを消したとき
//    同じハッシュコードを持つ命令ブレークポイントのリストから取り除く
//    同じページに他に命令ブレークポイントがなければページの命令ブレークポイントを無効にする
//  命令ブレークポイントに遭遇したとき
//    現在値が目標値と一致しているとき
//      インスタントのとき
//        待機ポイントがないとき
//          取り除く
//        待機ポイントがあるとき
//          閾値を0x7fffffff、現在値を0、目標値を0x7fffffffにする
//      インスタント化しているとき
//        目標値を閾値に戻す
//      インスタントでなくインスタント化していないとき
//        目標値を1増やす
//      停止する
//    現在値が目標値と一致していないとき
//      現在値を1増やす
//      待機ポイントがないとき
//        本来のメモリマップから命令コードをリードして返す
//      待機ポイントがあるとき
//        待機命令を返す
//  ここまで実行
//    命令ブレークポイントがないとき
//      インスタント命令ブレークポイントを作る
//    命令ブレークポイントがあるとき
//      インスタント化する
//        pcと一致しているとき
//          目標値=現在値+1とする
//        pcと一致していないとき
//          目標値=現在値とする
//    続行する
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class InstructionBreakPoint {

  public static final boolean IBP_ON = true;  //true=命令ブレークポイントの機能を用いる

  //ハッシュテーブル
  public static final int IBP_HASH_BITS = 8;
  public static final int IBP_HASH_SIZE = 1 << IBP_HASH_BITS;
  public static final int IBP_HASH_MASK = IBP_HASH_SIZE - 1;

  //命令ブレークポイントがあるページを差し替えたメモリマップ
  public static MemoryMappedDevice[] ibpUserMap;  //ユーザモード用
  public static MemoryMappedDevice[] ibpSuperMap;  //スーパーバイザモード用
  //第1オペコードのメモリマップ
  public static MemoryMappedDevice[] ibpOp1UserMap;  //ユーザモード用。XEiJ.busUserMapまたはibpUserMap
  public static MemoryMappedDevice[] ibpOp1SuperMap;  //スーパーバイザモード用。XEiJ.busSuperMapまたはibpSuperMap
  public static MemoryMappedDevice[] ibpOp1MemoryMap;  //第1オペコードの現在のメモリマップ。XEiJ.regSRS!=0?ibpSuperMap:ibpUserMap

  //命令ブレークポイントレコード
  public static class InstructionBreakRecord {
    public int ibrLogicalAddress;  //論理アドレス
    public int ibrPhysicalAddress;  //物理アドレス
    public int ibrThreshold;  //閾値。-1=インスタント。0x7fffffff=待機ポイントのみ
    public int ibrValue;  //現在値
    public int ibrTarget;  //目標値
    public InstructionBreakRecord ibrNext;  //同じ物理ハッシュコードを持つ次の命令ブレークポイント。null=終わり
    public ExpressionEvaluator.ExpressionElement ibrScriptElement;
    public WaitInstruction ibrWaitInstruction;  //待機命令。nullでないとき待機ポイント
  }  //class InstructionBreakRecord

  //論理アドレス→命令ブレークポイント
  public static TreeMap<Integer,InstructionBreakRecord> ibpPointTable;
  //物理ハッシュコード→その物理ハッシュコードを持つ命令ブレークポイントのリストの先頭
  public static InstructionBreakRecord[] ibpHashTable;

  //ibpInit ()
  //  命令ブレークポイントを初期化する
  public static void ibpInit () {
    ibpUserMap = new MemoryMappedDevice[XEiJ.BUS_PAGE_COUNT];
    ibpSuperMap = new MemoryMappedDevice[XEiJ.BUS_PAGE_COUNT];
    Arrays.fill (ibpUserMap, MemoryMappedDevice.MMD_NUL);
    Arrays.fill (ibpSuperMap, MemoryMappedDevice.MMD_NUL);
    ibpOp1UserMap = XEiJ.busUserMap;
    ibpOp1SuperMap = XEiJ.busSuperMap;
    ibpOp1MemoryMap = ibpOp1SuperMap;
    ibpPointTable = new TreeMap<Integer,InstructionBreakRecord> ();
    ibpHashTable = new InstructionBreakRecord[IBP_HASH_SIZE];
  }  //ibpInit()

  //ibpReset ()
  //  すべての命令ブレークポイントをリセットする
  public static void ibpReset () {
    for (InstructionBreakRecord r : ibpPointTable.values ()) {
      r.ibrValue = 0;
      r.ibrTarget = r.ibrThreshold < 0 ? 0 : r.ibrThreshold;
    }
  }  //ibpReset()

  //ibpAddWaitPoint (logicalAddress, supervisor, waitInstruction)
  //  指定された論理アドレスに待機ポイントを設置する
  public static void ibpAddWaitPoint (int logicalAddress, int supervisor, WaitInstruction waitInstruction) {
    TreeMap<Integer,InstructionBreakRecord> pointTable = ibpPointTable;
    InstructionBreakRecord r = pointTable.get (logicalAddress);
    if (r == null) {
      r = ibpPut (logicalAddress, supervisor, 0, 0x7fffffff, null);  //待機ポイントのみ
    }
    r.ibrWaitInstruction = waitInstruction;  //待機命令を設定する
  }  //ibpAddWaitPoint

  //ibpRemoveWaitPoint (logicalAddress, supervisor)
  //  指定された論理アドレスの待機ポイントを撤去する
  public static void ibpRemoveWaitPoint (int logicalAddress, int supervisor) {
    TreeMap<Integer,InstructionBreakRecord> pointTable = ibpPointTable;
    InstructionBreakRecord r = pointTable.get (logicalAddress);
    if (r != null) {
      r.ibrWaitInstruction = null;  //待機ポイントを撤去する
      if (r.ibrThreshold == 0x7fffffff) {  //待機ポイントのみ
        ibpRemove (logicalAddress, supervisor);
      }
    }
  }  //ibpRemoveWaitPoint

  //ibpInstant (logicalAddress, supervisor)
  //  指定された論理アドレスにインスタント命令ブレークポイントを設定する
  //  既に同じ論理アドレスに命令ブレークポイントが設定されているときはそれをインスタント化する
  public static void ibpInstant (int logicalAddress, int supervisor) {
    TreeMap<Integer,InstructionBreakRecord> pointTable = ibpPointTable;
    InstructionBreakRecord r = pointTable.get (logicalAddress);
    if (r == null) {
      ibpPut (logicalAddress, supervisor, 0, -1, null);  //インスタント命令ブレークポイントを作る
    } else {
      r.ibrTarget = r.ibrLogicalAddress == logicalAddress ? r.ibrValue + 1 : r.ibrValue;  //既存の命令ブレークポイントをインスタント化する
    }
  }  //ibpInstant(int,int)

  //r = ibpPut (logicalAddress, supervisor, value, threshold, scriptElement)
  //  指定されたアドレスに命令ブレークポイントを設定する
  //  既に同じアドレスに命令ブレークポイントが設定されているときは閾値を変更する
  //  threshold  -1=インスタント。0x7fffffff=待機ポイントのみ
  public static InstructionBreakRecord ibpPut (int logicalAddress, int supervisor, int value, int threshold,
                                               ExpressionEvaluator.ExpressionElement scriptElement) {
    int physicalAddress = MC68060.mmuTranslatePeek (logicalAddress, supervisor, 0);  //物理アドレスに変換する
    if ((logicalAddress ^ physicalAddress) == 1) {  //変換できない
      return null;
    }
    TreeMap<Integer,InstructionBreakRecord> pointTable = ibpPointTable;
    InstructionBreakRecord r = pointTable.get (logicalAddress);  //探す
    if (r == null) {  //指定されたアドレスに命令ブレークポイントが設定されていない
      //命令ブレークポイントがなかったら命令ブレークポイントを有効にする
      if (pointTable.isEmpty ()) {
        ibpOp1SuperMap = ibpSuperMap;
        ibpOp1UserMap = ibpUserMap;
        ibpOp1MemoryMap = XEiJ.regSRS != 0 ? ibpOp1SuperMap : ibpOp1UserMap;  //ここはsupervisor!=0ではない
      }
      //新しい命令ブレークポイントを作る
      r = new InstructionBreakRecord ();
      r.ibrLogicalAddress = logicalAddress;
      r.ibrPhysicalAddress = physicalAddress;
      //命令ブレークポイントのマップに加える
      pointTable.put (logicalAddress, r);
      //同じ物理ハッシュコードを持つ命令ブレークポイントのリストに加える
      {
        InstructionBreakRecord[] hashTable = ibpHashTable;
        int h = physicalAddress >>> 1 & IBP_HASH_MASK;  //物理ハッシュコード
        r.ibrNext = hashTable[h];
        hashTable[h] = r;
      }
      //ページの命令ブレークポイントを有効にする
      {
        int p = physicalAddress >>> XEiJ.BUS_PAGE_BITS;  //ページ番号
        ibpSuperMap[p] = MemoryMappedDevice.MMD_IBP;
        ibpUserMap[p] = MemoryMappedDevice.MMD_IBP;
      }
    }
    r.ibrValue = value;  //現在値
    r.ibrTarget = threshold < 0 ? 0 : threshold;  //目標値
    r.ibrThreshold = threshold;  //閾値
    r.ibrWaitInstruction = null;  //待機ポイントではない
    r.ibrScriptElement = scriptElement;
    return r;
  }  //ibpPut(int,int,int,int,ExpressionEvaluator.ExpressionElement)

  //ibpRemove (logicalAddress, supervisor)
  //  指定された論理アドレスの命令ブレークポイントを取り除く
  public static void ibpRemove (int logicalAddress, int supervisor) {
    int physicalAddress = MC68060.mmuTranslatePeek (logicalAddress, supervisor, 0);  //物理アドレスに変換する
    if ((logicalAddress ^ physicalAddress) == 1) {  //変換できない
      return;
    }
    TreeMap<Integer,InstructionBreakRecord> pointTable = ibpPointTable;
    InstructionBreakRecord r = pointTable.get (logicalAddress);  //探す
    if (r != null) {  //指定された論理アドレスに命令ブレークポイントが設定されている
      //同じ物理ハッシュコードを持つ命令ブレークポイントのリストから取り除く
      {
        InstructionBreakRecord[] hashTable = ibpHashTable;
        int h = physicalAddress >>> 1 & IBP_HASH_MASK;  //物理ハッシュコード
        InstructionBreakRecord t = hashTable[h];
        if (t == r) {  //先頭にあった
          hashTable[h] = r.ibrNext;
        } else {
          for (; t.ibrNext != r; t = t.ibrNext) {  //必ずあるはずなのでnullのチェックを省略する
          }
          t.ibrNext = r.ibrNext;
        }
        r.ibrNext = null;
      }
      //同じ物理ページに他に命令ブレークポイントがなければ物理ページの命令ブレークポイントを無効にする
      {
        int p = physicalAddress >>> XEiJ.BUS_PAGE_BITS;  //物理ページ番号
        boolean more = false;
        for (InstructionBreakRecord t : pointTable.values ()) {
          if (t.ibrPhysicalAddress >>> XEiJ.BUS_PAGE_BITS == p) {
            more = true;
            break;
          }
        }
        ibpSuperMap[p] = more ? MemoryMappedDevice.MMD_IBP : XEiJ.busSuperMap[p];
        ibpUserMap[p] = more ? MemoryMappedDevice.MMD_IBP : XEiJ.busUserMap[p];
      }
      //命令ブレークポイントのマップから取り除く
      pointTable.remove (logicalAddress);
      //命令ブレークポイントがなくなったら命令ブレークポイントを無効にする
      if (pointTable.isEmpty ()) {
        ibpOp1SuperMap = XEiJ.busSuperMap;
        ibpOp1UserMap = XEiJ.busUserMap;
        ibpOp1MemoryMap = XEiJ.regSRS != 0 ? ibpOp1SuperMap : ibpOp1UserMap;  //ここはsupervisor!=0ではない
      }
    }
  }  //ibpRemove(int,int)

}  //class InstructionBreakPoint



