//========================================================================================
//  MC68000.java
//    en:MC68010 core
//    ja:MC68010コア
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class MC68010 {

  public static void mpuCore () {

    //例外ループ
    //  別のメソッドで検出された例外を命令ループの外側でcatchすることで命令ループを高速化する
  errorLoop:
    while (XEiJ.mpuClockTime < XEiJ.mpuClockLimit) {
      try {
        //命令ループ
        while (XEiJ.mpuClockTime < XEiJ.mpuClockLimit) {
          int t;
          //命令を実行する
          XEiJ.mpuTraceFlag = XEiJ.regSRT1;  //命令実行前のsrT1
          XEiJ.mpuCycleCount = 0;  //第1オペコードからROMのアクセスウエイトを有効にする。命令のサイクル数はすべてXEiJ.mpuCycleCount+=～で加えること
          XEiJ.regPC0 = t = XEiJ.regPC;  //命令の先頭アドレス
          XEiJ.regPC = t + 2;
          XEiJ.regOC = (InstructionBreakPoint.IBP_ON ? InstructionBreakPoint.ibpOp1MemoryMap : DataBreakPoint.DBP_ON ? XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap : XEiJ.busMemoryMap)[t >>> XEiJ.BUS_PAGE_BITS].mmdRwz (t);  //第1オペコード。必ずゼロ拡張すること。pcに奇数が入っていることはないのでアドレスエラーのチェックを省略する

          //命令の処理
          //  第1オペコードの上位10bitで分岐する
          //  分岐方法
          //    XEiJ.IRP_STATIC
          //      手順
          //        命令の処理をstaticメソッドに書く
          //        switch(XEiJ.regOC>>>6)で分岐してirpXXX()で呼び出す
          //          Javaはメソッドのサイズに制限があるためswitch(XEiJ.regOC>>>6)の中にすべての命令の処理を書くことができない
          //          C言語のときはswitchの中にすべての命令の処理を書くことができる
          //      利点
          //        速い
          //    XEiJ.IRP_ENUM_DIRECT
          //      手順
          //        命令の処理をenum bodyのメソッドに書く
          //        switch(XEiJ.regOC>>>6)で分岐してIRP.XXX.exec()で呼び出す
          //      欠点
          //        XEiJ.IRP_STATICよりも遅い
          //          staticなメソッドを直接呼び出せることに変わりないはずだが、インライン展開などの最適化が弱くなるのかも知れない
          //        XEiJ.IRP_STATICと共存させようとすると命令の処理を2回書かなければならず管理が面倒になる
          //    XEiJ.IRP_ENUM_INDIRECT
          //      手順
          //        命令の処理をenum bodyのメソッドに書く
          //        XEiJ.regOCの上位10bit→enum値の配列を用意する
          //        IRPMAP.IRPMAP0[XEiJ.regOC>>>6].exec()で呼び出す
          //          C言語のときは関数を指すポインタの配列が使えるのでIRPMAP0[XEiJ.regOC>>>6]()で済む
          //      利点
          //        命令の処理を動的に組み替えることができる
          //      欠点
          //        XEiJ.IRP_ENUM_DIRECTよりも遅い
          //          配列参照による多分岐とenum bodyのメソッドの動的呼び出しによる多分岐の2段階になる
          //        XEiJ.IRP_STATICと共存させようとすると命令の処理を2回書かなければならず管理が面倒になる
          //        enum値の配列の初期化コードが大きくなるのでクラスを分ける必要がある
        irpSwitch:
          switch (XEiJ.regOC >>> 6) {  //第1オペコードの上位10ビット。XEiJ.regOCはゼロ拡張されているので0b1111_111_111&を省略

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ORI.B #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_000_mmm_rrr-{data}
            //OR.B #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_000_mmm_rrr-{data}  [ORI.B #<data>,<ea>]
            //ORI.B #<data>,CCR                               |-|012346|-|*****|*****|          |0000_000_000_111_100-{data}
          case 0b0000_000_000:
            irpOriByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ORI.W #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_001_mmm_rrr-{data}
            //OR.W #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_001_mmm_rrr-{data}  [ORI.W #<data>,<ea>]
            //ORI.W #<data>,SR                                |-|012346|P|*****|*****|          |0000_000_001_111_100-{data}
          case 0b0000_000_001:
            irpOriWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ORI.L #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_010_mmm_rrr-{data}
            //OR.L #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_010_mmm_rrr-{data}  [ORI.L #<data>,<ea>]
          case 0b0000_000_010:
            irpOriLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BITREV.L Dr                                     |-|------|-|-----|-----|D         |0000_000_011_000_rrr (ISA_C)
          case 0b0000_000_011:
            irpCmp2Chk2Byte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BTST.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_100_000_rrr
            //MOVEP.W (d16,Ar),Dq                             |-|01234S|-|-----|-----|          |0000_qqq_100_001_rrr-{data}
            //BTST.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZPI|0000_qqq_100_mmm_rrr
          case 0b0000_000_100:
          case 0b0000_001_100:
          case 0b0000_010_100:
          case 0b0000_011_100:
          case 0b0000_100_100:
          case 0b0000_101_100:
          case 0b0000_110_100:
          case 0b0000_111_100:
            irpBtstReg ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCHG.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_101_000_rrr
            //MOVEP.L (d16,Ar),Dq                             |-|01234S|-|-----|-----|          |0000_qqq_101_001_rrr-{data}
            //BCHG.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_101_mmm_rrr
          case 0b0000_000_101:
          case 0b0000_001_101:
          case 0b0000_010_101:
          case 0b0000_011_101:
          case 0b0000_100_101:
          case 0b0000_101_101:
          case 0b0000_110_101:
          case 0b0000_111_101:
            irpBchgReg ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCLR.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_110_000_rrr
            //MOVEP.W Dq,(d16,Ar)                             |-|01234S|-|-----|-----|          |0000_qqq_110_001_rrr-{data}
            //BCLR.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_110_mmm_rrr
          case 0b0000_000_110:
          case 0b0000_001_110:
          case 0b0000_010_110:
          case 0b0000_011_110:
          case 0b0000_100_110:
          case 0b0000_101_110:
          case 0b0000_110_110:
          case 0b0000_111_110:
            irpBclrReg ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BSET.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_111_000_rrr
            //MOVEP.L Dq,(d16,Ar)                             |-|01234S|-|-----|-----|          |0000_qqq_111_001_rrr-{data}
            //BSET.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_111_mmm_rrr
          case 0b0000_000_111:
          case 0b0000_001_111:
          case 0b0000_010_111:
          case 0b0000_011_111:
          case 0b0000_100_111:
          case 0b0000_101_111:
          case 0b0000_110_111:
          case 0b0000_111_111:
            irpBsetReg ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ANDI.B #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_000_mmm_rrr-{data}
            //AND.B #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_000_mmm_rrr-{data}  [ANDI.B #<data>,<ea>]
            //ANDI.B #<data>,CCR                              |-|012346|-|*****|*****|          |0000_001_000_111_100-{data}
          case 0b0000_001_000:
            irpAndiByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ANDI.W #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_001_mmm_rrr-{data}
            //AND.W #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_001_mmm_rrr-{data}  [ANDI.W #<data>,<ea>]
            //ANDI.W #<data>,SR                               |-|012346|P|*****|*****|          |0000_001_001_111_100-{data}
          case 0b0000_001_001:
            irpAndiWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ANDI.L #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_010_mmm_rrr-{data}
            //AND.L #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_010_mmm_rrr-{data}  [ANDI.L #<data>,<ea>]
          case 0b0000_001_010:
            irpAndiLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BYTEREV.L Dr                                    |-|------|-|-----|-----|D         |0000_001_011_000_rrr (ISA_C)
          case 0b0000_001_011:
            irpCmp2Chk2Word ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBI.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_000_mmm_rrr-{data}
            //SUB.B #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_000_mmm_rrr-{data}  [SUBI.B #<data>,<ea>]
          case 0b0000_010_000:
            irpSubiByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBI.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_001_mmm_rrr-{data}
            //SUB.W #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_001_mmm_rrr-{data}  [SUBI.W #<data>,<ea>]
          case 0b0000_010_001:
            irpSubiWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBI.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_010_mmm_rrr-{data}
            //SUB.L #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_010_mmm_rrr-{data}  [SUBI.L #<data>,<ea>]
          case 0b0000_010_010:
            irpSubiLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //FF1.L Dr                                        |-|------|-|-UUUU|-**00|D         |0000_010_011_000_rrr (ISA_C)
          case 0b0000_010_011:
            irpCmp2Chk2Long ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDI.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_000_mmm_rrr-{data}
          case 0b0000_011_000:
            irpAddiByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDI.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_001_mmm_rrr-{data}
          case 0b0000_011_001:
            irpAddiWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDI.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_010_mmm_rrr-{data}
          case 0b0000_011_010:
            irpAddiLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BTST.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_000_000_rrr-{data}
            //BTST.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZP |0000_100_000_mmm_rrr-{data}
          case 0b0000_100_000:
            irpBtstImm ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCHG.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_001_000_rrr-{data}
            //BCHG.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_001_mmm_rrr-{data}
          case 0b0000_100_001:
            irpBchgImm ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCLR.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_010_000_rrr-{data}
            //BCLR.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_010_mmm_rrr-{data}
          case 0b0000_100_010:
            irpBclrImm ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BSET.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_011_000_rrr-{data}
            //BSET.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_011_mmm_rrr-{data}
          case 0b0000_100_011:
            irpBsetImm ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EORI.B #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}
            //EOR.B #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}  [EORI.B #<data>,<ea>]
            //EORI.B #<data>,CCR                              |-|012346|-|*****|*****|          |0000_101_000_111_100-{data}
          case 0b0000_101_000:
            irpEoriByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EORI.W #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}
            //EOR.W #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}  [EORI.W #<data>,<ea>]
            //EORI.W #<data>,SR                               |-|012346|P|*****|*****|          |0000_101_001_111_100-{data}
          case 0b0000_101_001:
            irpEoriWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EORI.L #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}
            //EOR.L #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}  [EORI.L #<data>,<ea>]
          case 0b0000_101_010:
            irpEoriLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMPI.B #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_000_mmm_rrr-{data}
            //CMP.B #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_000_mmm_rrr-{data}  [CMPI.B #<data>,<ea>]
          case 0b0000_110_000:
            irpCmpiByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMPI.W #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_001_mmm_rrr-{data}
            //CMP.W #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_001_mmm_rrr-{data}  [CMPI.W #<data>,<ea>]
          case 0b0000_110_001:
            irpCmpiWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMPI.L #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_010_mmm_rrr-{data}
            //CMP.L #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_010_mmm_rrr-{data}  [CMPI.L #<data>,<ea>]
          case 0b0000_110_010:
            irpCmpiLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVES.B <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn000000000000
            //MOVES.B Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn100000000000
          case 0b0000_111_000:
            irpMovesByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVES.W <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn000000000000
            //MOVES.W Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn100000000000
          case 0b0000_111_001:
            irpMovesWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVES.L <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn000000000000
            //MOVES.L Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn100000000000
          case 0b0000_111_010:
            irpMovesLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,Dq                                  |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_000_mmm_rrr
          case 0b0001_000_000:
          case 0b0001_001_000:
          case 0b0001_010_000:
          case 0b0001_011_000:
          case 0b0001_100_000:
          case 0b0001_101_000:
          case 0b0001_110_000:
          case 0b0001_111_000:
            irpMoveToDRByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_010_mmm_rrr
          case 0b0001_000_010:
          case 0b0001_001_010:
          case 0b0001_010_010:
          case 0b0001_011_010:
          case 0b0001_100_010:
          case 0b0001_101_010:
          case 0b0001_110_010:
          case 0b0001_111_010:
            irpMoveToMMByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_011_mmm_rrr
          case 0b0001_000_011:
          case 0b0001_001_011:
          case 0b0001_010_011:
          case 0b0001_011_011:
          case 0b0001_100_011:
          case 0b0001_101_011:
          case 0b0001_110_011:
          case 0b0001_111_011:
            irpMoveToMPByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_100_mmm_rrr
          case 0b0001_000_100:
          case 0b0001_001_100:
          case 0b0001_010_100:
          case 0b0001_011_100:
          case 0b0001_100_100:
          case 0b0001_101_100:
          case 0b0001_110_100:
          case 0b0001_111_100:
            irpMoveToMNByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_101_mmm_rrr
          case 0b0001_000_101:
          case 0b0001_001_101:
          case 0b0001_010_101:
          case 0b0001_011_101:
          case 0b0001_100_101:
          case 0b0001_101_101:
          case 0b0001_110_101:
          case 0b0001_111_101:
            irpMoveToMWByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_110_mmm_rrr
          case 0b0001_000_110:
          case 0b0001_001_110:
          case 0b0001_010_110:
          case 0b0001_011_110:
          case 0b0001_100_110:
          case 0b0001_101_110:
          case 0b0001_110_110:
          case 0b0001_111_110:
            irpMoveToMXByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_000_111_mmm_rrr
          case 0b0001_000_111:
            irpMoveToZWByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.B <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_001_111_mmm_rrr
          case 0b0001_001_111:
            irpMoveToZLByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,Dq                                  |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_000_mmm_rrr
          case 0b0010_000_000:
          case 0b0010_001_000:
          case 0b0010_010_000:
          case 0b0010_011_000:
          case 0b0010_100_000:
          case 0b0010_101_000:
          case 0b0010_110_000:
          case 0b0010_111_000:
            irpMoveToDRLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVEA.L <ea>,Aq                                 |-|012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr
            //MOVE.L <ea>,Aq                                  |A|012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr [MOVEA.L <ea>,Aq]
          case 0b0010_000_001:
          case 0b0010_001_001:
          case 0b0010_010_001:
          case 0b0010_011_001:
          case 0b0010_100_001:
          case 0b0010_101_001:
          case 0b0010_110_001:
          case 0b0010_111_001:
            irpMoveaLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_010_mmm_rrr
          case 0b0010_000_010:
          case 0b0010_001_010:
          case 0b0010_010_010:
          case 0b0010_011_010:
          case 0b0010_100_010:
          case 0b0010_101_010:
          case 0b0010_110_010:
          case 0b0010_111_010:
            irpMoveToMMLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_011_mmm_rrr
          case 0b0010_000_011:
          case 0b0010_001_011:
          case 0b0010_010_011:
          case 0b0010_011_011:
          case 0b0010_100_011:
          case 0b0010_101_011:
          case 0b0010_110_011:
          case 0b0010_111_011:
            irpMoveToMPLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_100_mmm_rrr
          case 0b0010_000_100:
          case 0b0010_001_100:
          case 0b0010_010_100:
          case 0b0010_011_100:
          case 0b0010_100_100:
          case 0b0010_101_100:
          case 0b0010_110_100:
          case 0b0010_111_100:
            irpMoveToMNLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_101_mmm_rrr
          case 0b0010_000_101:
          case 0b0010_001_101:
          case 0b0010_010_101:
          case 0b0010_011_101:
          case 0b0010_100_101:
          case 0b0010_101_101:
          case 0b0010_110_101:
          case 0b0010_111_101:
            irpMoveToMWLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_110_mmm_rrr
          case 0b0010_000_110:
          case 0b0010_001_110:
          case 0b0010_010_110:
          case 0b0010_011_110:
          case 0b0010_100_110:
          case 0b0010_101_110:
          case 0b0010_110_110:
          case 0b0010_111_110:
            irpMoveToMXLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_000_111_mmm_rrr
          case 0b0010_000_111:
            irpMoveToZWLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.L <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_001_111_mmm_rrr
          case 0b0010_001_111:
            irpMoveToZLLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,Dq                                  |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_000_mmm_rrr
          case 0b0011_000_000:
          case 0b0011_001_000:
          case 0b0011_010_000:
          case 0b0011_011_000:
          case 0b0011_100_000:
          case 0b0011_101_000:
          case 0b0011_110_000:
          case 0b0011_111_000:
            irpMoveToDRWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVEA.W <ea>,Aq                                 |-|012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr
            //MOVE.W <ea>,Aq                                  |A|012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr [MOVEA.W <ea>,Aq]
          case 0b0011_000_001:
          case 0b0011_001_001:
          case 0b0011_010_001:
          case 0b0011_011_001:
          case 0b0011_100_001:
          case 0b0011_101_001:
          case 0b0011_110_001:
          case 0b0011_111_001:
            irpMoveaWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_010_mmm_rrr
          case 0b0011_000_010:
          case 0b0011_001_010:
          case 0b0011_010_010:
          case 0b0011_011_010:
          case 0b0011_100_010:
          case 0b0011_101_010:
          case 0b0011_110_010:
          case 0b0011_111_010:
            irpMoveToMMWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_011_mmm_rrr
          case 0b0011_000_011:
          case 0b0011_001_011:
          case 0b0011_010_011:
          case 0b0011_011_011:
          case 0b0011_100_011:
          case 0b0011_101_011:
          case 0b0011_110_011:
          case 0b0011_111_011:
            irpMoveToMPWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_100_mmm_rrr
          case 0b0011_000_100:
          case 0b0011_001_100:
          case 0b0011_010_100:
          case 0b0011_011_100:
          case 0b0011_100_100:
          case 0b0011_101_100:
          case 0b0011_110_100:
          case 0b0011_111_100:
            irpMoveToMNWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_101_mmm_rrr
          case 0b0011_000_101:
          case 0b0011_001_101:
          case 0b0011_010_101:
          case 0b0011_011_101:
          case 0b0011_100_101:
          case 0b0011_101_101:
          case 0b0011_110_101:
          case 0b0011_111_101:
            irpMoveToMWWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_110_mmm_rrr
          case 0b0011_000_110:
          case 0b0011_001_110:
          case 0b0011_010_110:
          case 0b0011_011_110:
          case 0b0011_100_110:
          case 0b0011_101_110:
          case 0b0011_110_110:
          case 0b0011_111_110:
            irpMoveToMXWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_000_111_mmm_rrr
          case 0b0011_000_111:
            irpMoveToZWWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_001_111_mmm_rrr
          case 0b0011_001_111:
            irpMoveToZLWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NEGX.B <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_000_mmm_rrr
          case 0b0100_000_000:
            irpNegxByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NEGX.W <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_001_mmm_rrr
          case 0b0100_000_001:
            irpNegxWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NEGX.L <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_010_mmm_rrr
          case 0b0100_000_010:
            irpNegxLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W SR,<ea>                                  |-|-12346|P|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr
          case 0b0100_000_011:
            irpMoveFromSR ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CHK.W <ea>,Dq                                   |-|012346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_110_mmm_rrr
          case 0b0100_000_110:
          case 0b0100_001_110:
          case 0b0100_010_110:
          case 0b0100_011_110:
          case 0b0100_100_110:
          case 0b0100_101_110:
          case 0b0100_110_110:
          case 0b0100_111_110:
            irpChkWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //LEA.L <ea>,Aq                                   |-|012346|-|-----|-----|  M  WXZP |0100_qqq_111_mmm_rrr
          case 0b0100_000_111:
          case 0b0100_001_111:
          case 0b0100_010_111:
          case 0b0100_011_111:
          case 0b0100_100_111:
          case 0b0100_101_111:
          case 0b0100_110_111:
          case 0b0100_111_111:
            irpLea ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CLR.B <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_000_mmm_rrr (68000 and 68008 read before clear)
          case 0b0100_001_000:
            irpClrByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CLR.W <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_001_mmm_rrr (68000 and 68008 read before clear)
          case 0b0100_001_001:
            irpClrWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CLR.L <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_010_mmm_rrr (68000 and 68008 read before clear)
          case 0b0100_001_010:
            irpClrLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W CCR,<ea>                                 |-|-12346|-|*****|-----|D M+-WXZ  |0100_001_011_mmm_rrr
          case 0b0100_001_011:
            irpMoveFromCCR ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NEG.B <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_000_mmm_rrr
          case 0b0100_010_000:
            irpNegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NEG.W <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_001_mmm_rrr
          case 0b0100_010_001:
            irpNegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NEG.L <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_010_mmm_rrr
          case 0b0100_010_010:
            irpNegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,CCR                                 |-|012346|-|UUUUU|*****|D M+-WXZPI|0100_010_011_mmm_rrr
          case 0b0100_010_011:
            irpMoveToCCR ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NOT.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_000_mmm_rrr
          case 0b0100_011_000:
            irpNotByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NOT.W <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_001_mmm_rrr
          case 0b0100_011_001:
            irpNotWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NOT.L <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_010_mmm_rrr
          case 0b0100_011_010:
            irpNotLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVE.W <ea>,SR                                  |-|012346|P|UUUUU|*****|D M+-WXZPI|0100_011_011_mmm_rrr
          case 0b0100_011_011:
            irpMoveToSR ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //NBCD.B <ea>                                     |-|012346|-|UUUUU|*U*U*|D M+-WXZ  |0100_100_000_mmm_rrr
          case 0b0100_100_000:
            irpNbcd ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SWAP.W Dr                                       |-|012346|-|-UUUU|-**00|D         |0100_100_001_000_rrr
            //BKPT #<data>                                    |-|-12346|-|-----|-----|          |0100_100_001_001_ddd
            //PEA.L <ea>                                      |-|012346|-|-----|-----|  M  WXZP |0100_100_001_mmm_rrr
          case 0b0100_100_001:
            irpPea ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EXT.W Dr                                        |-|012346|-|-UUUU|-**00|D         |0100_100_010_000_rrr
            //MOVEM.W <list>,<ea>                             |-|012346|-|-----|-----|  M -WXZ  |0100_100_010_mmm_rrr-llllllllllllllll
          case 0b0100_100_010:
            irpMovemToMemWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EXT.L Dr                                        |-|012346|-|-UUUU|-**00|D         |0100_100_011_000_rrr
            //MOVEM.L <list>,<ea>                             |-|012346|-|-----|-----|  M -WXZ  |0100_100_011_mmm_rrr-llllllllllllllll
          case 0b0100_100_011:
            irpMovemToMemLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //TST.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_000_mmm_rrr
          case 0b0100_101_000:
            irpTstByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //TST.W <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_001_mmm_rrr
          case 0b0100_101_001:
            irpTstWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //TST.L <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_010_mmm_rrr
          case 0b0100_101_010:
            irpTstLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //TAS.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_011_mmm_rrr
            //ILLEGAL                                         |-|012346|-|-----|-----|          |0100_101_011_111_100
          case 0b0100_101_011:
            irpTas ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SATS.L Dr                                       |-|------|-|-UUUU|-**00|D         |0100_110_010_000_rrr (ISA_B)
            //MOVEM.W <ea>,<list>                             |-|012346|-|-----|-----|  M+ WXZP |0100_110_010_mmm_rrr-llllllllllllllll
          case 0b0100_110_010:
            irpMovemToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MOVEM.L <ea>,<list>                             |-|012346|-|-----|-----|  M+ WXZP |0100_110_011_mmm_rrr-llllllllllllllll
          case 0b0100_110_011:
            irpMovemToRegLong ();
            break irpSwitch;

          case 0b0100_111_001:
            switch (XEiJ.regOC & 0b111_111) {

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //TRAP #<vector>                                  |-|012346|-|-----|-----|          |0100_111_001_00v_vvv
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
              irpTrap ();
              break irpSwitch;
            case 0b001_111:
              irpTrap15 ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //LINK.W Ar,#<data>                               |-|012346|-|-----|-----|          |0100_111_001_010_rrr-{data}
            case 0b010_000:
            case 0b010_001:
            case 0b010_010:
            case 0b010_011:
            case 0b010_100:
            case 0b010_101:
            case 0b010_110:
            case 0b010_111:
              irpLinkWord ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //UNLK Ar                                         |-|012346|-|-----|-----|          |0100_111_001_011_rrr
            case 0b011_000:
            case 0b011_001:
            case 0b011_010:
            case 0b011_011:
            case 0b011_100:
            case 0b011_101:
            case 0b011_110:
            case 0b011_111:
              irpUnlk ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //MOVE.L Ar,USP                                   |-|012346|P|-----|-----|          |0100_111_001_100_rrr
            case 0b100_000:
            case 0b100_001:
            case 0b100_010:
            case 0b100_011:
            case 0b100_100:
            case 0b100_101:
            case 0b100_110:
            case 0b100_111:
              irpMoveToUsp ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //MOVE.L USP,Ar                                   |-|012346|P|-----|-----|          |0100_111_001_101_rrr
            case 0b101_000:
            case 0b101_001:
            case 0b101_010:
            case 0b101_011:
            case 0b101_100:
            case 0b101_101:
            case 0b101_110:
            case 0b101_111:
              irpMoveFromUsp ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //RESET                                           |-|012346|P|-----|-----|          |0100_111_001_110_000
            case 0b110_000:
              irpReset ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //NOP                                             |-|012346|-|-----|-----|          |0100_111_001_110_001
            case 0b110_001:
              irpNop ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //STOP #<data>                                    |-|012346|P|UUUUU|*****|          |0100_111_001_110_010-{data}
            case 0b110_010:
              irpStop ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //RTE                                             |-|012346|P|UUUUU|*****|          |0100_111_001_110_011
            case 0b110_011:
              irpRte ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //RTD #<data>                                     |-|-12346|-|-----|-----|          |0100_111_001_110_100-{data}
            case 0b110_100:
              irpRtd ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //RTS                                             |-|012346|-|-----|-----|          |0100_111_001_110_101
            case 0b110_101:
              irpRts ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //TRAPV                                           |-|012346|-|---*-|-----|          |0100_111_001_110_110
            case 0b110_110:
              irpTrapv ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //RTR                                             |-|012346|-|UUUUU|*****|          |0100_111_001_110_111
            case 0b110_111:
              irpRtr ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //MOVEC.L Rc,Rn                                   |-|-12346|P|-----|-----|          |0100_111_001_111_010-rnnncccccccccccc
            case 0b111_010:
              irpMovecFromControl ();
              break irpSwitch;

              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
              //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
              //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
              //MOVEC.L Rn,Rc                                   |-|-12346|P|-----|-----|          |0100_111_001_111_011-rnnncccccccccccc
            case 0b111_011:
              irpMovecToControl ();
              break irpSwitch;

            default:
              irpIllegal ();

            }  //switch XEiJ.regOC & 0b111_111
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //JSR <ea>                                        |-|012346|-|-----|-----|  M  WXZP |0100_111_010_mmm_rrr
            //JBSR.L <label>                                  |A|012346|-|-----|-----|          |0100_111_010_111_001-{address}       [JSR <label>]
          case 0b0100_111_010:
            irpJsr ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //JMP <ea>                                        |-|012346|-|-----|-----|  M  WXZP |0100_111_011_mmm_rrr
            //JBRA.L <label>                                  |A|012346|-|-----|-----|          |0100_111_011_111_001-{address}       [JMP <label>]
          case 0b0100_111_011:
            irpJmp ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDQ.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_000_mmm_rrr
            //INC.B <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_000_mmm_rrr [ADDQ.B #1,<ea>]
          case 0b0101_000_000:
          case 0b0101_001_000:
          case 0b0101_010_000:
          case 0b0101_011_000:
          case 0b0101_100_000:
          case 0b0101_101_000:
          case 0b0101_110_000:
          case 0b0101_111_000:
            irpAddqByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDQ.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_001_mmm_rrr
            //ADDQ.W #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_001_001_rrr
            //INC.W <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_001_mmm_rrr [ADDQ.W #1,<ea>]
            //INC.W Ar                                        |A|012346|-|-----|-----| A        |0101_001_001_001_rrr [ADDQ.W #1,Ar]
          case 0b0101_000_001:
          case 0b0101_001_001:
          case 0b0101_010_001:
          case 0b0101_011_001:
          case 0b0101_100_001:
          case 0b0101_101_001:
          case 0b0101_110_001:
          case 0b0101_111_001:
            irpAddqWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDQ.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_010_mmm_rrr
            //ADDQ.L #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_010_001_rrr
            //INC.L <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_010_mmm_rrr [ADDQ.L #1,<ea>]
            //INC.L Ar                                        |A|012346|-|-----|-----| A        |0101_001_010_001_rrr [ADDQ.L #1,Ar]
          case 0b0101_000_010:
          case 0b0101_001_010:
          case 0b0101_010_010:
          case 0b0101_011_010:
          case 0b0101_100_010:
          case 0b0101_101_010:
          case 0b0101_110_010:
          case 0b0101_111_010:
            irpAddqLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ST.B <ea>                                       |-|012346|-|-----|-----|D M+-WXZ  |0101_000_011_mmm_rrr
            //SNF.B <ea>                                      |A|012346|-|-----|-----|D M+-WXZ  |0101_000_011_mmm_rrr [ST.B <ea>]
            //DBT.W Dr,<label>                                |-|012346|-|-----|-----|          |0101_000_011_001_rrr-{offset}
            //DBNF.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_011_001_rrr-{offset}        [DBT.W Dr,<label>]
          case 0b0101_000_011:
            irpSt ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBQ.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_100_mmm_rrr
            //DEC.B <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_100_mmm_rrr [SUBQ.B #1,<ea>]
          case 0b0101_000_100:
          case 0b0101_001_100:
          case 0b0101_010_100:
          case 0b0101_011_100:
          case 0b0101_100_100:
          case 0b0101_101_100:
          case 0b0101_110_100:
          case 0b0101_111_100:
            irpSubqByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBQ.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_101_mmm_rrr
            //SUBQ.W #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_101_001_rrr
            //DEC.W <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_101_mmm_rrr [SUBQ.W #1,<ea>]
            //DEC.W Ar                                        |A|012346|-|-----|-----| A        |0101_001_101_001_rrr [SUBQ.W #1,Ar]
          case 0b0101_000_101:
          case 0b0101_001_101:
          case 0b0101_010_101:
          case 0b0101_011_101:
          case 0b0101_100_101:
          case 0b0101_101_101:
          case 0b0101_110_101:
          case 0b0101_111_101:
            irpSubqWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBQ.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_110_mmm_rrr
            //SUBQ.L #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_110_001_rrr
            //DEC.L <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_110_mmm_rrr [SUBQ.L #1,<ea>]
            //DEC.L Ar                                        |A|012346|-|-----|-----| A        |0101_001_110_001_rrr [SUBQ.L #1,Ar]
          case 0b0101_000_110:
          case 0b0101_001_110:
          case 0b0101_010_110:
          case 0b0101_011_110:
          case 0b0101_100_110:
          case 0b0101_101_110:
          case 0b0101_110_110:
          case 0b0101_111_110:
            irpSubqLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SF.B <ea>                                       |-|012346|-|-----|-----|D M+-WXZ  |0101_000_111_mmm_rrr
            //SNT.B <ea>                                      |A|012346|-|-----|-----|D M+-WXZ  |0101_000_111_mmm_rrr [SF.B <ea>]
            //DBF.W Dr,<label>                                |-|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}
            //DBNT.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}        [DBF.W Dr,<label>]
            //DBRA.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}        [DBF.W Dr,<label>]
          case 0b0101_000_111:
            irpSf ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SHI.B <ea>                                      |-|012346|-|--*-*|-----|D M+-WXZ  |0101_001_011_mmm_rrr
            //SNLS.B <ea>                                     |A|012346|-|--*-*|-----|D M+-WXZ  |0101_001_011_mmm_rrr [SHI.B <ea>]
            //DBHI.W Dr,<label>                               |-|012346|-|--*-*|-----|          |0101_001_011_001_rrr-{offset}
            //DBNLS.W Dr,<label>                              |A|012346|-|--*-*|-----|          |0101_001_011_001_rrr-{offset}        [DBHI.W Dr,<label>]
          case 0b0101_001_011:
            irpShi ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SLS.B <ea>                                      |-|012346|-|--*-*|-----|D M+-WXZ  |0101_001_111_mmm_rrr
            //SNHI.B <ea>                                     |A|012346|-|--*-*|-----|D M+-WXZ  |0101_001_111_mmm_rrr [SLS.B <ea>]
            //DBLS.W Dr,<label>                               |-|012346|-|--*-*|-----|          |0101_001_111_001_rrr-{offset}
            //DBNHI.W Dr,<label>                              |A|012346|-|--*-*|-----|          |0101_001_111_001_rrr-{offset}        [DBLS.W Dr,<label>]
          case 0b0101_001_111:
            irpSls ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SCC.B <ea>                                      |-|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr
            //SHS.B <ea>                                      |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
            //SNCS.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
            //SNLO.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
            //DBCC.W Dr,<label>                               |-|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}
            //DBHS.W Dr,<label>                               |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
            //DBNCS.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
            //DBNLO.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
          case 0b0101_010_011:
            irpShs ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SCS.B <ea>                                      |-|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr
            //SLO.B <ea>                                      |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
            //SNCC.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
            //SNHS.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
            //DBCS.W Dr,<label>                               |-|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}
            //DBLO.W Dr,<label>                               |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
            //DBNCC.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
            //DBNHS.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
          case 0b0101_010_111:
            irpSlo ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SNE.B <ea>                                      |-|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr
            //SNEQ.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
            //SNZ.B <ea>                                      |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
            //SNZE.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
            //DBNE.W Dr,<label>                               |-|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}
            //DBNEQ.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
            //DBNZ.W Dr,<label>                               |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
            //DBNZE.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
          case 0b0101_011_011:
            irpSne ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SEQ.B <ea>                                      |-|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr
            //SNNE.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
            //SNNZ.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
            //SZE.B <ea>                                      |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
            //DBEQ.W Dr,<label>                               |-|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}
            //DBNNE.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
            //DBNNZ.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
            //DBZE.W Dr,<label>                               |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
          case 0b0101_011_111:
            irpSeq ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SVC.B <ea>                                      |-|012346|-|---*-|-----|D M+-WXZ  |0101_100_011_mmm_rrr
            //SNVS.B <ea>                                     |A|012346|-|---*-|-----|D M+-WXZ  |0101_100_011_mmm_rrr [SVC.B <ea>]
            //DBVC.W Dr,<label>                               |-|012346|-|---*-|-----|          |0101_100_011_001_rrr-{offset}
            //DBNVS.W Dr,<label>                              |A|012346|-|---*-|-----|          |0101_100_011_001_rrr-{offset}        [DBVC.W Dr,<label>]
          case 0b0101_100_011:
            irpSvc ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SVS.B <ea>                                      |-|012346|-|---*-|-----|D M+-WXZ  |0101_100_111_mmm_rrr
            //SNVC.B <ea>                                     |A|012346|-|---*-|-----|D M+-WXZ  |0101_100_111_mmm_rrr [SVS.B <ea>]
            //DBVS.W Dr,<label>                               |-|012346|-|---*-|-----|          |0101_100_111_001_rrr-{offset}
            //DBNVC.W Dr,<label>                              |A|012346|-|---*-|-----|          |0101_100_111_001_rrr-{offset}        [DBVS.W Dr,<label>]
          case 0b0101_100_111:
            irpSvs ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SPL.B <ea>                                      |-|012346|-|-*---|-----|D M+-WXZ  |0101_101_011_mmm_rrr
            //SNMI.B <ea>                                     |A|012346|-|-*---|-----|D M+-WXZ  |0101_101_011_mmm_rrr [SPL.B <ea>]
            //DBPL.W Dr,<label>                               |-|012346|-|-*---|-----|          |0101_101_011_001_rrr-{offset}
            //DBNMI.W Dr,<label>                              |A|012346|-|-*---|-----|          |0101_101_011_001_rrr-{offset}        [DBPL.W Dr,<label>]
          case 0b0101_101_011:
            irpSpl ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SMI.B <ea>                                      |-|012346|-|-*---|-----|D M+-WXZ  |0101_101_111_mmm_rrr
            //SNPL.B <ea>                                     |A|012346|-|-*---|-----|D M+-WXZ  |0101_101_111_mmm_rrr [SMI.B <ea>]
            //DBMI.W Dr,<label>                               |-|012346|-|-*---|-----|          |0101_101_111_001_rrr-{offset}
            //DBNPL.W Dr,<label>                              |A|012346|-|-*---|-----|          |0101_101_111_001_rrr-{offset}        [DBMI.W Dr,<label>]
          case 0b0101_101_111:
            irpSmi ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SGE.B <ea>                                      |-|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_011_mmm_rrr
            //SNLT.B <ea>                                     |A|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_011_mmm_rrr [SGE.B <ea>]
            //DBGE.W Dr,<label>                               |-|012346|-|-*-*-|-----|          |0101_110_011_001_rrr-{offset}
            //DBNLT.W Dr,<label>                              |A|012346|-|-*-*-|-----|          |0101_110_011_001_rrr-{offset}        [DBGE.W Dr,<label>]
          case 0b0101_110_011:
            irpSge ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SLT.B <ea>                                      |-|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_111_mmm_rrr
            //SNGE.B <ea>                                     |A|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_111_mmm_rrr [SLT.B <ea>]
            //DBLT.W Dr,<label>                               |-|012346|-|-*-*-|-----|          |0101_110_111_001_rrr-{offset}
            //DBNGE.W Dr,<label>                              |A|012346|-|-*-*-|-----|          |0101_110_111_001_rrr-{offset}        [DBLT.W Dr,<label>]
          case 0b0101_110_111:
            irpSlt ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SGT.B <ea>                                      |-|012346|-|-***-|-----|D M+-WXZ  |0101_111_011_mmm_rrr
            //SNLE.B <ea>                                     |A|012346|-|-***-|-----|D M+-WXZ  |0101_111_011_mmm_rrr [SGT.B <ea>]
            //DBGT.W Dr,<label>                               |-|012346|-|-***-|-----|          |0101_111_011_001_rrr-{offset}
            //DBNLE.W Dr,<label>                              |A|012346|-|-***-|-----|          |0101_111_011_001_rrr-{offset}        [DBGT.W Dr,<label>]
          case 0b0101_111_011:
            irpSgt ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SLE.B <ea>                                      |-|012346|-|-***-|-----|D M+-WXZ  |0101_111_111_mmm_rrr
            //SNGT.B <ea>                                     |A|012346|-|-***-|-----|D M+-WXZ  |0101_111_111_mmm_rrr [SLE.B <ea>]
            //DBLE.W Dr,<label>                               |-|012346|-|-***-|-----|          |0101_111_111_001_rrr-{offset}
            //DBNGT.W Dr,<label>                              |A|012346|-|-***-|-----|          |0101_111_111_001_rrr-{offset}        [DBLE.W Dr,<label>]
          case 0b0101_111_111:
            irpSle ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BRA.W <label>                                   |-|012346|-|-----|-----|          |0110_000_000_000_000-{offset}
            //JBRA.W <label>                                  |A|012346|-|-----|-----|          |0110_000_000_000_000-{offset}        [BRA.W <label>]
            //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_000_sss_sss (s is not equal to 0)
            //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_000_sss_sss (s is not equal to 0)   [BRA.S <label>]
          case 0b0110_000_000:
            irpBrasw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_001_sss_sss
            //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_001_sss_sss [BRA.S <label>]
          case 0b0110_000_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_010_sss_sss
            //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_010_sss_sss [BRA.S <label>]
          case 0b0110_000_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BRA.S <label>                                   |-|01----|-|-----|-----|          |0110_000_011_sss_sss
            //JBRA.S <label>                                  |A|01----|-|-----|-----|          |0110_000_011_sss_sss [BRA.S <label>]
          case 0b0110_000_011:
            irpBras ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BSR.W <label>                                   |-|012346|-|-----|-----|          |0110_000_100_000_000-{offset}
            //JBSR.W <label>                                  |A|012346|-|-----|-----|          |0110_000_100_000_000-{offset}        [BSR.W <label>]
            //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_100_sss_sss (s is not equal to 0)
            //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_100_sss_sss (s is not equal to 0)   [BSR.S <label>]
          case 0b0110_000_100:
            irpBsrsw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_101_sss_sss
            //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_101_sss_sss [BSR.S <label>]
          case 0b0110_000_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_110_sss_sss
            //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_110_sss_sss [BSR.S <label>]
          case 0b0110_000_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BSR.S <label>                                   |-|01----|-|-----|-----|          |0110_000_111_sss_sss
            //JBSR.S <label>                                  |A|01----|-|-----|-----|          |0110_000_111_sss_sss [BSR.S <label>]
          case 0b0110_000_111:
            irpBsrs ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BHI.W <label>                                   |-|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}
            //BNLS.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
            //JBHI.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
            //JBNLS.W <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
            //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)
            //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
            //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
            //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
            //JBLS.L <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_110-0100111011111001-{address}      [BHI.S (*)+8;JMP <label>]
            //JBNHI.L <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_000_110-0100111011111001-{address}      [BHI.S (*)+8;JMP <label>]
          case 0b0110_001_000:
            irpBhisw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_001_sss_sss
            //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
            //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
            //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
          case 0b0110_001_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_010_sss_sss
            //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
            //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
            //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
          case 0b0110_001_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BHI.S <label>                                   |-|01----|-|--*-*|-----|          |0110_001_011_sss_sss
            //BNLS.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
            //JBHI.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
            //JBNLS.S <label>                                 |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
          case 0b0110_001_011:
            irpBhis ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLS.W <label>                                   |-|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}
            //BNHI.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
            //JBLS.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
            //JBNHI.W <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
            //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)
            //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
            //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
            //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
            //JBHI.L <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_110-0100111011111001-{address}      [BLS.S (*)+8;JMP <label>]
            //JBNLS.L <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_000_110-0100111011111001-{address}      [BLS.S (*)+8;JMP <label>]
          case 0b0110_001_100:
            irpBlssw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_101_sss_sss
            //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
            //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
            //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
          case 0b0110_001_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_110_sss_sss
            //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
            //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
            //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
          case 0b0110_001_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLS.S <label>                                   |-|01----|-|--*-*|-----|          |0110_001_111_sss_sss
            //BNHI.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
            //JBLS.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
            //JBNHI.S <label>                                 |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
          case 0b0110_001_111:
            irpBlss ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCC.W <label>                                   |-|012346|-|----*|-----|          |0110_010_000_000_000-{offset}
            //BHS.W <label>                                   |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //BNCS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //BNLO.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //JBCC.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //JBHS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //JBNCS.W <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //JBNLO.W <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
            //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)
            //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
            //JBCS.L <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
            //JBLO.L <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
            //JBNCC.L <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
            //JBNHS.L <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
          case 0b0110_010_000:
            irpBhssw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_001_sss_sss
            //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
            //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
            //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
            //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
            //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
            //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
            //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
          case 0b0110_010_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_010_sss_sss
            //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
            //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
            //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
            //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
            //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
            //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
            //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
          case 0b0110_010_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCC.S <label>                                   |-|01----|-|----*|-----|          |0110_010_011_sss_sss
            //BHS.S <label>                                   |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
            //BNCS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
            //BNLO.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
            //JBCC.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
            //JBHS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
            //JBNCS.S <label>                                 |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
            //JBNLO.S <label>                                 |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
          case 0b0110_010_011:
            irpBhss ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCS.W <label>                                   |-|012346|-|----*|-----|          |0110_010_100_000_000-{offset}
            //BLO.W <label>                                   |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //BNCC.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //BNHS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //JBCS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //JBLO.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //JBNCC.W <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //JBNHS.W <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
            //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)
            //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
            //JBCC.L <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
            //JBHS.L <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
            //JBNCS.L <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
            //JBNLO.L <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
          case 0b0110_010_100:
            irpBlosw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_101_sss_sss
            //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
            //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
            //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
            //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
            //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
            //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
            //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
          case 0b0110_010_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_110_sss_sss
            //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
            //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
            //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
            //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
            //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
            //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
            //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
          case 0b0110_010_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BCS.S <label>                                   |-|01----|-|----*|-----|          |0110_010_111_sss_sss
            //BLO.S <label>                                   |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
            //BNCC.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
            //BNHS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
            //JBCS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
            //JBLO.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
            //JBNCC.S <label>                                 |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
            //JBNHS.S <label>                                 |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
          case 0b0110_010_111:
            irpBlos ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BNE.W <label>                                   |-|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}
            //BNEQ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //BNZ.W <label>                                   |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //BNZE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //JBNE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //JBNEQ.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //JBNZ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //JBNZE.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
            //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)
            //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
            //JBEQ.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
            //JBNEQ.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
            //JBNNE.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
            //JBNNZ.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
            //JBNZ.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
            //JBNZE.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
            //JBZE.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
          case 0b0110_011_000:
            irpBnesw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_001_sss_sss
            //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
            //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
            //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
            //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
            //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
            //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
            //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
          case 0b0110_011_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_010_sss_sss
            //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
            //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
            //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
            //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
            //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
            //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
            //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
          case 0b0110_011_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BNE.S <label>                                   |-|01----|-|--*--|-----|          |0110_011_011_sss_sss
            //BNEQ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
            //BNZ.S <label>                                   |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
            //BNZE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
            //JBNE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
            //JBNEQ.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
            //JBNZ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
            //JBNZE.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
          case 0b0110_011_011:
            irpBnes ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BEQ.W <label>                                   |-|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}
            //BNNE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //BNNZ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //BZE.W <label>                                   |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //JBEQ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //JBNNE.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //JBNNZ.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //JBZE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
            //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)
            //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
            //JBNE.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_110-0100111011111001-{address}      [BEQ.S (*)+8;JMP <label>]
          case 0b0110_011_100:
            irpBeqsw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_101_sss_sss
            //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
            //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
            //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
            //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
            //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
            //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
            //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
          case 0b0110_011_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_110_sss_sss
            //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
            //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
            //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
            //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
            //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
            //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
            //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
          case 0b0110_011_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BEQ.S <label>                                   |-|01----|-|--*--|-----|          |0110_011_111_sss_sss
            //BNNE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
            //BNNZ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
            //BZE.S <label>                                   |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
            //JBEQ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
            //JBNNE.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
            //JBNNZ.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
            //JBZE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
          case 0b0110_011_111:
            irpBeqs ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVC.W <label>                                   |-|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}
            //BNVS.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
            //JBNVS.W <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
            //JBVC.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
            //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)
            //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
            //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
            //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
            //JBNVC.L <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_000_110-0100111011111001-{address}      [BVC.S (*)+8;JMP <label>]
            //JBVS.L <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_110-0100111011111001-{address}      [BVC.S (*)+8;JMP <label>]
          case 0b0110_100_000:
            irpBvcsw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_001_sss_sss
            //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
            //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
            //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
          case 0b0110_100_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_010_sss_sss
            //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
            //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
            //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
          case 0b0110_100_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVC.S <label>                                   |-|01----|-|---*-|-----|          |0110_100_011_sss_sss
            //BNVS.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
            //JBNVS.S <label>                                 |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
            //JBVC.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
          case 0b0110_100_011:
            irpBvcs ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVS.W <label>                                   |-|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}
            //BNVC.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
            //JBNVC.W <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
            //JBVS.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
            //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)
            //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
            //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
            //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
            //JBNVS.L <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_000_110-0100111011111001-{address}      [BVS.S (*)+8;JMP <label>]
            //JBVC.L <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_110-0100111011111001-{address}      [BVS.S (*)+8;JMP <label>]
          case 0b0110_100_100:
            irpBvssw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_101_sss_sss
            //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
            //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
            //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
          case 0b0110_100_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_110_sss_sss
            //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
            //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
            //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
          case 0b0110_100_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BVS.S <label>                                   |-|01----|-|---*-|-----|          |0110_100_111_sss_sss
            //BNVC.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
            //JBNVC.S <label>                                 |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
            //JBVS.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
          case 0b0110_100_111:
            irpBvss ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BPL.W <label>                                   |-|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}
            //BNMI.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
            //JBNMI.W <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
            //JBPL.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
            //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)
            //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
            //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
            //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
            //JBMI.L <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_110-0100111011111001-{address}      [BPL.S (*)+8;JMP <label>]
            //JBNPL.L <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_000_110-0100111011111001-{address}      [BPL.S (*)+8;JMP <label>]
          case 0b0110_101_000:
            irpBplsw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_001_sss_sss
            //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
            //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
            //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
          case 0b0110_101_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_010_sss_sss
            //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
            //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
            //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
          case 0b0110_101_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BPL.S <label>                                   |-|01----|-|-*---|-----|          |0110_101_011_sss_sss
            //BNMI.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
            //JBNMI.S <label>                                 |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
            //JBPL.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
          case 0b0110_101_011:
            irpBpls ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BMI.W <label>                                   |-|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}
            //BNPL.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
            //JBMI.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
            //JBNPL.W <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
            //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)
            //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
            //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
            //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
            //JBNMI.L <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_000_110-0100111011111001-{address}      [BMI.S (*)+8;JMP <label>]
            //JBPL.L <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_110-0100111011111001-{address}      [BMI.S (*)+8;JMP <label>]
          case 0b0110_101_100:
            irpBmisw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_101_sss_sss
            //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
            //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
            //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
          case 0b0110_101_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_110_sss_sss
            //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
            //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
            //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
          case 0b0110_101_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BMI.S <label>                                   |-|01----|-|-*---|-----|          |0110_101_111_sss_sss
            //BNPL.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
            //JBMI.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
            //JBNPL.S <label>                                 |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
          case 0b0110_101_111:
            irpBmis ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGE.W <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}
            //BNLT.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
            //JBGE.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
            //JBNLT.W <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
            //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)
            //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
            //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
            //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
            //JBLT.L <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_110-0100111011111001-{address}      [BGE.S (*)+8;JMP <label>]
            //JBNGE.L <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_000_110-0100111011111001-{address}      [BGE.S (*)+8;JMP <label>]
          case 0b0110_110_000:
            irpBgesw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_001_sss_sss
            //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
            //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
            //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
          case 0b0110_110_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_010_sss_sss
            //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
            //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
            //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
          case 0b0110_110_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGE.S <label>                                   |-|01----|-|-*-*-|-----|          |0110_110_011_sss_sss
            //BNLT.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
            //JBGE.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
            //JBNLT.S <label>                                 |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
          case 0b0110_110_011:
            irpBges ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLT.W <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}
            //BNGE.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
            //JBLT.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
            //JBNGE.W <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
            //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)
            //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
            //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
            //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
            //JBGE.L <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_110-0100111011111001-{address}      [BLT.S (*)+8;JMP <label>]
            //JBNLT.L <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_000_110-0100111011111001-{address}      [BLT.S (*)+8;JMP <label>]
          case 0b0110_110_100:
            irpBltsw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_101_sss_sss
            //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
            //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
            //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
          case 0b0110_110_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_110_sss_sss
            //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
            //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
            //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
          case 0b0110_110_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLT.S <label>                                   |-|01----|-|-*-*-|-----|          |0110_110_111_sss_sss
            //BNGE.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
            //JBLT.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
            //JBNGE.S <label>                                 |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
          case 0b0110_110_111:
            irpBlts ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGT.W <label>                                   |-|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}
            //BNLE.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
            //JBGT.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
            //JBNLE.W <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
            //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)
            //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
            //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
            //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
            //JBLE.L <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_110-0100111011111001-{address}      [BGT.S (*)+8;JMP <label>]
            //JBNGT.L <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_000_110-0100111011111001-{address}      [BGT.S (*)+8;JMP <label>]
          case 0b0110_111_000:
            irpBgtsw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_001_sss_sss
            //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
            //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
            //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
          case 0b0110_111_001:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_010_sss_sss
            //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
            //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
            //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
          case 0b0110_111_010:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BGT.S <label>                                   |-|01----|-|-***-|-----|          |0110_111_011_sss_sss
            //BNLE.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
            //JBGT.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
            //JBNLE.S <label>                                 |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
          case 0b0110_111_011:
            irpBgts ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLE.W <label>                                   |-|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}
            //BNGT.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
            //JBLE.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
            //JBNGT.W <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
            //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)
            //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
            //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
            //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
            //JBGT.L <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_110-0100111011111001-{address}      [BLE.S (*)+8;JMP <label>]
            //JBNLE.L <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_000_110-0100111011111001-{address}      [BLE.S (*)+8;JMP <label>]
          case 0b0110_111_100:
            irpBlesw ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_101_sss_sss
            //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
            //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
            //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
          case 0b0110_111_101:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_110_sss_sss
            //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
            //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
            //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
          case 0b0110_111_110:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //BLE.S <label>                                   |-|01----|-|-***-|-----|          |0110_111_111_sss_sss
            //BNGT.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
            //JBLE.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
            //JBNGT.S <label>                                 |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
          case 0b0110_111_111:
            irpBles ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //IOCS <name>                                     |A|012346|-|UUUUU|UUUUU|          |0111_000_0dd_ddd_ddd-0100111001001111        [MOVEQ.L #<data>,D0;TRAP #15]
            //MOVEQ.L #<data>,Dq                              |-|012346|-|-UUUU|-**00|          |0111_qqq_0dd_ddd_ddd
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
          case 0b0111_111_011:
            irpMoveq ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MVS.B <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr (ISA_B)
          case 0b0111_000_100:
          case 0b0111_001_100:
          case 0b0111_010_100:
          case 0b0111_011_100:
          case 0b0111_100_100:
          case 0b0111_101_100:
          case 0b0111_110_100:
          case 0b0111_111_100:
            irpMvsByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MVS.W <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr (ISA_B)
          case 0b0111_000_101:
          case 0b0111_001_101:
          case 0b0111_010_101:
          case 0b0111_011_101:
          case 0b0111_100_101:
          case 0b0111_101_101:
          case 0b0111_110_101:
          case 0b0111_111_101:
            irpMvsWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MVZ.B <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr (ISA_B)
          case 0b0111_000_110:
          case 0b0111_001_110:
          case 0b0111_010_110:
          case 0b0111_011_110:
          case 0b0111_100_110:
          case 0b0111_101_110:
          case 0b0111_110_110:
          case 0b0111_111_110:
            irpMvzByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MVZ.W <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr (ISA_B)
          case 0b0111_000_111:
          case 0b0111_001_111:
          case 0b0111_010_111:
          case 0b0111_011_111:
          case 0b0111_100_111:
          case 0b0111_101_111:
          case 0b0111_110_111:
          case 0b0111_111_111:
            irpMvzWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //OR.B <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_000_mmm_rrr
          case 0b1000_000_000:
          case 0b1000_001_000:
          case 0b1000_010_000:
          case 0b1000_011_000:
          case 0b1000_100_000:
          case 0b1000_101_000:
          case 0b1000_110_000:
          case 0b1000_111_000:
            irpOrToRegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //OR.W <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_001_mmm_rrr
          case 0b1000_000_001:
          case 0b1000_001_001:
          case 0b1000_010_001:
          case 0b1000_011_001:
          case 0b1000_100_001:
          case 0b1000_101_001:
          case 0b1000_110_001:
          case 0b1000_111_001:
            irpOrToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //OR.L <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_010_mmm_rrr
          case 0b1000_000_010:
          case 0b1000_001_010:
          case 0b1000_010_010:
          case 0b1000_011_010:
          case 0b1000_100_010:
          case 0b1000_101_010:
          case 0b1000_110_010:
          case 0b1000_111_010:
            irpOrToRegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //DIVU.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_011_mmm_rrr
          case 0b1000_000_011:
          case 0b1000_001_011:
          case 0b1000_010_011:
          case 0b1000_011_011:
          case 0b1000_100_011:
          case 0b1000_101_011:
          case 0b1000_110_011:
          case 0b1000_111_011:
            irpDivuWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SBCD.B Dr,Dq                                    |-|012346|-|UUUUU|*U*U*|          |1000_qqq_100_000_rrr
            //SBCD.B -(Ar),-(Aq)                              |-|012346|-|UUUUU|*U*U*|          |1000_qqq_100_001_rrr
            //OR.B Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_100_mmm_rrr
          case 0b1000_000_100:
          case 0b1000_001_100:
          case 0b1000_010_100:
          case 0b1000_011_100:
          case 0b1000_100_100:
          case 0b1000_101_100:
          case 0b1000_110_100:
          case 0b1000_111_100:
            irpOrToMemByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //OR.W Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_101_mmm_rrr
          case 0b1000_000_101:
          case 0b1000_001_101:
          case 0b1000_010_101:
          case 0b1000_011_101:
          case 0b1000_100_101:
          case 0b1000_101_101:
          case 0b1000_110_101:
          case 0b1000_111_101:
            irpOrToMemWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //OR.L Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_110_mmm_rrr
          case 0b1000_000_110:
          case 0b1000_001_110:
          case 0b1000_010_110:
          case 0b1000_011_110:
          case 0b1000_100_110:
          case 0b1000_101_110:
          case 0b1000_110_110:
          case 0b1000_111_110:
            irpOrToMemLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //DIVS.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_111_mmm_rrr
          case 0b1000_000_111:
          case 0b1000_001_111:
          case 0b1000_010_111:
          case 0b1000_011_111:
          case 0b1000_100_111:
          case 0b1000_101_111:
          case 0b1000_110_111:
          case 0b1000_111_111:
            irpDivsWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUB.B <ea>,Dq                                   |-|012346|-|UUUUU|*****|D M+-WXZPI|1001_qqq_000_mmm_rrr
          case 0b1001_000_000:
          case 0b1001_001_000:
          case 0b1001_010_000:
          case 0b1001_011_000:
          case 0b1001_100_000:
          case 0b1001_101_000:
          case 0b1001_110_000:
          case 0b1001_111_000:
            irpSubToRegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUB.W <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_001_mmm_rrr
          case 0b1001_000_001:
          case 0b1001_001_001:
          case 0b1001_010_001:
          case 0b1001_011_001:
          case 0b1001_100_001:
          case 0b1001_101_001:
          case 0b1001_110_001:
          case 0b1001_111_001:
            irpSubToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUB.L <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_010_mmm_rrr
          case 0b1001_000_010:
          case 0b1001_001_010:
          case 0b1001_010_010:
          case 0b1001_011_010:
          case 0b1001_100_010:
          case 0b1001_101_010:
          case 0b1001_110_010:
          case 0b1001_111_010:
            irpSubToRegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBA.W <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr
            //SUB.W <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr [SUBA.W <ea>,Aq]
            //CLR.W Ar                                        |A|012346|-|-----|-----| A        |1001_rrr_011_001_rrr [SUBA.W Ar,Ar]
          case 0b1001_000_011:
          case 0b1001_001_011:
          case 0b1001_010_011:
          case 0b1001_011_011:
          case 0b1001_100_011:
          case 0b1001_101_011:
          case 0b1001_110_011:
          case 0b1001_111_011:
            irpSubaWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBX.B Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_100_000_rrr
            //SUBX.B -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_100_001_rrr
            //SUB.B Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_100_mmm_rrr
          case 0b1001_000_100:
          case 0b1001_001_100:
          case 0b1001_010_100:
          case 0b1001_011_100:
          case 0b1001_100_100:
          case 0b1001_101_100:
          case 0b1001_110_100:
          case 0b1001_111_100:
            irpSubToMemByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBX.W Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_101_000_rrr
            //SUBX.W -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_101_001_rrr
            //SUB.W Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_101_mmm_rrr
          case 0b1001_000_101:
          case 0b1001_001_101:
          case 0b1001_010_101:
          case 0b1001_011_101:
          case 0b1001_100_101:
          case 0b1001_101_101:
          case 0b1001_110_101:
          case 0b1001_111_101:
            irpSubToMemWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBX.L Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_110_000_rrr
            //SUBX.L -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_110_001_rrr
            //SUB.L Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_110_mmm_rrr
          case 0b1001_000_110:
          case 0b1001_001_110:
          case 0b1001_010_110:
          case 0b1001_011_110:
          case 0b1001_100_110:
          case 0b1001_101_110:
          case 0b1001_110_110:
          case 0b1001_111_110:
            irpSubToMemLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SUBA.L <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr
            //SUB.L <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr [SUBA.L <ea>,Aq]
            //CLR.L Ar                                        |A|012346|-|-----|-----| A        |1001_rrr_111_001_rrr [SUBA.L Ar,Ar]
          case 0b1001_000_111:
          case 0b1001_001_111:
          case 0b1001_010_111:
          case 0b1001_011_111:
          case 0b1001_100_111:
          case 0b1001_101_111:
          case 0b1001_110_111:
          case 0b1001_111_111:
            irpSubaLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //SXCALL <name>                                   |A|012346|-|UUUUU|*****|          |1010_0dd_ddd_ddd_ddd [ALINE #<data>]
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
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ALINE #<data>                                   |-|012346|-|UUUUU|*****|          |1010_ddd_ddd_ddd_ddd (line 1010 emulator)
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
          case 0b1010_111_111:
            irpAline ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMP.B <ea>,Dq                                   |-|012346|-|-UUUU|-****|D M+-WXZPI|1011_qqq_000_mmm_rrr
          case 0b1011_000_000:
          case 0b1011_001_000:
          case 0b1011_010_000:
          case 0b1011_011_000:
          case 0b1011_100_000:
          case 0b1011_101_000:
          case 0b1011_110_000:
          case 0b1011_111_000:
            irpCmpByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMP.W <ea>,Dq                                   |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_001_mmm_rrr
          case 0b1011_000_001:
          case 0b1011_001_001:
          case 0b1011_010_001:
          case 0b1011_011_001:
          case 0b1011_100_001:
          case 0b1011_101_001:
          case 0b1011_110_001:
          case 0b1011_111_001:
            irpCmpWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMP.L <ea>,Dq                                   |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_010_mmm_rrr
          case 0b1011_000_010:
          case 0b1011_001_010:
          case 0b1011_010_010:
          case 0b1011_011_010:
          case 0b1011_100_010:
          case 0b1011_101_010:
          case 0b1011_110_010:
          case 0b1011_111_010:
            irpCmpLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMPA.W <ea>,Aq                                  |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr
            //CMP.W <ea>,Aq                                   |A|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr [CMPA.W <ea>,Aq]
          case 0b1011_000_011:
          case 0b1011_001_011:
          case 0b1011_010_011:
          case 0b1011_011_011:
          case 0b1011_100_011:
          case 0b1011_101_011:
          case 0b1011_110_011:
          case 0b1011_111_011:
            irpCmpaWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EOR.B Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_100_mmm_rrr
            //CMPM.B (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_100_001_rrr
          case 0b1011_000_100:
          case 0b1011_001_100:
          case 0b1011_010_100:
          case 0b1011_011_100:
          case 0b1011_100_100:
          case 0b1011_101_100:
          case 0b1011_110_100:
          case 0b1011_111_100:
            irpEorByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EOR.W Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_101_mmm_rrr
            //CMPM.W (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_101_001_rrr
          case 0b1011_000_101:
          case 0b1011_001_101:
          case 0b1011_010_101:
          case 0b1011_011_101:
          case 0b1011_100_101:
          case 0b1011_101_101:
          case 0b1011_110_101:
          case 0b1011_111_101:
            irpEorWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EOR.L Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_110_mmm_rrr
            //CMPM.L (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_110_001_rrr
          case 0b1011_000_110:
          case 0b1011_001_110:
          case 0b1011_010_110:
          case 0b1011_011_110:
          case 0b1011_100_110:
          case 0b1011_101_110:
          case 0b1011_110_110:
          case 0b1011_111_110:
            irpEorLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //CMPA.L <ea>,Aq                                  |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr
            //CMP.L <ea>,Aq                                   |A|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr [CMPA.L <ea>,Aq]
          case 0b1011_000_111:
          case 0b1011_001_111:
          case 0b1011_010_111:
          case 0b1011_011_111:
          case 0b1011_100_111:
          case 0b1011_101_111:
          case 0b1011_110_111:
          case 0b1011_111_111:
            irpCmpaLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //AND.B <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_000_mmm_rrr
          case 0b1100_000_000:
          case 0b1100_001_000:
          case 0b1100_010_000:
          case 0b1100_011_000:
          case 0b1100_100_000:
          case 0b1100_101_000:
          case 0b1100_110_000:
          case 0b1100_111_000:
            irpAndToRegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //AND.W <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_001_mmm_rrr
          case 0b1100_000_001:
          case 0b1100_001_001:
          case 0b1100_010_001:
          case 0b1100_011_001:
          case 0b1100_100_001:
          case 0b1100_101_001:
          case 0b1100_110_001:
          case 0b1100_111_001:
            irpAndToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //AND.L <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_010_mmm_rrr
          case 0b1100_000_010:
          case 0b1100_001_010:
          case 0b1100_010_010:
          case 0b1100_011_010:
          case 0b1100_100_010:
          case 0b1100_101_010:
          case 0b1100_110_010:
          case 0b1100_111_010:
            irpAndToRegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MULU.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_011_mmm_rrr
          case 0b1100_000_011:
          case 0b1100_001_011:
          case 0b1100_010_011:
          case 0b1100_011_011:
          case 0b1100_100_011:
          case 0b1100_101_011:
          case 0b1100_110_011:
          case 0b1100_111_011:
            irpMuluWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ABCD.B Dr,Dq                                    |-|012346|-|UUUUU|*U*U*|          |1100_qqq_100_000_rrr
            //ABCD.B -(Ar),-(Aq)                              |-|012346|-|UUUUU|*U*U*|          |1100_qqq_100_001_rrr
            //AND.B Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_100_mmm_rrr
          case 0b1100_000_100:
          case 0b1100_001_100:
          case 0b1100_010_100:
          case 0b1100_011_100:
          case 0b1100_100_100:
          case 0b1100_101_100:
          case 0b1100_110_100:
          case 0b1100_111_100:
            irpAndToMemByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EXG.L Dq,Dr                                     |-|012346|-|-----|-----|          |1100_qqq_101_000_rrr
            //EXG.L Aq,Ar                                     |-|012346|-|-----|-----|          |1100_qqq_101_001_rrr
            //AND.W Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_101_mmm_rrr
          case 0b1100_000_101:
          case 0b1100_001_101:
          case 0b1100_010_101:
          case 0b1100_011_101:
          case 0b1100_100_101:
          case 0b1100_101_101:
          case 0b1100_110_101:
          case 0b1100_111_101:
            irpAndToMemWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //EXG.L Dq,Ar                                     |-|012346|-|-----|-----|          |1100_qqq_110_001_rrr
            //AND.L Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_110_mmm_rrr
          case 0b1100_000_110:
          case 0b1100_001_110:
          case 0b1100_010_110:
          case 0b1100_011_110:
          case 0b1100_100_110:
          case 0b1100_101_110:
          case 0b1100_110_110:
          case 0b1100_111_110:
            irpAndToMemLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //MULS.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_111_mmm_rrr
          case 0b1100_000_111:
          case 0b1100_001_111:
          case 0b1100_010_111:
          case 0b1100_011_111:
          case 0b1100_100_111:
          case 0b1100_101_111:
          case 0b1100_110_111:
          case 0b1100_111_111:
            irpMulsWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADD.B <ea>,Dq                                   |-|012346|-|UUUUU|*****|D M+-WXZPI|1101_qqq_000_mmm_rrr
          case 0b1101_000_000:
          case 0b1101_001_000:
          case 0b1101_010_000:
          case 0b1101_011_000:
          case 0b1101_100_000:
          case 0b1101_101_000:
          case 0b1101_110_000:
          case 0b1101_111_000:
            irpAddToRegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADD.W <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_001_mmm_rrr
          case 0b1101_000_001:
          case 0b1101_001_001:
          case 0b1101_010_001:
          case 0b1101_011_001:
          case 0b1101_100_001:
          case 0b1101_101_001:
          case 0b1101_110_001:
          case 0b1101_111_001:
            irpAddToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADD.L <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_010_mmm_rrr
          case 0b1101_000_010:
          case 0b1101_001_010:
          case 0b1101_010_010:
          case 0b1101_011_010:
          case 0b1101_100_010:
          case 0b1101_101_010:
          case 0b1101_110_010:
          case 0b1101_111_010:
            irpAddToRegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDA.W <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr
            //ADD.W <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr [ADDA.W <ea>,Aq]
          case 0b1101_000_011:
          case 0b1101_001_011:
          case 0b1101_010_011:
          case 0b1101_011_011:
          case 0b1101_100_011:
          case 0b1101_101_011:
          case 0b1101_110_011:
          case 0b1101_111_011:
            irpAddaWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDX.B Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_100_000_rrr
            //ADDX.B -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_100_001_rrr
            //ADD.B Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_100_mmm_rrr
          case 0b1101_000_100:
          case 0b1101_001_100:
          case 0b1101_010_100:
          case 0b1101_011_100:
          case 0b1101_100_100:
          case 0b1101_101_100:
          case 0b1101_110_100:
          case 0b1101_111_100:
            irpAddToMemByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDX.W Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_101_000_rrr
            //ADDX.W -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_101_001_rrr
            //ADD.W Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_101_mmm_rrr
          case 0b1101_000_101:
          case 0b1101_001_101:
          case 0b1101_010_101:
          case 0b1101_011_101:
          case 0b1101_100_101:
          case 0b1101_101_101:
          case 0b1101_110_101:
          case 0b1101_111_101:
            irpAddToMemWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDX.L Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_110_000_rrr
            //ADDX.L -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_110_001_rrr
            //ADD.L Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_110_mmm_rrr
          case 0b1101_000_110:
          case 0b1101_001_110:
          case 0b1101_010_110:
          case 0b1101_011_110:
          case 0b1101_100_110:
          case 0b1101_101_110:
          case 0b1101_110_110:
          case 0b1101_111_110:
            irpAddToMemLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ADDA.L <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr
            //ADD.L <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr [ADDA.L <ea>,Aq]
          case 0b1101_000_111:
          case 0b1101_001_111:
          case 0b1101_010_111:
          case 0b1101_011_111:
          case 0b1101_100_111:
          case 0b1101_101_111:
          case 0b1101_110_111:
          case 0b1101_111_111:
            irpAddaLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASR.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_000_000_rrr
            //LSR.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_000_001_rrr
            //ROXR.B #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_000_010_rrr
            //ROR.B #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_000_011_rrr
            //ASR.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_000_100_rrr
            //LSR.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_000_101_rrr
            //ROXR.B Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_000_110_rrr
            //ROR.B Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_000_111_rrr
            //ASR.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_000_000_rrr [ASR.B #1,Dr]
            //LSR.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_000_001_rrr [LSR.B #1,Dr]
            //ROXR.B Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_000_010_rrr [ROXR.B #1,Dr]
            //ROR.B Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_000_011_rrr [ROR.B #1,Dr]
          case 0b1110_000_000:
          case 0b1110_001_000:
          case 0b1110_010_000:
          case 0b1110_011_000:
          case 0b1110_100_000:
          case 0b1110_101_000:
          case 0b1110_110_000:
          case 0b1110_111_000:
            irpXxrToRegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASR.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_001_000_rrr
            //LSR.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_001_001_rrr
            //ROXR.W #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_001_010_rrr
            //ROR.W #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_001_011_rrr
            //ASR.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_001_100_rrr
            //LSR.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_001_101_rrr
            //ROXR.W Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_001_110_rrr
            //ROR.W Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_001_111_rrr
            //ASR.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_001_000_rrr [ASR.W #1,Dr]
            //LSR.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_001_001_rrr [LSR.W #1,Dr]
            //ROXR.W Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_001_010_rrr [ROXR.W #1,Dr]
            //ROR.W Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_001_011_rrr [ROR.W #1,Dr]
          case 0b1110_000_001:
          case 0b1110_001_001:
          case 0b1110_010_001:
          case 0b1110_011_001:
          case 0b1110_100_001:
          case 0b1110_101_001:
          case 0b1110_110_001:
          case 0b1110_111_001:
            irpXxrToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASR.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_010_000_rrr
            //LSR.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_010_001_rrr
            //ROXR.L #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_010_010_rrr
            //ROR.L #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_010_011_rrr
            //ASR.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_010_100_rrr
            //LSR.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_010_101_rrr
            //ROXR.L Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_010_110_rrr
            //ROR.L Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_010_111_rrr
            //ASR.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_010_000_rrr [ASR.L #1,Dr]
            //LSR.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_010_001_rrr [LSR.L #1,Dr]
            //ROXR.L Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_010_010_rrr [ROXR.L #1,Dr]
            //ROR.L Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_010_011_rrr [ROR.L #1,Dr]
          case 0b1110_000_010:
          case 0b1110_001_010:
          case 0b1110_010_010:
          case 0b1110_011_010:
          case 0b1110_100_010:
          case 0b1110_101_010:
          case 0b1110_110_010:
          case 0b1110_111_010:
            irpXxrToRegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASR.W <ea>                                      |-|012346|-|UUUUU|***0*|  M+-WXZ  |1110_000_011_mmm_rrr
          case 0b1110_000_011:
            irpAsrToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASL.B #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_100_000_rrr
            //LSL.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_100_001_rrr
            //ROXL.B #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_100_010_rrr
            //ROL.B #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_100_011_rrr
            //ASL.B Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_100_100_rrr
            //LSL.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_100_101_rrr
            //ROXL.B Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_100_110_rrr
            //ROL.B Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_100_111_rrr
            //ASL.B Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_100_000_rrr [ASL.B #1,Dr]
            //LSL.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_100_001_rrr [LSL.B #1,Dr]
            //ROXL.B Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_100_010_rrr [ROXL.B #1,Dr]
            //ROL.B Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_100_011_rrr [ROL.B #1,Dr]
          case 0b1110_000_100:
          case 0b1110_001_100:
          case 0b1110_010_100:
          case 0b1110_011_100:
          case 0b1110_100_100:
          case 0b1110_101_100:
          case 0b1110_110_100:
          case 0b1110_111_100:
            irpXxlToRegByte ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASL.W #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_101_000_rrr
            //LSL.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_101_001_rrr
            //ROXL.W #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_101_010_rrr
            //ROL.W #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_101_011_rrr
            //ASL.W Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_101_100_rrr
            //LSL.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_101_101_rrr
            //ROXL.W Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_101_110_rrr
            //ROL.W Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_101_111_rrr
            //ASL.W Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_101_000_rrr [ASL.W #1,Dr]
            //LSL.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_101_001_rrr [LSL.W #1,Dr]
            //ROXL.W Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_101_010_rrr [ROXL.W #1,Dr]
            //ROL.W Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_101_011_rrr [ROL.W #1,Dr]
          case 0b1110_000_101:
          case 0b1110_001_101:
          case 0b1110_010_101:
          case 0b1110_011_101:
          case 0b1110_100_101:
          case 0b1110_101_101:
          case 0b1110_110_101:
          case 0b1110_111_101:
            irpXxlToRegWord ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASL.L #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_110_000_rrr
            //LSL.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_110_001_rrr
            //ROXL.L #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_110_010_rrr
            //ROL.L #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_110_011_rrr
            //ASL.L Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_110_100_rrr
            //LSL.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_110_101_rrr
            //ROXL.L Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_110_110_rrr
            //ROL.L Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_110_111_rrr
            //ASL.L Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_110_000_rrr [ASL.L #1,Dr]
            //LSL.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_110_001_rrr [LSL.L #1,Dr]
            //ROXL.L Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_110_010_rrr [ROXL.L #1,Dr]
            //ROL.L Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_110_011_rrr [ROL.L #1,Dr]
          case 0b1110_000_110:
          case 0b1110_001_110:
          case 0b1110_010_110:
          case 0b1110_011_110:
          case 0b1110_100_110:
          case 0b1110_101_110:
          case 0b1110_110_110:
          case 0b1110_111_110:
            irpXxlToRegLong ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ASL.W <ea>                                      |-|012346|-|UUUUU|*****|  M+-WXZ  |1110_000_111_mmm_rrr
          case 0b1110_000_111:
            irpAslToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //LSR.W <ea>                                      |-|012346|-|UUUUU|*0*0*|  M+-WXZ  |1110_001_011_mmm_rrr
          case 0b1110_001_011:
            irpLsrToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //LSL.W <ea>                                      |-|012346|-|UUUUU|***0*|  M+-WXZ  |1110_001_111_mmm_rrr
          case 0b1110_001_111:
            irpLslToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ROXR.W <ea>                                     |-|012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_011_mmm_rrr
          case 0b1110_010_011:
            irpRoxrToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ROXL.W <ea>                                     |-|012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_111_mmm_rrr
          case 0b1110_010_111:
            irpRoxlToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ROR.W <ea>                                      |-|012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_011_mmm_rrr
          case 0b1110_011_011:
            irpRorToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //ROL.W <ea>                                      |-|012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_111_mmm_rrr
          case 0b1110_011_111:
            irpRolToMem ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //FPACK <data>                                    |A|012346|-|UUUUU|*****|          |1111_111_0dd_ddd_ddd [FLINE #<data>]
          case 0b1111_111_000:
          case 0b1111_111_001:
          case 0b1111_111_010:
          case 0b1111_111_011:
            irpFpack ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //DOS <data>                                      |A|012346|-|UUUUU|UUUUU|          |1111_111_1dd_ddd_ddd [FLINE #<data>]
          case 0b1111_111_100:
          case 0b1111_111_101:
          case 0b1111_111_110:
          case 0b1111_111_111:
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //FLINE #<data>                                   |-|012346|-|UUUUU|UUUUU|          |1111_ddd_ddd_ddd_ddd (line 1111 emulator)
          case 0b1111_000_000:
          case 0b1111_000_001:
          case 0b1111_000_010:
          case 0b1111_000_011:
          case 0b1111_000_100:
          case 0b1111_000_101:
          case 0b1111_000_110:
          case 0b1111_000_111:
          case 0b1111_001_000:
          case 0b1111_001_001:
          case 0b1111_001_010:
          case 0b1111_001_011:
          case 0b1111_001_100:
          case 0b1111_001_101:
          case 0b1111_001_110:
          case 0b1111_001_111:
          case 0b1111_010_000:
          case 0b1111_010_001:
          case 0b1111_010_010:
          case 0b1111_010_011:
          case 0b1111_010_100:
          case 0b1111_010_101:
          case 0b1111_010_110:
          case 0b1111_010_111:
          case 0b1111_011_000:
          case 0b1111_011_001:
          case 0b1111_011_010:
          case 0b1111_011_011:
          case 0b1111_011_100:
          case 0b1111_011_101:
          case 0b1111_011_110:
          case 0b1111_011_111:
          case 0b1111_100_000:
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
          case 0b1111_101_011:
          case 0b1111_101_100:
          case 0b1111_101_101:
          case 0b1111_101_110:
          case 0b1111_101_111:
          case 0b1111_110_000:
          case 0b1111_110_001:
          case 0b1111_110_010:
          case 0b1111_110_011:
          case 0b1111_110_100:
          case 0b1111_110_101:
          case 0b1111_110_110:
          case 0b1111_110_111:
            irpFline ();
            break irpSwitch;

            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
            //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
            //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
            //HFSBOOT                                         |-|012346|-|-----|-----|          |0100_111_000_000_000
            //HFSINST                                         |-|012346|-|-----|-----|          |0100_111_000_000_001
            //HFSSTR                                          |-|012346|-|-----|-----|          |0100_111_000_000_010
            //HFSINT                                          |-|012346|-|-----|-----|          |0100_111_000_000_011
            //EMXNOP                                          |-|012346|-|-----|-----|          |0100_111_000_000_100
          case 0b0100_111_000:
            irpEmx ();
            break;

          default:
            irpIllegal ();

          }  //switch XEiJ.regOC >>> 6

          //トレース例外
          //  命令実行前にsrのTビットがセットされていたとき命令実行後にトレース例外が発生する
          //  トレース例外の発動は命令の機能拡張であり、他の例外処理で命令が中断されたときはトレース例外は発生しない
          //  命令例外はトレース例外の前に、割り込み例外はトレース例外の後に処理される
          //  未実装命令のエミュレーションルーチンはrteの直前にsrのTビットを復元することで未実装命令が1個の命令としてトレースされたように見せる
          //    ;DOSコールの終了
          //    ~008616:
          //            btst.b  #$07,(sp)
          //            bne.s   ~00861E
          //            rte
          //    ~00861E:
          //            ori.w   #$8000,sr
          //            rte
          if (XEiJ.mpuTraceFlag != 0) {  //命令実行前にsrのTビットがセットされていた
            XEiJ.mpuCycleCount += 34;
            irpException (M68kException.M6E_TRACE, XEiJ.regPC, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //pcは次の命令
          }
          //クロックをカウントアップする
          //  オペランドをアクセスした時点ではまだXEiJ.mpuClockTimeが更新されていないのでXEiJ.mpuClockTime<xxxClock
          //  xxxTickを呼び出すときはXEiJ.mpuClockTime>=xxxClock
          XEiJ.mpuClockTime += XEiJ.mpuModifiedUnit * XEiJ.mpuCycleCount;
          //デバイスを呼び出す
          TickerQueue.tkqRun (XEiJ.mpuClockTime);
          //割り込みを受け付ける
          if ((t = XEiJ.mpuIMR & XEiJ.mpuIRR) != 0) {  //マスクされているレベルよりも高くて受け付けていない割り込みがあるとき
            if (XEiJ.MPU_INTERRUPT_SWITCH) {
              switch (t) {
              case 0b00000001:
              case 0b00000011:
              case 0b00000101:
              case 0b00000111:
              case 0b00001001:
              case 0b00001011:
              case 0b00001101:
              case 0b00001111:
              case 0b00010001:
              case 0b00010011:
              case 0b00010101:
              case 0b00010111:
              case 0b00011001:
              case 0b00011011:
              case 0b00011101:
              case 0b00011111:
              case 0b00100001:
              case 0b00100011:
              case 0b00100101:
              case 0b00100111:
              case 0b00101001:
              case 0b00101011:
              case 0b00101101:
              case 0b00101111:
              case 0b00110001:
              case 0b00110011:
              case 0b00110101:
              case 0b00110111:
              case 0b00111001:
              case 0b00111011:
              case 0b00111101:
              case 0b00111111:
              case 0b01000001:
              case 0b01000011:
              case 0b01000101:
              case 0b01000111:
              case 0b01001001:
              case 0b01001011:
              case 0b01001101:
              case 0b01001111:
              case 0b01010001:
              case 0b01010011:
              case 0b01010101:
              case 0b01010111:
              case 0b01011001:
              case 0b01011011:
              case 0b01011101:
              case 0b01011111:
              case 0b01100001:
              case 0b01100011:
              case 0b01100101:
              case 0b01100111:
              case 0b01101001:
              case 0b01101011:
              case 0b01101101:
              case 0b01101111:
              case 0b01110001:
              case 0b01110011:
              case 0b01110101:
              case 0b01110111:
              case 0b01111001:
              case 0b01111011:
              case 0b01111101:
              case 0b01111111:
              case 0b10000001:
              case 0b10000011:
              case 0b10000101:
              case 0b10000111:
              case 0b10001001:
              case 0b10001011:
              case 0b10001101:
              case 0b10001111:
              case 0b10010001:
              case 0b10010011:
              case 0b10010101:
              case 0b10010111:
              case 0b10011001:
              case 0b10011011:
              case 0b10011101:
              case 0b10011111:
              case 0b10100001:
              case 0b10100011:
              case 0b10100101:
              case 0b10100111:
              case 0b10101001:
              case 0b10101011:
              case 0b10101101:
              case 0b10101111:
              case 0b10110001:
              case 0b10110011:
              case 0b10110101:
              case 0b10110111:
              case 0b10111001:
              case 0b10111011:
              case 0b10111101:
              case 0b10111111:
              case 0b11000001:
              case 0b11000011:
              case 0b11000101:
              case 0b11000111:
              case 0b11001001:
              case 0b11001011:
              case 0b11001101:
              case 0b11001111:
              case 0b11010001:
              case 0b11010011:
              case 0b11010101:
              case 0b11010111:
              case 0b11011001:
              case 0b11011011:
              case 0b11011101:
              case 0b11011111:
              case 0b11100001:
              case 0b11100011:
              case 0b11100101:
              case 0b11100111:
              case 0b11101001:
              case 0b11101011:
              case 0b11101101:
              case 0b11101111:
              case 0b11110001:
              case 0b11110011:
              case 0b11110101:
              case 0b11110111:
              case 0b11111001:
              case 0b11111011:
              case 0b11111101:
              case 0b11111111:
                //レベル7
                XEiJ.mpuIRR &= ~XEiJ.MPU_SYS_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = XEiJ.sysAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_SYS_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
                break;
              case 0b00000010:
              case 0b00000110:
              case 0b00001010:
              case 0b00001110:
              case 0b00010010:
              case 0b00010110:
              case 0b00011010:
              case 0b00011110:
              case 0b00100010:
              case 0b00100110:
              case 0b00101010:
              case 0b00101110:
              case 0b00110010:
              case 0b00110110:
              case 0b00111010:
              case 0b00111110:
              case 0b01000010:
              case 0b01000110:
              case 0b01001010:
              case 0b01001110:
              case 0b01010010:
              case 0b01010110:
              case 0b01011010:
              case 0b01011110:
              case 0b01100010:
              case 0b01100110:
              case 0b01101010:
              case 0b01101110:
              case 0b01110010:
              case 0b01110110:
              case 0b01111010:
              case 0b01111110:
              case 0b10000010:
              case 0b10000110:
              case 0b10001010:
              case 0b10001110:
              case 0b10010010:
              case 0b10010110:
              case 0b10011010:
              case 0b10011110:
              case 0b10100010:
              case 0b10100110:
              case 0b10101010:
              case 0b10101110:
              case 0b10110010:
              case 0b10110110:
              case 0b10111010:
              case 0b10111110:
              case 0b11000010:
              case 0b11000110:
              case 0b11001010:
              case 0b11001110:
              case 0b11010010:
              case 0b11010110:
              case 0b11011010:
              case 0b11011110:
              case 0b11100010:
              case 0b11100110:
              case 0b11101010:
              case 0b11101110:
              case 0b11110010:
              case 0b11110110:
              case 0b11111010:
              case 0b11111110:
                //レベル6
                XEiJ.mpuIRR &= ~XEiJ.MPU_MFP_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = MC68901.mfpAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_MFP_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
                break;
              case 0b00000100:
              case 0b00001100:
              case 0b00010100:
              case 0b00011100:
              case 0b00100100:
              case 0b00101100:
              case 0b00110100:
              case 0b00111100:
              case 0b01000100:
              case 0b01001100:
              case 0b01010100:
              case 0b01011100:
              case 0b01100100:
              case 0b01101100:
              case 0b01110100:
              case 0b01111100:
              case 0b10000100:
              case 0b10001100:
              case 0b10010100:
              case 0b10011100:
              case 0b10100100:
              case 0b10101100:
              case 0b10110100:
              case 0b10111100:
              case 0b11000100:
              case 0b11001100:
              case 0b11010100:
              case 0b11011100:
              case 0b11100100:
              case 0b11101100:
              case 0b11110100:
              case 0b11111100:
                //レベル5
                XEiJ.mpuIRR &= ~XEiJ.MPU_SCC_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = Z8530.sccAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_SCC_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
                break;
              case 0b00010000:
              case 0b00110000:
              case 0b01010000:
              case 0b01110000:
              case 0b10010000:
              case 0b10110000:
              case 0b11010000:
              case 0b11110000:
                //レベル3
                XEiJ.mpuIRR &= ~XEiJ.MPU_DMA_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = HD63450.dmaAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_DMA_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
                break;
              case 0b00100000:
              case 0b01100000:
              case 0b10100000:
              case 0b11100000:
                //レベル2
                XEiJ.mpuIRR &= ~XEiJ.MPU_EB2_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = XEiJ.eb2Acknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_EB2_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
                break;
              case 0b01000000:
              case 0b11000000:
                //レベル1
                XEiJ.mpuIRR &= ~XEiJ.MPU_IOI_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = IOInterrupt.ioiAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_IOI_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
                break;
              }
            } else {
              t &= -t;
              //  x&=-xはxの最下位の1のビットだけを残す演算
              //  すなわちマスクされているレベルよりも高くて受け付けていない割り込みの中で最高レベルの割り込みのビットだけが残る
              //  最高レベルの割り込みのビットしか残っていないので、割り込みの有無をレベルの高い順ではなく使用頻度の高い順に調べられる
              //  MFPやDMAの割り込みがかかる度にそれより優先度の高いインタラプトスイッチが押されていないかどうかを確かめる必要がない
              if (t == XEiJ.MPU_MFP_INTERRUPT_MASK) {
                XEiJ.mpuIRR &= ~XEiJ.MPU_MFP_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = MC68901.mfpAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_MFP_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
              } else if (t == XEiJ.MPU_DMA_INTERRUPT_MASK) {
                XEiJ.mpuIRR &= ~XEiJ.MPU_DMA_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = HD63450.dmaAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_DMA_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
              } else if (t == XEiJ.MPU_SCC_INTERRUPT_MASK) {
                XEiJ.mpuIRR &= ~XEiJ.MPU_SCC_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = Z8530.sccAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_SCC_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
              } else if (t == XEiJ.MPU_IOI_INTERRUPT_MASK) {
                XEiJ.mpuIRR &= ~XEiJ.MPU_IOI_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = IOInterrupt.ioiAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_IOI_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
              } else if (t == XEiJ.MPU_EB2_INTERRUPT_MASK) {
                XEiJ.mpuIRR &= ~XEiJ.MPU_EB2_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = XEiJ.eb2Acknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_EB2_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
              } else if (t == XEiJ.MPU_SYS_INTERRUPT_MASK) {
                XEiJ.mpuIRR &= ~XEiJ.MPU_SYS_INTERRUPT_MASK;  //割り込みを受け付ける
                if ((t = XEiJ.sysAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
                  irpInterrupt (t, XEiJ.MPU_SYS_INTERRUPT_LEVEL);  //割り込み処理を開始する
                }
              }
            }
          }  //if t!=0
          if (MC68901.MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuIRR |= XEiJ.mpuDIRR;  //遅延割り込み要求
            XEiJ.mpuDIRR = 0;
          }
        }  //命令ループ
      } catch (M68kException e) {
        if (M68kException.m6eNumber == M68kException.M6E_WAIT_EXCEPTION) {  //待機例外
          if (irpWaitException ()) {
            continue;
          } else {
            break errorLoop;
          }
        }
        if (M68kException.m6eNumber == M68kException.M6E_INSTRUCTION_BREAK_POINT) {  //命令ブレークポイントによる停止
          XEiJ.regPC = XEiJ.regPC0;
          XEiJ.mpuStop1 (null);  //"Instruction Break Point"
          break errorLoop;
        }
        XEiJ.mpuClockTime += XEiJ.mpuModifiedUnit * XEiJ.mpuCycleCount;
        //例外処理
        //  ここで処理するのはベクタ番号が2～31の例外に限る。TRAP #n命令はインライン展開する
        //  使用頻度が高いと思われる例外はインライン展開するのでここには来ない
        //    例外処理をインライン展開する場合はMC68000とMC68030のコードを分けなければならずコードが冗長になる
        //    使用頻度が低いと思われる例外はインライン展開しない
        //  セーブされるpcは以下の例外は命令の先頭、これ以外は次の命令
        //     2  BUS_ERROR
        //     3  ADDRESS_ERROR
        //     4  ILLEGAL_INSTRUCTION
        //     8  PRIVILEGE_VIOLATION
        //    10  LINE_1010_EMULATOR
        //    11  LINE_1111_EMULATOR
        //                                      fedcba9876543210fedcba9876543210
        //if ((1 << M68kException.m6eNumber & 0b00000000000000000000110100011100) != 0) {
        //    0123456789abcdef0123456789abcdef
        if (0b00111000101100000000000000000000 << M68kException.m6eNumber < 0) {
          XEiJ.regPC = XEiJ.regPC0;  //セーブされるpcは命令の先頭
        }
        try {
          int save_sr = XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
          int sp = XEiJ.regRn[15];
          XEiJ.regSRT1 = 0;  //srのTビットを消す
          if (XEiJ.regSRS == 0) {  //ユーザモードのとき
            XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
            XEiJ.mpuUSP = sp;  //USPを保存
            sp = XEiJ.mpuISP;  //SSPを復元
            if (DataBreakPoint.DBP_ON) {
              DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
            } else {
              XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
            }
            if (InstructionBreakPoint.IBP_ON) {
              InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
            }
          }
          int vectorOffset = M68kException.m6eNumber << 2;  //vector offset
          if (M68kException.m6eNumber <= M68kException.M6E_ADDRESS_ERROR) {  //バスエラーまたはアドレスエラー
            //ホストファイルシステムのデバイスコマンドを強制終了させる
            HFS.hfsState = HFS.HFS_STATE_IDLE;
            XEiJ.mpuClockTime += 50 * XEiJ.mpuModifiedUnit;
            //long format stack frame (bus error/address error)
            //    $0000   .w      status register
            //    $0002   .l      program counter
            //    $0006   .w      format and vector offset. $8008=bus error, $800C=address error
            //    $0008   .w      special status word
            //    $000A   .l      fault address
            //    $000E   .w      unused, reserved
            //    $0010   .w      data output buffer
            //    $0012   .w      unused, reserved
            //    $0014   .w      data input buffer
            //    $0016   .w      unused, reserved
            //    $0018   .w      instruction input buffer
            //    $001A   .w[16]  internal information, 16 words
            //    29 words, 58 bytes
            //special status word
            //    bit15   RR      Rerun flag; 0=processor rerun (default), 1=software rerun
            //    bit14   *       These bits are reserved for future use by Motorola and will be zero when written by the MC68010.
            //    bit13   IF      Instruction fetch to the instruction input buffer
            //    bit12   DF      Data fetch to the data input buffer
            //    bit11   RM      Read-modify-write cycle
            //    bit10   HB      High-byte transfer from the data output buffer or to the data input buffer
            //    bit9    BY      Byte-transfer flag; HB selects the high or low byte of the transfer register.
            //                    If BY is clear, the transfer is word.
            //    bit8    RW      Read/write flag; 0=write, 1=read
            //    bit7-3  *
            //    bit2-0  FC2-FC0 The function code used during the faulted access
            XEiJ.regRn[15] = sp -= 58;  //long format
            XEiJ.busWl (sp + 10, M68kException.m6eAddress);  //fault address
            XEiJ.busWw (sp + 8,
                        (M68kException.m6eSize == XEiJ.MPU_SS_BYTE ? 0x0200 : 0x0000) |  //BY
                        (M68kException.m6eDirection == XEiJ.MPU_WR_READ ? 0x0100 : 0x0000)  //RW
                        //!!! 他のフラグをセットしていない
                        );  //special status word
            XEiJ.busWw (sp + 6, 0x8000 | vectorOffset);  //format and vector offset
            XEiJ.busWl (sp + 2, XEiJ.regPC);  //program counter
            XEiJ.busWw (sp, save_sr);  //status register
          } else {
            //short format stack frame
            //    $0000   .w      status register
            //    $0002   .l      program counter
            //    $0006   .w      format and vector offset
            //    4 words, 8 bytes
            XEiJ.regRn[15] = sp -= 8;  //short format
            XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
            XEiJ.busWl (sp + 2, XEiJ.regPC);  //program counter
            XEiJ.busWw (sp, save_sr);  //status register
          }
          irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
          if (XEiJ.dbgStopOnError) {  //エラーで停止する場合
            if (XEiJ.dbgDoStopOnError ()) {
              break errorLoop;
            }
          }
        } catch (M68kException ee) {  //ダブルバスフォルト
          XEiJ.dbgDoubleBusFault ();
          break errorLoop;
        }
      }  //catch M68kException
    }  //例外ループ

    //  通常
    //    pc0  最後に実行した命令
    //    pc  次に実行する命令
    //  バスエラー、アドレスエラー、不当命令、特権違反で停止したとき
    //    pc0  エラーを発生させた命令
    //    pc  例外処理ルーチンの先頭
    //  ダブルバスフォルトで停止したとき
    //    pc0  エラーを発生させた命令
    //    pc  エラーを発生させた命令
    //  命令ブレークポイントで停止したとき
    //    pc0  命令ブレークポイントが設定された、次に実行する命令
    //    pc  命令ブレークポイントが設定された、次に実行する命令
    //  データブレークポイントで停止したとき
    //    pc0  データを書き換えた、最後に実行した命令
    //    pc  次に実行する命令

    //分岐ログに停止レコードを記録する
    if (BranchLog.BLG_ON) {
      BranchLog.blgStop ();
    }

  }  //mpuCore()



  //cont = irpWaitException ()
  //  待機例外をキャッチしたとき
  public static boolean irpWaitException () {
    XEiJ.regPC = XEiJ.regPC0;  //PCを巻き戻す
    XEiJ.regRn[8 + (XEiJ.regOC & 7)] += WaitInstruction.REWIND_AR[XEiJ.regOC >> 3];  //(Ar)+|-(Ar)で変化したArを巻き戻す
    try {
      //トレース例外を処理する
      if (XEiJ.mpuTraceFlag != 0) {  //命令実行前にsrのTビットがセットされていた
        XEiJ.mpuCycleCount += 34;
        irpException (M68kException.M6E_TRACE, XEiJ.regPC, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //pcは次の命令
      }
      //デバイスを呼び出す
      TickerQueue.tkqRun (XEiJ.mpuClockTime);
      //割り込みを受け付ける
      int t;
      if ((t = XEiJ.mpuIMR & XEiJ.mpuIRR) != 0) {  //マスクされているレベルよりも高くて受け付けていない割り込みがあるとき
        t &= -t;
        //  x&=-xはxの最下位の1のビットだけを残す演算
        //  すなわちマスクされているレベルよりも高くて受け付けていない割り込みの中で最高レベルの割り込みのビットだけが残る
        //  最高レベルの割り込みのビットしか残っていないので、割り込みの有無をレベルの高い順ではなく使用頻度の高い順に調べられる
        //  MFPやDMAの割り込みがかかる度にそれより優先度の高いインタラプトスイッチが押されていないかどうかを確かめる必要がない
        if (t == XEiJ.MPU_MFP_INTERRUPT_MASK) {
          XEiJ.mpuIRR &= ~XEiJ.MPU_MFP_INTERRUPT_MASK;  //割り込みを受け付ける
          if ((t = MC68901.mfpAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
            irpInterrupt (t, XEiJ.MPU_MFP_INTERRUPT_LEVEL);  //割り込み処理を開始する
          }
        } else if (t == XEiJ.MPU_DMA_INTERRUPT_MASK) {
          XEiJ.mpuIRR &= ~XEiJ.MPU_DMA_INTERRUPT_MASK;  //割り込みを受け付ける
          if ((t = HD63450.dmaAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
            irpInterrupt (t, XEiJ.MPU_DMA_INTERRUPT_LEVEL);  //割り込み処理を開始する
          }
        } else if (t == XEiJ.MPU_SCC_INTERRUPT_MASK) {
          XEiJ.mpuIRR &= ~XEiJ.MPU_SCC_INTERRUPT_MASK;  //割り込みを受け付ける
          if ((t = Z8530.sccAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
            irpInterrupt (t, XEiJ.MPU_SCC_INTERRUPT_LEVEL);  //割り込み処理を開始する
          }
        } else if (t == XEiJ.MPU_IOI_INTERRUPT_MASK) {
          XEiJ.mpuIRR &= ~XEiJ.MPU_IOI_INTERRUPT_MASK;  //割り込みを受け付ける
          if ((t = IOInterrupt.ioiAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
            irpInterrupt (t, XEiJ.MPU_IOI_INTERRUPT_LEVEL);  //割り込み処理を開始する
          }
        } else if (t == XEiJ.MPU_EB2_INTERRUPT_MASK) {
          XEiJ.mpuIRR &= ~XEiJ.MPU_EB2_INTERRUPT_MASK;  //割り込みを受け付ける
          if ((t = XEiJ.eb2Acknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
            irpInterrupt (t, XEiJ.MPU_EB2_INTERRUPT_LEVEL);  //割り込み処理を開始する
          }
        } else if (t == XEiJ.MPU_SYS_INTERRUPT_MASK) {
          XEiJ.mpuIRR &= ~XEiJ.MPU_SYS_INTERRUPT_MASK;  //割り込みを受け付ける
          if ((t = XEiJ.sysAcknowledge ()) != 0) {  //デバイスにベクタ番号を要求して割り込み処理中の状態になったとき
            irpInterrupt (t, XEiJ.MPU_SYS_INTERRUPT_LEVEL);  //割り込み処理を開始する
          }
        }
      }  //if t!=0
      if (MC68901.MFP_DELAYED_INTERRUPT) {
        XEiJ.mpuIRR |= XEiJ.mpuDIRR;  //遅延割り込み要求
        XEiJ.mpuDIRR = 0;
      }
    } catch (M68kException e) {
      //!!! 待機例外処理中のバスエラーの処理は省略
      XEiJ.dbgDoubleBusFault ();
      return false;
    }  //catch M68kException
    return true;
  }  //irpWaitException



  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ORI.B #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_000_mmm_rrr-{data}
  //OR.B #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_000_mmm_rrr-{data}  [ORI.B #<data>,<ea>]
  //ORI.B #<data>,CCR                               |-|012346|-|*****|*****|          |0000_000_000_111_100-{data}
  public static void irpOriByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      z = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      z = XEiJ.regPC;
      XEiJ.regPC = z + 2;
      z = XEiJ.busRbs (z + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //ORI.B #<data>,Dr
      if (XEiJ.DBG_ORI_BYTE_ZERO_D0) {
        if (z == 0 && ea == 0 && XEiJ.dbgOriByteZeroD0) {  //ORI.B #$00,D0
          XEiJ.mpuCycleCount += 34;
          M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
          throw M68kException.m6eSignal;
        }
      }
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[ea] |= 255 & z;  //0拡張してからOR
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    } else if (ea == XEiJ.EA_IM) {  //ORI.B #<data>,CCR
      XEiJ.mpuCycleCount += 20;
      XEiJ.regCCR |= XEiJ.REG_CCR_MASK & z;
    } else {  //ORI.B #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z |= XEiJ.busRbs (a));
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    }
  }  //irpOriByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ORI.W #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_001_mmm_rrr-{data}
  //OR.W #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_001_mmm_rrr-{data}  [ORI.W #<data>,<ea>]
  //ORI.W #<data>,SR                                |-|012346|P|*****|*****|          |0000_000_001_111_100-{data}
  public static void irpOriWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //ORI.W #<data>,Dr
      int z;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        z = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
      } else {
        z = XEiJ.regPC;
        XEiJ.regPC = z + 2;
        z = XEiJ.busRwse (z);  //pcws
      }
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[ea] |= (char) z;  //0拡張してからOR
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    } else if (ea == XEiJ.EA_IM) {  //ORI.W #<data>,SR
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.mpuCycleCount += 34;
        M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
        throw M68kException.m6eSignal;
      }
      //以下はスーパーバイザモード
      XEiJ.mpuCycleCount += 20;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        irpSetSR (XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR | XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。特権違反チェックが先
      } else {
        int t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        irpSetSR (XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR | XEiJ.busRwse (t));  //pcws。特権違反チェックが先
      }
    } else {  //ORI.W #<data>,<mem>
      int z;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        z = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
      } else {
        z = XEiJ.regPC;
        XEiJ.regPC = z + 2;
        z = XEiJ.busRwse (z);  //pcws
      }
      XEiJ.mpuCycleCount += 12;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z |= XEiJ.busRws (a));
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    }
  }  //irpOriWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ORI.L #<data>,<ea>                              |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_010_mmm_rrr-{data}
  //OR.L #<data>,<ea>                               |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_000_010_mmm_rrr-{data}  [ORI.L #<data>,<ea>]
  public static void irpOriLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 4;
      y = XEiJ.busRlse (y);  //pcls
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //ORI.L #<data>,Dr
      XEiJ.mpuCycleCount += 16;
      z = XEiJ.regRn[ea] |= y;
    } else {  //ORI.L #<data>,<mem>
      XEiJ.mpuCycleCount += 20;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = XEiJ.busRls (a) | y);
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpOriLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BITREV.L Dr                                     |-|------|-|-----|-----|D         |0000_000_011_000_rrr (ISA_C)
  //
  //BITREV.L Dr
  //  Drのビットの並びを逆順にする。CCRは変化しない
  public static void irpCmp2Chk2Byte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //BITREV.L Dr
      XEiJ.mpuCycleCount += 4;
      if (XEiJ.IRP_BITREV_REVERSE) {  //2.83ns  0x0f801f3c
        XEiJ.regRn[ea] = Integer.reverse (XEiJ.regRn[ea]);
      } else if (XEiJ.IRP_BITREV_SHIFT) {  //2.57ns  0x0f801f3c
        int x = XEiJ.regRn[ea];
        x = x << 16 | x >>> 16;
        x = x << 8 & 0xff00ff00 | x >>> 8 & 0x00ff00ff;
        x = x << 4 & 0xf0f0f0f0 | x >>> 4 & 0x0f0f0f0f;
        x = x << 2 & 0xcccccccc | x >>> 2 & 0x33333333;
        XEiJ.regRn[ea] = x << 1 & 0xaaaaaaaa | x >>> 1 & 0x55555555;
      } else if (XEiJ.IRP_BITREV_TABLE) {  //1.57ns  0x0f801f3c
        int x = XEiJ.regRn[ea];
        XEiJ.regRn[ea] = XEiJ.MPU_BITREV_TABLE_0[x & 2047] | XEiJ.MPU_BITREV_TABLE_1[x << 10 >>> 21] | XEiJ.MPU_BITREV_TABLE_2[x >>> 22];
      }
    } else {  //CMP2/CHK2.B <ea>,Rn
      //プロセッサの判別に使われることがあるのでMC68000ではCMP2/CHK2をエラーにしなければならない
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpCmp2Chk2Byte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BTST.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_100_000_rrr
  //MOVEP.W (d16,Ar),Dq                             |-|01234S|-|-----|-----|          |0000_qqq_100_001_rrr-{data}
  //BTST.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZPI|0000_qqq_100_mmm_rrr
  public static void irpBtstReg () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9;  //qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //MOVEP.W (d16,Ar),Dq
      XEiJ.mpuCycleCount += 16;
      int a;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        a = XEiJ.regRn[ea] + XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws。このr[ea]はアドレスレジスタ
      } else {
        a = XEiJ.regPC;
        XEiJ.regPC = a + 2;
        a = XEiJ.regRn[ea] + XEiJ.busRwse (a);  //pcws。このr[ea]はアドレスレジスタ
      }
      XEiJ.regRn[qqq] = ~0xffff & XEiJ.regRn[qqq] | XEiJ.busRbz (a) << 8 | XEiJ.busRbz (a + 2);  //Javaは評価順序が保証されている
    } else {  //BTST.L Dq,Dr/<ea>
      int y = XEiJ.regRn[qqq];
      if (ea < XEiJ.EA_AR) {  //BTST.L Dq,Dr
        XEiJ.mpuCycleCount += 6;
        XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (~XEiJ.regRn[ea] >>> y & 1) << 2;  //ccr_btst。intのシフトは5bitでマスクされるので&31を省略
      } else {  //BTST.B Dq,<ea>
        XEiJ.mpuCycleCount += 4;
        XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (~XEiJ.busRbs (efaAnyByte (ea)) >>> (y & 7) & 1) << 2;  //ccr_btst
      }
    }
  }  //irpBtstReg

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCHG.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_101_000_rrr
  //MOVEP.L (d16,Ar),Dq                             |-|01234S|-|-----|-----|          |0000_qqq_101_001_rrr-{data}
  //BCHG.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_101_mmm_rrr
  public static void irpBchgReg () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9;  //qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //MOVEP.L (d16,Ar),Dq
      XEiJ.mpuCycleCount += 24;
      int a;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        a = XEiJ.regRn[ea] + XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws。このr[ea]はアドレスレジスタ
      } else {
        a = XEiJ.regPC;
        XEiJ.regPC = a + 2;
        a = XEiJ.regRn[ea] + XEiJ.busRwse (a);  //pcws。このr[ea]はアドレスレジスタ
      }
      XEiJ.regRn[qqq] = XEiJ.busRbs (a) << 24 | XEiJ.busRbz (a + 2) << 16 | XEiJ.busRbz (a + 4) << 8 | XEiJ.busRbz (a + 6);  //Javaは評価順序が保証されている
    } else {  //BCHG.L Dq,Dr/<ea>
      int x;
      int y = XEiJ.regRn[qqq];
      if (ea < XEiJ.EA_AR) {  //BCHG.L Dq,Dr
        XEiJ.regRn[ea] = (x = XEiJ.regRn[ea]) ^ (y = 1 << y);  //intのシフトは5bitでマスクされるので1<<(y&0x1f)の&0x1fを省略
        XEiJ.mpuCycleCount += (char) y != 0 ? 6 : 8;  //(0xffff&y)!=0
      } else {  //BCHG.B Dq,<ea>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltByte (ea);
        XEiJ.busWb (a, (x = XEiJ.busRbs (a)) ^ (y = 1 << (y & 7)));
      }
      XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (x & y) - 1 >>> 31 << 2;  //ccr_btst
    }
  }  //irpBchgReg

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCLR.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_110_000_rrr
  //MOVEP.W Dq,(d16,Ar)                             |-|01234S|-|-----|-----|          |0000_qqq_110_001_rrr-{data}
  //BCLR.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_110_mmm_rrr
  public static void irpBclrReg () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y = XEiJ.regRn[XEiJ.regOC >> 9];  //qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //MOVEP.W Dq,(d16,Ar)
      XEiJ.mpuCycleCount += 16;
      int a;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        a = XEiJ.regRn[ea] + XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws。このr[ea]はアドレスレジスタ
      } else {
        a = XEiJ.regPC;
        XEiJ.regPC = a + 2;
        a = XEiJ.regRn[ea] + XEiJ.busRwse (a);  //pcws。このr[ea]はアドレスレジスタ
      }
      XEiJ.busWb (a, y >> 8);
      XEiJ.busWb (a + 2, y);
    } else {  //BCLR.L Dq,Dr/<ea>
      int x;
      if (ea < XEiJ.EA_AR) {  //BCLR.L Dq,Dr
        XEiJ.regRn[ea] = (x = XEiJ.regRn[ea]) & ~(y = 1 << y);  //intのシフトは5bitでマスクされるので1<<(y&0x1f)の&0x1fを省略
        XEiJ.mpuCycleCount += (char) y != 0 ? 8 : 10;  //(0xffff&y)!=0
      } else {  //BCLR.B Dq,<ea>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltByte (ea);
        XEiJ.busWb (a, (x = XEiJ.busRbs (a)) & ~(y = 1 << (y & 7)));
      }
      XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (x & y) - 1 >>> 31 << 2;  //ccr_btst
    }
  }  //irpBclrReg

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BSET.L Dq,Dr                                    |-|012346|-|--U--|--*--|D         |0000_qqq_111_000_rrr
  //MOVEP.L Dq,(d16,Ar)                             |-|01234S|-|-----|-----|          |0000_qqq_111_001_rrr-{data}
  //BSET.B Dq,<ea>                                  |-|012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_111_mmm_rrr
  public static void irpBsetReg () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y = XEiJ.regRn[XEiJ.regOC >> 9];  //qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //MOVEP.L Dq,(d16,Ar)
      XEiJ.mpuCycleCount += 24;
      int a;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        a = XEiJ.regRn[ea] + XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws。このr[ea]はアドレスレジスタ
      } else {
        a = XEiJ.regPC;
        XEiJ.regPC = a + 2;
        a = XEiJ.regRn[ea] + XEiJ.busRwse (a);  //pcws。このr[ea]はアドレスレジスタ
      }
      XEiJ.busWb (a, y >> 24);
      XEiJ.busWb (a + 2, y >> 16);
      XEiJ.busWb (a + 4, y >> 8);
      XEiJ.busWb (a + 6, y);
    } else {  //BSET.L Dq,Dr/<ea>
      int x;
      if (ea < XEiJ.EA_AR) {  //BSET.L Dq,Dr
        XEiJ.regRn[ea] = (x = XEiJ.regRn[ea]) | (y = 1 << y);  //intのシフトは5bitでマスクされるので1<<(y&0x1f)の&0x1fを省略
        XEiJ.mpuCycleCount += (char) y != 0 ? 6 : 8;  //(0xffff&y)!=0
      } else {  //BSET.B Dq,<ea>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltByte (ea);
        XEiJ.busWb (a, (x = XEiJ.busRbs (a)) | (y = 1 << (y & 7)));
      }
      XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (x & y) - 1 >>> 31 << 2;  //ccr_btst
    }
  }  //irpBsetReg

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ANDI.B #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_000_mmm_rrr-{data}
  //AND.B #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_000_mmm_rrr-{data}  [ANDI.B #<data>,<ea>]
  //ANDI.B #<data>,CCR                              |-|012346|-|*****|*****|          |0000_001_000_111_100-{data}
  public static void irpAndiByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      z = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      z = XEiJ.regPC;
      XEiJ.regPC = z + 2;
      z = XEiJ.busRbs (z + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //ANDI.B #<data>,Dr
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[ea] &= ~255 | z;  //1拡張してからAND
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    } else if (ea == XEiJ.EA_IM) {  //ANDI.B #<data>,CCR
      XEiJ.mpuCycleCount += 20;
      XEiJ.regCCR &= z;
    } else {  //ANDI.B #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z &= XEiJ.busRbs (a));
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    }
  }  //irpAndiByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ANDI.W #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_001_mmm_rrr-{data}
  //AND.W #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_001_mmm_rrr-{data}  [ANDI.W #<data>,<ea>]
  //ANDI.W #<data>,SR                               |-|012346|P|*****|*****|          |0000_001_001_111_100-{data}
  public static void irpAndiWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //ANDI.W #<data>,Dr
      int z;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        z = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
      } else {
        z = XEiJ.regPC;
        XEiJ.regPC = z + 2;
        z = XEiJ.busRwse (z);  //pcws
      }
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[ea] &= ~65535 | z;  //1拡張してからAND
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    } else if (ea == XEiJ.EA_IM) {  //ANDI.W #<data>,SR
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.mpuCycleCount += 34;
        M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
        throw M68kException.m6eSignal;
      }
      //以下はスーパーバイザモード
      XEiJ.mpuCycleCount += 20;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        irpSetSR ((XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR) & XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。特権違反チェックが先
      } else {
        int t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        irpSetSR ((XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR) & XEiJ.busRwse (t));  //pcws。特権違反チェックが先
      }
    } else {  //ANDI.W #<data>,<mem>
      int z;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        z = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
      } else {
        z = XEiJ.regPC;
        XEiJ.regPC = z + 2;
        z = XEiJ.busRwse (z);  //pcws
      }
      XEiJ.mpuCycleCount += 12;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z &= XEiJ.busRws (a));
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    }
  }  //irpAndiWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ANDI.L #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_010_mmm_rrr-{data}
  //AND.L #<data>,<ea>                              |A|012346|-|-UUUU|-**00|  M+-WXZ  |0000_001_010_mmm_rrr-{data}  [ANDI.L #<data>,<ea>]
  public static void irpAndiLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 4;
      y = XEiJ.busRlse (y);  //pcls
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //ANDI.L #<data>,Dr
      XEiJ.mpuCycleCount += 16;
      z = XEiJ.regRn[ea] &= y;
    } else {  //ANDI.L #<data>,<mem>
      XEiJ.mpuCycleCount += 20;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = XEiJ.busRls (a) & y);
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpAndiLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BYTEREV.L Dr                                    |-|------|-|-----|-----|D         |0000_001_011_000_rrr (ISA_C)
  //
  //BYTEREV.L Dr
  //  Drのバイトの並びを逆順にする。CCRは変化しない
  public static void irpCmp2Chk2Word () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //BYTEREV.L Dr
      XEiJ.mpuCycleCount += 4;
      if (true) {  //0.10ns-0.18ns  0x782750ec
        XEiJ.regRn[ea] = Integer.reverseBytes (XEiJ.regRn[ea]);
      } else {  //1.06ns  0x782750ec
        int x = XEiJ.regRn[ea];
        XEiJ.regRn[ea] = x << 24 | x << 8 & 0x00ff0000 | x >>> 8 & 0x0000ff00 | x >>> 24;
      }
    } else {  //CMP2/CHK2.W <ea>,Rn
      //プロセッサの判別に使われることがあるのでMC68000ではCMP2/CHK2をエラーにしなければならない
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpCmp2Chk2Word

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBI.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_000_mmm_rrr-{data}
  //SUB.B #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_000_mmm_rrr-{data}  [SUBI.B #<data>,<ea>]
  public static void irpSubiByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //SUBI.B #<data>,Dr
      XEiJ.mpuCycleCount += 8;
      z = (byte) (XEiJ.regRn[ea] = ~0xff & (x = XEiJ.regRn[ea]) | 0xff & (x = (byte) x) - y);
    } else {  //SUBI.B #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = (byte) ((x = XEiJ.busRbs (a)) - y));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub
  }  //irpSubiByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBI.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_001_mmm_rrr-{data}
  //SUB.W #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_001_mmm_rrr-{data}  [SUBI.W #<data>,<ea>]
  public static void irpSubiWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRwse (y);  //pcws
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //SUBI.W #<data>,Dr
      XEiJ.mpuCycleCount += 8;
      z = (short) (XEiJ.regRn[ea] = ~0xffff & (x = XEiJ.regRn[ea]) | (char) ((x = (short) x) - y));
    } else {  //SUBI.W #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z = (short) ((x = XEiJ.busRws (a)) - y));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub
  }  //irpSubiWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBI.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_010_010_mmm_rrr-{data}
  //SUB.L #<data>,<ea>                              |A|012346|-|UUUUU|*****|  M+-WXZ  |0000_010_010_mmm_rrr-{data}  [SUBI.L #<data>,<ea>]
  public static void irpSubiLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 4;
      y = XEiJ.busRlse (y);  //pcls
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //SUBI.L #<data>,Dr
      XEiJ.mpuCycleCount += 16;
      XEiJ.regRn[ea] = z = (x = XEiJ.regRn[ea]) - y;
    } else {  //SUBI.L #<data>,<mem>
      XEiJ.mpuCycleCount += 20;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) - y);
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub
  }  //irpSubiLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //FF1.L Dr                                        |-|------|-|-UUUU|-**00|D         |0000_010_011_000_rrr (ISA_C)
  //
  //FF1.L Dr
  //  Drの最上位の1のbit31からのオフセットをDrに格納する
  //  Drが0のときは32になる
  public static void irpCmp2Chk2Long () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //FF1.L Dr
      XEiJ.mpuCycleCount += 4;
      int z = XEiJ.regRn[ea];
      if (true) {
        XEiJ.regRn[ea] = Integer.numberOfLeadingZeros (z);
      } else {
        if (z == 0) {
          XEiJ.regRn[ea] = 32;
        } else {
          int k = -(z >>> 16) >> 16 & 16;
          k += -(z >>> k + 8) >> 8 & 8;
          k += -(z >>> k + 4) >> 4 & 4;
          //     bit3  1  1  1  1  1  1  1  1  0  0  0  0  0  0  0  0
          //     bit2  1  1  1  1  0  0  0  0  1  1  1  1  0  0  0  0
          //     bit1  1  1  0  0  1  1  0  0  1  1  0  0  1  1  0  0
          //     bit0  1  0  1  0  1  0  1  0  1  0  1  0  1  0  1  0
          XEiJ.regRn[ea] = ((0b11_11_11_11_11_11_11_11_10_10_10_10_01_01_00_00 >>> (z >>> k << 1)) & 3) + k;  //intのシフトカウントは下位5bitだけが使用される
        }
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    } else {  //CMP2/CHK2.L <ea>,Rn
      //プロセッサの判別に使われることがあるのでMC68000ではCMP2/CHK2をエラーにしなければならない
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpCmp2Chk2Long

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDI.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_000_mmm_rrr-{data}
  public static void irpAddiByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //ADDI.B #<data>,Dr
      XEiJ.mpuCycleCount += 8;
      z = (byte) (XEiJ.regRn[ea] = ~0xff & (x = XEiJ.regRn[ea]) | 0xff & (x = (byte) x) + y);
    } else {  //ADDI.B #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = (byte) ((x = XEiJ.busRbs (a)) + y));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add
  }  //irpAddiByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDI.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_001_mmm_rrr-{data}
  public static void irpAddiWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRwse (y);  //pcws
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //ADDI.W #<data>,Dr
      XEiJ.mpuCycleCount += 8;
      z = (short) (XEiJ.regRn[ea] = ~0xffff & (x = XEiJ.regRn[ea]) | (char) ((x = (short) x) + y));
    } else {  //ADDI.W #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z = (short) ((x = XEiJ.busRws (a)) + y));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add
  }  //irpAddiWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDI.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0000_011_010_mmm_rrr-{data}
  public static void irpAddiLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 4;
      y = XEiJ.busRlse (y);  //pcls
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //ADDI.L #<data>,Dr
      XEiJ.mpuCycleCount += 16;
      XEiJ.regRn[ea] = z = (x = XEiJ.regRn[ea]) + y;
    } else {  //ADDI.L #<data>,<mem>
      XEiJ.mpuCycleCount += 20;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) + y);
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add
  }  //irpAddiLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BTST.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_000_000_rrr-{data}
  //BTST.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZP |0000_100_000_mmm_rrr-{data}
  public static void irpBtstImm () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //BTST.L #<data>,Dr
      XEiJ.mpuCycleCount += 10;
      XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (~XEiJ.regRn[ea] >>> y & 1) << 2;  //ccr_btst。intのシフトは5bitでマスクされるので&31を省略
    } else {  //BTST.B #<data>,<ea>
      XEiJ.mpuCycleCount += 8;
      XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (~XEiJ.busRbs (efaMemByte (ea)) >>> (y & 7) & 1) << 2;  //ccr_btst
    }
  }  //irpBtstImm

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCHG.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_001_000_rrr-{data}
  //BCHG.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_001_mmm_rrr-{data}
  public static void irpBchgImm () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //BCHG.L #<data>,Dr
      XEiJ.regRn[ea] = (x = XEiJ.regRn[ea]) ^ (y = 1 << y);  //intのシフトは5bitでマスクされるので1<<(y&0x1f)の&0x1fを省略
      XEiJ.mpuCycleCount += (char) y != 0 ? 10 : 12;  //(0xffff&y)!=0
    } else {  //BCHG.B #<data>,<ea>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, (x = XEiJ.busRbs (a)) ^ (y = 1 << (y & 7)));
    }
    XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (x & y) - 1 >>> 31 << 2;  //ccr_btst
  }  //irpBchgImm

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCLR.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_010_000_rrr-{data}
  //BCLR.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_010_mmm_rrr-{data}
  public static void irpBclrImm () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //BCLR.L #<data>,Dr
      XEiJ.regRn[ea] = (x = XEiJ.regRn[ea]) & ~(y = 1 << y);  //intのシフトは5bitでマスクされるので1<<(y&0x1f)の&0x1fを省略
      XEiJ.mpuCycleCount += (char) y != 0 ? 12 : 14;  //(0xffff&y)!=0
    } else {  //BCLR.B #<data>,<ea>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, (x = XEiJ.busRbs (a)) & ~(y = 1 << (y & 7)));
    }
    XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (x & y) - 1 >>> 31 << 2;  //ccr_btst
  }  //irpBclrImm

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BSET.L #<data>,Dr                               |-|012346|-|--U--|--*--|D         |0000_100_011_000_rrr-{data}
  //BSET.B #<data>,<ea>                             |-|012346|-|--U--|--*--|  M+-WXZ  |0000_100_011_mmm_rrr-{data}
  public static void irpBsetImm () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //BSET.L #<data>,Dr
      XEiJ.regRn[ea] = (x = XEiJ.regRn[ea]) | (y = 1 << y);  //intのシフトは5bitでマスクされるので1<<(y&0x1f)の&0x1fを省略
      XEiJ.mpuCycleCount += (char) y != 0 ? 10 : 12;  //(0xffff&y)!=0
    } else {  //BSET.B #<data>,<ea>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, (x = XEiJ.busRbs (a)) | (y = 1 << (y & 7)));
    }
    XEiJ.regCCR = XEiJ.regCCR & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_N | XEiJ.REG_CCR_V | XEiJ.REG_CCR_C) | (x & y) - 1 >>> 31 << 2;  //ccr_btst
  }  //irpBsetImm

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EORI.B #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}
  //EOR.B #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}  [EORI.B #<data>,<ea>]
  //EORI.B #<data>,CCR                              |-|012346|-|*****|*****|          |0000_101_000_111_100-{data}
  public static void irpEoriByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      z = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      z = XEiJ.regPC;
      XEiJ.regPC = z + 2;
      z = XEiJ.busRbs (z + 1);  //pcbs
    }
    if (ea < XEiJ.EA_AR) {  //EORI.B #<data>,Dr
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[ea] ^= 255 & z;  //0拡張してからEOR
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    } else if (ea == XEiJ.EA_IM) {  //EORI.B #<data>,CCR
      XEiJ.mpuCycleCount += 20;
      XEiJ.regCCR ^= XEiJ.REG_CCR_MASK & z;
    } else {  //EORI.B #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z ^= XEiJ.busRbs (a));
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    }
  }  //irpEoriByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EORI.W #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}
  //EOR.W #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}  [EORI.W #<data>,<ea>]
  //EORI.W #<data>,SR                               |-|012346|P|*****|*****|          |0000_101_001_111_100-{data}
  public static void irpEoriWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //EORI.W #<data>,Dr
      int z;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        z = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
      } else {
        z = XEiJ.regPC;
        XEiJ.regPC = z + 2;
        z = XEiJ.busRwse (z);  //pcws
      }
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[ea] ^= (char) z;  //0拡張してからEOR
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    } else if (ea == XEiJ.EA_IM) {  //EORI.W #<data>,SR
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.mpuCycleCount += 34;
        M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
        throw M68kException.m6eSignal;
      }
      //以下はスーパーバイザモード
      XEiJ.mpuCycleCount += 20;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        irpSetSR ((XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR) ^ XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。特権違反チェックが先
      } else {
        int t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        irpSetSR ((XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR) ^ XEiJ.busRwse (t));  //pcws。特権違反チェックが先
      }
    } else {  //EORI.W #<data>,<mem>
      int z;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        z = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
      } else {
        z = XEiJ.regPC;
        XEiJ.regPC = z + 2;
        z = XEiJ.busRwse (z);  //pcws
      }
      XEiJ.mpuCycleCount += 12;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z ^= XEiJ.busRws (a));
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    }
  }  //irpEoriWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EORI.L #<data>,<ea>                             |-|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}
  //EOR.L #<data>,<ea>                              |A|012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}  [EORI.L #<data>,<ea>]
  public static void irpEoriLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 4;
      y = XEiJ.busRlse (y);  //pcls
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //EORI.L #<data>,Dr
      XEiJ.mpuCycleCount += 16;
      z = XEiJ.regRn[ea] ^= y;
    } else {  //EORI.L #<data>,<mem>
      XEiJ.mpuCycleCount += 20;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = XEiJ.busRls (a) ^ y);
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpEoriLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMPI.B #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_000_mmm_rrr-{data}
  //CMP.B #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_000_mmm_rrr-{data}  [CMPI.B #<data>,<ea>]
  public static void irpCmpiByte () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRbs ((XEiJ.regPC += 2) - 1);  //pcbs
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRbs (y + 1);  //pcbs
    }
    int z = (byte) ((x = ea < XEiJ.EA_AR ? (byte) XEiJ.regRn[ea] : XEiJ.busRbs (efaMltByte (ea))) - y);  //アドレッシングモードに注意
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpiByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMPI.W #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_001_mmm_rrr-{data}
  //CMP.W #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_001_mmm_rrr-{data}  [CMPI.W #<data>,<ea>]
  public static void irpCmpiWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 2;
      y = XEiJ.busRwse (y);  //pcws
    }
    int z = (short) ((x = ea < XEiJ.EA_AR ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaMltWord (ea))) - y);  //アドレッシングモードに注意
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpiWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMPI.L #<data>,<ea>                             |-|01----|-|-UUUU|-****|D M+-WXZ  |0000_110_010_mmm_rrr-{data}
  //CMP.L #<data>,<ea>                              |A|01----|-|-UUUU|-****|  M+-WXZ  |0000_110_010_mmm_rrr-{data}  [CMPI.L #<data>,<ea>]
  public static void irpCmpiLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      y = XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    } else {
      y = XEiJ.regPC;
      XEiJ.regPC = y + 4;
      y = XEiJ.busRlse (y);  //pcls
    }
    int z;
    if (ea < XEiJ.EA_AR) {  //CMPI.L #<data>,Dr
      XEiJ.mpuCycleCount += 14;
      z = (x = XEiJ.regRn[ea]) - y;
    } else {  //CMPI.L #<data>,<mem>
      XEiJ.mpuCycleCount += 12;
      z = (x = XEiJ.busRls (efaMltLong (ea))) - y;  //アドレッシングモードに注意
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpiLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVES.B <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn000000000000
  //MOVES.B Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn100000000000
  //
  //MOVES.B <ea>,Rn
  //  MOVES.B <ea>,DnはDnの最下位バイトだけ更新する
  //  MOVES.B <ea>,Anはバイトデータをロングに符号拡張してAnの全体を更新する
  //  SFC=1,2,5,6はアドレス変換あり、SFC=0,3,4はアドレス変換なし、
  //  SFC=7はCPU空間なのでコプロセッサが割り当てられている領域以外はバスエラーになる
  //
  //MOVES.B Rn,<ea>
  //  DFC=1,2,5,6はアドレス変換あり、DFC=0,3,4はアドレス変換なし、
  //  DFC=7はCPU空間なのでコプロセッサが割り当てられている領域以外はバスエラーになる
  public static void irpMovesByte () throws M68kException {
    int w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz
    if (w << -11 != 0) {
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 4;
    int a = efaMltByte (XEiJ.regOC & 63);
    int n = w >>> 12;  //n
    if (w << 31 - 11 >= 0) {  //MOVES.B <ea>,Rn。リード
      MemoryMappedDevice[] mm;
      if (XEiJ.mpuSFC == 1 || XEiJ.mpuSFC == 2) {  //ユーザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpUserMap : XEiJ.busUserMap;
      } else if (XEiJ.mpuSFC == 5 || XEiJ.mpuSFC == 6) {  //スーパーバイザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap;
      } else {  //CPU空間などは不可
        M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_BYTE;
        throw M68kException.m6eSignal;
      }
      if (n < 8) {  //MOVES.B <ea>,Dn
        XEiJ.regRn[n] = XEiJ.regRn[n] & ~255 | mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
      } else {  //MOVES.B <ea>,An
        XEiJ.regRn[n] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbs (a);
      }
    } else {  //MOVES.B Rn,<ea>。ライト
      MemoryMappedDevice[] mm;
      if (XEiJ.mpuDFC == 1 || XEiJ.mpuDFC == 2) {  //ユーザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpUserMap : XEiJ.busUserMap;
      } else if (XEiJ.mpuDFC == 5 || XEiJ.mpuDFC == 6) {  //スーパーバイザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap;
      } else {  //CPU空間などは不可
        M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
        M68kException.m6eSize = XEiJ.MPU_SS_BYTE;
        throw M68kException.m6eSignal;
      }
      mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, XEiJ.regRn[n]);
    }
  }  //irpMovesByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVES.W <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn000000000000
  //MOVES.W Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn100000000000
  //
  //MOVES.W <ea>,Rn
  //  MOVES.W <ea>,DnはDnの下位ワードだけ更新する
  //  MOVES.W <ea>,Anはワードデータをロングに符号拡張してAnの全体を更新する
  //  SFC=1,2,5,6はアドレス変換あり、SFC=0,3,4はアドレス変換なし、
  //  SFC=7はCPU空間なのでコプロセッサが割り当てられている領域以外はバスエラーになる
  //
  //MOVES.W Rn,<ea>
  //  DFC=1,2,5,6はアドレス変換あり、DFC=0,3,4はアドレス変換なし、
  //  DFC=7はCPU空間なのでコプロセッサが割り当てられている領域以外はバスエラーになる
  public static void irpMovesWord () throws M68kException {
    int w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz
    if (w << -11 != 0) {
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 4;
    int a = efaMltWord (XEiJ.regOC & 63);
    int n = w >>> 12;  //n
    if (w << 31 - 11 >= 0) {  //MOVES.W <ea>,Rn。リード
      MemoryMappedDevice[] mm;
      if (XEiJ.mpuSFC == 1 || XEiJ.mpuSFC == 2) {  //ユーザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpUserMap : XEiJ.busUserMap;
      } else if (XEiJ.mpuSFC == 5 || XEiJ.mpuSFC == 6) {  //スーパーバイザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap;
      } else {  //CPU空間などは不可
        M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_WORD;
        throw M68kException.m6eSignal;
      }
      int z;
      if ((a & 1) == 0) {  //偶数
        z = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
      } else {  //奇数
        M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_WORD;
        throw M68kException.m6eSignal;
      }
      if (n < 8) {  //MOVES.W <ea>,Dn
        XEiJ.regRn[n] = XEiJ.regRn[n] & ~65535 | z;
      } else {  //MOVES.W <ea>,An
        XEiJ.regRn[n] = (short) z;
      }
    } else {  //MOVES.W Rn,<ea>。ライト
      MemoryMappedDevice[] mm;
      if (XEiJ.mpuDFC == 1 || XEiJ.mpuDFC == 2) {  //ユーザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpUserMap : XEiJ.busUserMap;
      } else if (XEiJ.mpuDFC == 5 || XEiJ.mpuDFC == 6) {  //スーパーバイザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap;
      } else {  //CPU空間などは不可
        M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
        M68kException.m6eSize = XEiJ.MPU_SS_WORD;
        throw M68kException.m6eSignal;
      }
      int z = XEiJ.regRn[n];
      if ((a & 1) == 0) {  //偶数
        mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, z);
      } else {  //奇数
        M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
        M68kException.m6eSize = XEiJ.MPU_SS_WORD;
        throw M68kException.m6eSignal;
      }
    }
  }  //irpMovesWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVES.L <ea>,Rn                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn000000000000
  //MOVES.L Rn,<ea>                                 |-|-12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn100000000000
  //
  //MOVES.L <ea>,Rn
  //  SFC=1,2,5,6はアドレス変換あり、SFC=0,3,4はアドレス変換なし、
  //  SFC=7はCPU空間なのでコプロセッサが割り当てられている領域以外はバスエラーになる
  //
  //MOVES.L Rn,<ea>
  //  DFC=1,2,5,6はアドレス変換あり、DFC=0,3,4はアドレス変換なし、
  //  DFC=7はCPU空間なのでコプロセッサが割り当てられている領域以外はバスエラーになる
  public static void irpMovesLong () throws M68kException {
    int w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz
    if (w << -11 != 0) {
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 4;
    int a = efaMltLong (XEiJ.regOC & 63);
    int n = w >>> 12;  //n
    if (w << 31 - 11 >= 0) {  //MOVES.L <ea>,Rn。リード
      MemoryMappedDevice[] mm;
      if (XEiJ.mpuSFC == 1 || XEiJ.mpuSFC == 2) {  //ユーザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpUserMap : XEiJ.busUserMap;
      } else if (XEiJ.mpuSFC == 5 || XEiJ.mpuSFC == 6) {  //スーパーバイザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap;
      } else {  //CPU空間などは不可
        M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_LONG;
        throw M68kException.m6eSignal;
      }
      int z;
      if ((a & 3) == 0) {  //4の倍数
        z = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRls (a);
      } else if ((a & 1) == 0) {  //4の倍数+2
        z = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16;
        a += 2;
        z |= mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
      } else {  //奇数
        M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_LONG;
        throw M68kException.m6eSignal;
      }
      XEiJ.regRn[n] = z;
    } else {  //MOVES.L Rn,<ea>。ライト
      MemoryMappedDevice[] mm;
      if (XEiJ.mpuDFC == 1 || XEiJ.mpuDFC == 2) {  //ユーザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpUserMap : XEiJ.busUserMap;
      } else if (XEiJ.mpuDFC == 5 || XEiJ.mpuDFC == 6) {  //スーパーバイザモード
        mm = DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap;
      } else {  //CPU空間などは不可
        M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
        M68kException.m6eSize = XEiJ.MPU_SS_LONG;
        throw M68kException.m6eSignal;
      }
      int z = XEiJ.regRn[n];
      if ((a & 3) == 0) {  //4の倍数
        mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWl (a, z);
      } else if ((a & 1) == 0) {  //4の倍数+2
        mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, z >> 16);
        a += 2;
        mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, z);
      } else {  //奇数
        M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
        M68kException.m6eSize = XEiJ.MPU_SS_LONG;
        throw M68kException.m6eSignal;
      }
    }
  }  //irpMovesLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,Dq                                  |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_000_mmm_rrr
  public static void irpMoveToDRByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));
    XEiJ.regRn[qqq] = ~255 & XEiJ.regRn[qqq] | 255 & z;
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToDRByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_010_mmm_rrr
  public static void irpMoveToMMByte () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));  //ここでAqが変化する可能性があることに注意
    XEiJ.busWb (XEiJ.regRn[XEiJ.regOC >> 9], z);  //1qqq=aqq
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToMMByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_011_mmm_rrr
  public static void irpMoveToMPByte () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int aqq = XEiJ.regOC >> 9;  //1qqq=aqq
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));  //ここでAqが変化する可能性があることに注意
    XEiJ.busWb (aqq < 15 ? XEiJ.regRn[aqq]++ : (XEiJ.regRn[15] += 2) - 2, z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToMPByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_100_mmm_rrr
  public static void irpMoveToMNByte () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int aqq = XEiJ.regOC >> 9;  //1qqq=aqq
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));  //ここでAqが変化する可能性があることに注意
    XEiJ.busWb (aqq < 15 ? --XEiJ.regRn[aqq] : (XEiJ.regRn[15] -= 2), z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToMNByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_101_mmm_rrr
  public static void irpMoveToMWByte () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int aqq = XEiJ.regOC >> 9;  //1qqq=aqq
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));  //ここでAqが変化する可能性があることに注意
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWb (XEiJ.regRn[aqq]  //ベースレジスタ
          + XEiJ.busRwse ((XEiJ.regPC += 2) - 2),  //pcws。ワードディスプレースメント
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.busWb (XEiJ.regRn[aqq]  //ベースレジスタ
          + XEiJ.busRwse (t),  //pcws。ワードディスプレースメント
          z);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToMWByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_110_mmm_rrr
  public static void irpMoveToMXByte () throws M68kException {
    XEiJ.mpuCycleCount += 14;
    int ea = XEiJ.regOC & 63;
    int aqq = XEiJ.regOC >> 9;  //1qqq=aqq
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));  //ここでAqが変化する可能性があることに注意
    int w;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
    } else {
      w = XEiJ.regPC;
      XEiJ.regPC = w + 2;
      w = XEiJ.busRwze (w);  //pcwz。拡張ワード
    }
    XEiJ.busWb (XEiJ.regRn[aqq]  //ベースレジスタ
        + (byte) w  //バイトディスプレースメント
        + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
           XEiJ.regRn[w >> 12]),  //ロングインデックス
        z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToMXByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_000_111_mmm_rrr
  public static void irpMoveToZWByte () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWb (XEiJ.busRwse ((XEiJ.regPC += 2) - 2),  //pcws
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.busWb (XEiJ.busRwse (t),  //pcws
          z);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToZWByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.B <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|D M+-WXZPI|0001_001_111_mmm_rrr
  public static void irpMoveToZLByte () throws M68kException {
    XEiJ.mpuCycleCount += 16;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWb (XEiJ.busRlse ((XEiJ.regPC += 4) - 4),  //pcls
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 4;
      XEiJ.busWb (XEiJ.busRlse (t),  //pcls
          z);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpMoveToZLByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,Dq                                  |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_000_mmm_rrr
  public static void irpMoveToDRLong () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z;
    XEiJ.regRn[XEiJ.regOC >> 9 & 7] = z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToDRLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVEA.L <ea>,Aq                                 |-|012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr
  //MOVE.L <ea>,Aq                                  |A|012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr [MOVEA.L <ea>,Aq]
  public static void irpMoveaLong () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    XEiJ.regRn[(XEiJ.regOC >> 9) - (16 - 8)] = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。右辺でAqが変化する可能性があることに注意
  }  //irpMoveaLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_010_mmm_rrr
  public static void irpMoveToMMLong () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.busWl (XEiJ.regRn[(XEiJ.regOC >> 9) - (16 - 8)], z);
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToMMLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_011_mmm_rrr
  public static void irpMoveToMPLong () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.busWl ((XEiJ.regRn[(XEiJ.regOC >> 9) - (16 - 8)] += 4) - 4, z);
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToMPLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_100_mmm_rrr
  public static void irpMoveToMNLong () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.busWl ((XEiJ.regRn[(XEiJ.regOC >> 9) - (16 - 8)] -= 4), z);
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToMNLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_101_mmm_rrr
  public static void irpMoveToMWLong () throws M68kException {
    XEiJ.mpuCycleCount += 16;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWl (XEiJ.regRn[(XEiJ.regOC >> 9) - (16 - 8)]  //ベースレジスタ
          + XEiJ.busRwse ((XEiJ.regPC += 2) - 2),  //pcws。ワードディスプレースメント
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.busWl (XEiJ.regRn[(XEiJ.regOC >> 9) - (16 - 8)]  //ベースレジスタ
          + XEiJ.busRwse (t),  //pcws。ワードディスプレースメント
          z);
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToMWLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_110_mmm_rrr
  public static void irpMoveToMXLong () throws M68kException {
    XEiJ.mpuCycleCount += 18;
    int ea = XEiJ.regOC & 63;
    int aqq = (XEiJ.regOC >> 9) - (16 - 8);
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    int w;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
    } else {
      w = XEiJ.regPC;
      XEiJ.regPC = w + 2;
      w = XEiJ.busRwze (w);  //pcwz。拡張ワード
    }
    XEiJ.busWl (XEiJ.regRn[aqq]  //ベースレジスタ
        + (byte) w  //バイトディスプレースメント
        + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
           XEiJ.regRn[w >> 12]),  //ロングインデックス
        z);
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToMXLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_000_111_mmm_rrr
  public static void irpMoveToZWLong () throws M68kException {
    XEiJ.mpuCycleCount += 16;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWl (XEiJ.busRwse ((XEiJ.regPC += 2) - 2),  //pcws
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.busWl (XEiJ.busRwse (t),  //pcws
          z);
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToZWLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0010_001_111_mmm_rrr
  public static void irpMoveToZLLong () throws M68kException {
    XEiJ.mpuCycleCount += 20;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWl (XEiJ.busRlse ((XEiJ.regPC += 4) - 4),  //pcls
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 4;
      XEiJ.busWl (XEiJ.busRlse (t),  //pcls
          z);
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveToZLLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,Dq                                  |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_000_mmm_rrr
  public static void irpMoveToDRWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    XEiJ.regRn[qqq] = ~65535 & XEiJ.regRn[qqq] | (char) z;
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToDRWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVEA.W <ea>,Aq                                 |-|012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr
  //MOVE.W <ea>,Aq                                  |A|012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr [MOVEA.W <ea>,Aq]
  //
  //MOVEA.W <ea>,Aq
  //  ワードデータをロングに符号拡張してAqの全体を更新する
  public static void irpMoveaWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    XEiJ.regRn[XEiJ.regOC >> 9 & 15] = ea < XEiJ.EA_MM ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //符号拡張して32bit全部書き換える。このr[ea]はデータレジスタまたはアドレスレジスタ。右辺でAqが変化する可能性があることに注意
  }  //irpMoveaWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,(Aq)                                |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_010_mmm_rrr
  public static void irpMoveToMMWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.busWw (XEiJ.regRn[XEiJ.regOC >> 9 & 15], z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToMMWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,(Aq)+                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_011_mmm_rrr
  public static void irpMoveToMPWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.busWw ((XEiJ.regRn[XEiJ.regOC >> 9 & 15] += 2) - 2, z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToMPWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,-(Aq)                               |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_100_mmm_rrr
  public static void irpMoveToMNWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.busWw ((XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= 2), z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToMNWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,(d16,Aq)                            |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_101_mmm_rrr
  public static void irpMoveToMWWord () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int aqq = XEiJ.regOC >> 9 & 15;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWw (XEiJ.regRn[aqq]  //ベースレジスタ
          + XEiJ.busRwse ((XEiJ.regPC += 2) - 2),  //pcws。ワードディスプレースメント
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.busWw (XEiJ.regRn[aqq]  //ベースレジスタ
          + XEiJ.busRwse (t),  //pcws。ワードディスプレースメント
          z);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToMWWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,(d8,Aq,Rn.wl)                       |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_110_mmm_rrr
  public static void irpMoveToMXWord () throws M68kException {
    XEiJ.mpuCycleCount += 14;
    int ea = XEiJ.regOC & 63;
    int aqq = XEiJ.regOC >> 9 & 15;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    int w;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
    } else {
      w = XEiJ.regPC;
      XEiJ.regPC = w + 2;
      w = XEiJ.busRwze (w);  //pcwz。拡張ワード
    }
    XEiJ.busWw (XEiJ.regRn[aqq]  //ベースレジスタ
        + (byte) w  //バイトディスプレースメント
        + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
           XEiJ.regRn[w >> 12]),  //ロングインデックス
        z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToMXWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,(xxx).W                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_000_111_mmm_rrr
  public static void irpMoveToZWWord () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWw (XEiJ.busRwse ((XEiJ.regPC += 2) - 2),  //pcws
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.busWw (XEiJ.busRwse (t),  //pcws
          z);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToZWWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,(xxx).L                             |-|012346|-|-UUUU|-**00|DAM+-WXZPI|0011_001_111_mmm_rrr
  public static void irpMoveToZLWord () throws M68kException {
    XEiJ.mpuCycleCount += 16;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.busWw (XEiJ.busRlse ((XEiJ.regPC += 4) - 4),  //pcls
          z);
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 4;
      XEiJ.busWw (XEiJ.busRlse (t),  //pcls
          z);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpMoveToZLWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NEGX.B <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_000_mmm_rrr
  public static void irpNegxByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    int z;
    if (ea < XEiJ.EA_AR) {  //NEGX.B Dr
      XEiJ.mpuCycleCount += 4;
      z = (byte) (XEiJ.regRn[ea] = ~0xff & (y = XEiJ.regRn[ea]) | 0xff & -(y = (byte) y) - (XEiJ.regCCR >> 4));  //Xの左側はすべて0なのでCCR_X&を省略
    } else {  //NEGX.B <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = (byte) (-(y = XEiJ.busRbs (a)) - (XEiJ.regCCR >> 4)));  //Xの左側はすべて0なのでCCR_X&を省略
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_negx
  }  //irpNegxByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NEGX.W <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_001_mmm_rrr
  public static void irpNegxWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    int z;
    if (ea < XEiJ.EA_AR) {  //NEGX.W Dr
      XEiJ.mpuCycleCount += 4;
      z = (short) (XEiJ.regRn[ea] = ~0xffff & (y = XEiJ.regRn[ea]) | (char) (-(y = (short) y) - (XEiJ.regCCR >> 4)));  //Xの左側はすべて0なのでCCR_X&を省略
    } else {  //NEGX.W <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z = (short) (-(y = XEiJ.busRws (a)) - (XEiJ.regCCR >> 4)));  //Xの左側はすべて0なのでCCR_X&を省略
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_negx
  }  //irpNegxWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NEGX.L <ea>                                     |-|012346|-|*UUUU|*****|D M+-WXZ  |0100_000_010_mmm_rrr
  public static void irpNegxLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    int z;
    if (ea < XEiJ.EA_AR) {  //NEGX.L Dr
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[ea] = z = -(y = XEiJ.regRn[ea]) - (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
    } else {  //NEGX.L <mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = -(y = XEiJ.busRls (a)) - (XEiJ.regCCR >> 4));  //Xの左側はすべて0なのでCCR_X&を省略
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_negx
  }  //irpNegxLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W SR,<ea>                                  |-|-12346|P|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr
  public static void irpMoveFromSR () throws M68kException {
    //MC68010以上では特権命令
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //MOVE.W SR,Dr
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[ea] = ~0xffff & XEiJ.regRn[ea] | XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
    } else {  //MOVE.W SR,<mem>
      //! 軽量化。MOVE from SRによる直後の命令のイミディエイトオペランドの自己書き換えが直後に反映されてしまう
      //  MC68000でFEファンクションコールやSXコールのようなCCRを変化させる例外処理ルーチンの出口を次のように書くと、
      //  自己書き換えが直後に反映されずイミディエイトオペランドの領域がバッファになって前回の結果を返すことになるので期待通りに動作しない
      //              move.w  sr,@f+2
      //      @@:     move.b  #0,(1,sp)
      //              rte
      //  これが期待通りに動作してしまったらMC68000を正しくエミュレートできていないということになる
      //  https://stdkmd.net/bbs/page2.htm#comment134
      XEiJ.mpuCycleCount += 8;
      int a = efaMltWord (ea);
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。MC68000では書き込む前にリードが入るが省略する
      } else {
        XEiJ.busRws (a);
      }
      XEiJ.busWw (a, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);
    }
  }  //irpMoveFromSR

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CHK.W <ea>,Dq                                   |-|012346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_110_mmm_rrr
  public static void irpChkWord () throws M68kException {
    XEiJ.mpuCycleCount += 10;
    int ea = XEiJ.regOC & 63;
    int x = ea < XEiJ.EA_AR ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));
    int y = (short) XEiJ.regRn[XEiJ.regOC >> 9 & 7];
    int z = (short) (x - y);
    XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |
                   (y < 0 ? XEiJ.REG_CCR_N : 0) |
                   (y == 0 ? XEiJ.REG_CCR_Z : 0) |
                   ((x ^ y) & (x ^ z)) >>> 31 << 1 |
                   (x & (y ^ z) ^ (y | z)) >>> 31);
    if (y < 0 || x < y) {
      XEiJ.mpuCycleCount += 38 - 10;
      M68kException.m6eNumber = M68kException.M6E_CHK_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpChkWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //LEA.L <ea>,Aq                                   |-|012346|-|-----|-----|  M  WXZP |0100_qqq_111_mmm_rrr
  public static void irpLea () throws M68kException {
    //XEiJ.mpuCycleCount += 4 - 4;
    XEiJ.regRn[(XEiJ.regOC >> 9) - (32 - 8)] = efaLeaPea (XEiJ.regOC & 63);
  }  //irpLea

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CLR.B <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_000_mmm_rrr (68000 and 68008 read before clear)
  public static void irpClrByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //CLR.B Dr
      XEiJ.mpuCycleCount += 4;
      XEiJ.regRn[ea] &= ~0xff;
    } else {  //CLR.B <mem>
      //MC68010はリードしない
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), 0);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z;  //ccr_clr
  }  //irpClrByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CLR.W <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_001_mmm_rrr (68000 and 68008 read before clear)
  public static void irpClrWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //CLR.W Dr
      XEiJ.mpuCycleCount += 4;
      XEiJ.regRn[ea] &= ~0xffff;
    } else {  //CLR.W <mem>
      //MC68010はリードしない
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWw (efaMltWord (ea), 0);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z;  //ccr_clr
  }  //irpClrWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CLR.L <ea>                                      |-|012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_010_mmm_rrr (68000 and 68008 read before clear)
  public static void irpClrLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //CLR.L Dr
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[ea] = 0;
    } else {  //CLR.L <mem>
      //MC68010はリードしない
      XEiJ.mpuCycleCount += 12;
      XEiJ.busWl (efaMltLong (ea), 0);
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z;  //ccr_clr
  }  //irpClrLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W CCR,<ea>                                 |-|-12346|-|*****|-----|D M+-WXZ  |0100_001_011_mmm_rrr
  public static void irpMoveFromCCR () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //MOVE.W CCR,Dr
      XEiJ.mpuCycleCount += 4;
      XEiJ.regRn[ea] = ~0xffff & XEiJ.regRn[ea] | XEiJ.regCCR;
    } else {  //MOVE.W CCR,<mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWw (efaMltWord (ea), XEiJ.regCCR);
    }
  }  //irpMoveFromCCR

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NEG.B <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_000_mmm_rrr
  public static void irpNegByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    int z;
    if (ea < XEiJ.EA_AR) {  //NEG.B Dr
      XEiJ.mpuCycleCount += 4;
      z = (byte) (XEiJ.regRn[ea] = ~0xff & (y = XEiJ.regRn[ea]) | 0xff & -(y = (byte) y));
    } else {  //NEG.B <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = (byte) -(y = XEiJ.busRbs (a)));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_neg
  }  //irpNegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NEG.W <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_001_mmm_rrr
  public static void irpNegWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    int z;
    if (ea < XEiJ.EA_AR) {  //NEG.W Dr
      XEiJ.mpuCycleCount += 4;
      z = (short) (XEiJ.regRn[ea] = ~0xffff & (y = XEiJ.regRn[ea]) | (char) -(y = (short) y));
    } else {  //NEG.W <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z = (short) -(y = XEiJ.busRws (a)));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_neg
  }  //irpNegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NEG.L <ea>                                      |-|012346|-|UUUUU|*****|D M+-WXZ  |0100_010_010_mmm_rrr
  public static void irpNegLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y;
    int z;
    if (ea < XEiJ.EA_AR) {  //NEG.L Dr
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[ea] = z = -(y = XEiJ.regRn[ea]);
    } else {  //NEG.L <mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = -(y = XEiJ.busRls (a)));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_neg
  }  //irpNegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,CCR                                 |-|012346|-|UUUUU|*****|D M+-WXZPI|0100_010_011_mmm_rrr
  public static void irpMoveToCCR () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    XEiJ.regCCR = XEiJ.REG_CCR_MASK & (ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea)));
  }  //irpMoveToCCR

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NOT.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_000_mmm_rrr
  public static void irpNotByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (ea < XEiJ.EA_AR) {  //NOT.B Dr
      XEiJ.mpuCycleCount += 4;
      z = XEiJ.regRn[ea] ^= 255;  //0拡張してからEOR
    } else {  //NOT.B <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = ~XEiJ.busRbs (a));
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpNotByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NOT.W <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_001_mmm_rrr
  public static void irpNotWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (ea < XEiJ.EA_AR) {  //NOT.W Dr
      XEiJ.mpuCycleCount += 4;
      z = XEiJ.regRn[ea] ^= 65535;  //0拡張してからEOR
    } else {  //NOT.W <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltWord (ea);
      XEiJ.busWw (a, z = ~XEiJ.busRws (a));
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpNotWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NOT.L <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_010_mmm_rrr
  public static void irpNotLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (ea < XEiJ.EA_AR) {  //NOT.L Dr
      XEiJ.mpuCycleCount += 6;
      z = XEiJ.regRn[ea] ^= 0xffffffff;
    } else {  //NOT.L <mem>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltLong (ea);
      XEiJ.busWl (a, z = ~XEiJ.busRls (a));
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpNotLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.W <ea>,SR                                  |-|012346|P|UUUUU|*****|D M+-WXZPI|0100_011_011_mmm_rrr
  public static void irpMoveToSR () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    irpSetSR (ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRwz (efaAnyWord (ea)));  //特権違反チェックが先
  }  //irpMoveToSR

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NBCD.B <ea>                                     |-|012346|-|UUUUU|*U*U*|D M+-WXZ  |0100_100_000_mmm_rrr
  public static void irpNbcd () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //NBCD.B Dr
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[ea] = ~0xff & XEiJ.regRn[ea] | irpSbcd (0, XEiJ.regRn[ea]);
    } else {  //NBCD.B <mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, irpSbcd (0, XEiJ.busRbs (a)));
    }
  }  //irpNbcd

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SWAP.W Dr                                       |-|012346|-|-UUUU|-**00|D         |0100_100_001_000_rrr
  //BKPT #<data>                                    |-|-12346|-|-----|-----|          |0100_100_001_001_ddd
  //PEA.L <ea>                                      |-|012346|-|-----|-----|  M  WXZP |0100_100_001_mmm_rrr
  public static void irpPea () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //SWAP.W Dr
      XEiJ.mpuCycleCount += 4;
      int x;
      int z;
      XEiJ.regRn[ea] = z = (x = XEiJ.regRn[ea]) << 16 | x >>> 16;
      //上位ワードと下位ワードを入れ替えた後のDrをロングでテストする
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    } else {  //PEA.L <ea>
      XEiJ.mpuCycleCount += 12 - 4;
      int a = efaLeaPea (ea);  //BKPT #<data>はここでillegal instructionになる
      XEiJ.busWl (XEiJ.regRn[15] -= 4, a);  //pushl。評価順序に注意。wl(r[15]-=4,eaz_leapea(ea))は不可
    }
  }  //irpPea

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EXT.W Dr                                        |-|012346|-|-UUUU|-**00|D         |0100_100_010_000_rrr
  //MOVEM.W <list>,<ea>                             |-|012346|-|-----|-----|  M -WXZ  |0100_100_010_mmm_rrr-llllllllllllllll
  public static void irpMovemToMemWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //EXT.W Dr
      XEiJ.mpuCycleCount += 4;
      int z;
      XEiJ.regRn[ea] = ~0xffff & (z = XEiJ.regRn[ea]) | (char) (z = (byte) z);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    } else {  //MOVEM.W <list>,<ea>
      int l = XEiJ.busRwze (XEiJ.regPC);  //pcwze。レジスタリスト。ゼロ拡張
      XEiJ.regPC += 2;
      if (ea >> 3 == XEiJ.MMM_MN) {  //-(Ar)
        //MOVEM.wl <list>,-(Ar)で<list>にArが含まれているとき、000/010は命令開始時のArを、020/030/040/060は命令開始時のAr-オペレーションサイズをメモリに書き込む
        //転送するレジスタが0個のときArは変化しない
        int arr = ea - (XEiJ.EA_MN - 8);
        int a = XEiJ.regRn[arr];
        if ((a & 1) != 0 && l != 0) {  //奇数アドレスで1ワード以上転送する
          M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
          M68kException.m6eAddress = a;
          M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
          M68kException.m6eSize = XEiJ.MPU_SS_WORD;
          throw M68kException.m6eSignal;
        }
        int t = a;
        if (XEiJ.IRP_MOVEM_MAINMEMORY &&  //000のときMOVEMでメインメモリを特別扱いにする
            (DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap)[a - 2 >>> XEiJ.BUS_PAGE_BITS] == MemoryMappedDevice.MMD_MMR &&  //メインメモリ
            2 * 16 <= (a & XEiJ.BUS_PAGE_SIZE - 1)) {  //16個転送してもページを跨がない
          a &= XEiJ.BUS_MOTHER_MASK;
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              a -= 2;
              int x = XEiJ.regRn[15];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0002) != 0) {
              a -= 2;
              int x = XEiJ.regRn[14];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0004) != 0) {
              a -= 2;
              int x = XEiJ.regRn[13];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0008) != 0) {
              a -= 2;
              int x = XEiJ.regRn[12];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0010) != 0) {
              a -= 2;
              int x = XEiJ.regRn[11];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0020) != 0) {
              a -= 2;
              int x = XEiJ.regRn[10];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0040) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 9];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              a -= 2;
              int x = XEiJ.regRn[ 8];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0100) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 7];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0200) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 6];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0400) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 5];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x0800) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 4];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x1000) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 3];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x2000) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 2];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((l & 0x4000) != 0) {
              a -= 2;
              int x = XEiJ.regRn[ 1];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              a -= 2;
              int x = XEiJ.regRn[ 0];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 15; i >= 0; i--) {
              if ((l & 0x8000 >>> i) != 0) {
                a -= 2;
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 8);
                MainMemory.mmrM8[a + 1] = (byte)  x;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 15; l != 0; i--, l <<= 1) {
              if (l < 0) {
                a -= 2;
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 8);
                MainMemory.mmrM8[a + 1] = (byte)  x;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 15; l != 0; i--, l >>>= 1) {
              if ((l & 1) != 0) {
                a -= 2;
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 8);
                MainMemory.mmrM8[a + 1] = (byte)  x;
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 15; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              a -= 2;
              int x = XEiJ.regRn[i -= k];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              l = l >>> k & ~1;
            }
          }
          a = t - (short) (t - a);
        } else {  //メインメモリでないかページを跨ぐ可能性がある
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[15]);
            }
            if ((l & 0x0002) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[14]);
            }
            if ((l & 0x0004) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[13]);
            }
            if ((l & 0x0008) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[12]);
            }
            if ((l & 0x0010) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[11]);
            }
            if ((l & 0x0020) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[10]);
            }
            if ((l & 0x0040) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 9]);
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 8]);
            }
            if ((l & 0x0100) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 7]);
            }
            if ((l & 0x0200) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 6]);
            }
            if ((l & 0x0400) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 5]);
            }
            if ((l & 0x0800) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 4]);
            }
            if ((l & 0x1000) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 3]);
            }
            if ((l & 0x2000) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 2]);
            }
            if ((l & 0x4000) != 0) {
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 1]);
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              XEiJ.busWwe (a -= 2, XEiJ.regRn[ 0]);
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 15; i >= 0; i--) {
              if ((l & 0x8000 >>> i) != 0) {
                XEiJ.busWwe (a -= 2, XEiJ.regRn[i]);
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 15; l != 0; i--, l <<= 1) {
              if (l < 0) {
                XEiJ.busWwe (a -= 2, XEiJ.regRn[i]);
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 15; l != 0; i--, l >>>= 1) {
              if ((l & 1) != 0) {
                XEiJ.busWwe (a -= 2, XEiJ.regRn[i]);
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 15; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              XEiJ.busWwe (a -= 2, XEiJ.regRn[i -= k]);
              l = l >>> k & ~1;
            }
          }
        }
        XEiJ.regRn[arr] = a;
        XEiJ.mpuCycleCount += 8 + (t - a << 1);  //2バイト/個→4サイクル/個
      } else {  //-(Ar)以外
        int a = efaCltWord (ea);
        if ((a & 1) != 0 && l != 0) {  //奇数アドレスで1ワード以上転送する
          M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
          M68kException.m6eAddress = a;
          M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
          M68kException.m6eSize = XEiJ.MPU_SS_WORD;
          throw M68kException.m6eSignal;
        }
        int t = a;
        if (XEiJ.IRP_MOVEM_MAINMEMORY &&  //000のときMOVEMでメインメモリを特別扱いにする
            (DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap)[a >>> XEiJ.BUS_PAGE_BITS] == MemoryMappedDevice.MMD_MMR &&  //メインメモリ
            (a & XEiJ.BUS_PAGE_SIZE - 1) <= XEiJ.BUS_PAGE_SIZE - 2 * 16) {  //16個転送してもページを跨がない
          a &= XEiJ.BUS_MOTHER_MASK;
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              int x = XEiJ.regRn[ 0];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0002) != 0) {
              int x = XEiJ.regRn[ 1];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0004) != 0) {
              int x = XEiJ.regRn[ 2];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0008) != 0) {
              int x = XEiJ.regRn[ 3];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0010) != 0) {
              int x = XEiJ.regRn[ 4];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0020) != 0) {
              int x = XEiJ.regRn[ 5];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0040) != 0) {
              int x = XEiJ.regRn[ 6];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              int x = XEiJ.regRn[ 7];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0100) != 0) {
              int x = XEiJ.regRn[ 8];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0200) != 0) {
              int x = XEiJ.regRn[ 9];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0400) != 0) {
              int x = XEiJ.regRn[10];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x0800) != 0) {
              int x = XEiJ.regRn[11];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x1000) != 0) {
              int x = XEiJ.regRn[12];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x2000) != 0) {
              int x = XEiJ.regRn[13];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((l & 0x4000) != 0) {
              int x = XEiJ.regRn[14];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              int x = XEiJ.regRn[15];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 0; i <= 15; i++) {
              if ((l & 0x0001 << i) != 0) {
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 8);
                MainMemory.mmrM8[a + 1] = (byte)  x;
                a += 2;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 0; l != 0; i++, l <<= 1) {
              if (l < 0) {
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 8);
                MainMemory.mmrM8[a + 1] = (byte)  x;
                a += 2;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 0; l != 0; i++, l >>>= 1) {
              if ((l & 1) != 0) {
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 8);
                MainMemory.mmrM8[a + 1] = (byte)  x;
                a += 2;
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 0; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              int x = XEiJ.regRn[i += k];
              MainMemory.mmrM8[a    ] = (byte) (x >> 8);
              MainMemory.mmrM8[a + 1] = (byte)  x;
              a += 2;
              l = l >>> k & ~1;
            }
          }
          a = t + (short) (a - t);
        } else {  //メインメモリでないかページを跨ぐ可能性がある
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 0]);
              a += 2;
            }
            if ((l & 0x0002) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 1]);
              a += 2;
            }
            if ((l & 0x0004) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 2]);
              a += 2;
            }
            if ((l & 0x0008) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 3]);
              a += 2;
            }
            if ((l & 0x0010) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 4]);
              a += 2;
            }
            if ((l & 0x0020) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 5]);
              a += 2;
            }
            if ((l & 0x0040) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 6]);
              a += 2;
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              XEiJ.busWwe (a, XEiJ.regRn[ 7]);
              a += 2;
            }
            if ((l & 0x0100) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 8]);
              a += 2;
            }
            if ((l & 0x0200) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[ 9]);
              a += 2;
            }
            if ((l & 0x0400) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[10]);
              a += 2;
            }
            if ((l & 0x0800) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[11]);
              a += 2;
            }
            if ((l & 0x1000) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[12]);
              a += 2;
            }
            if ((l & 0x2000) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[13]);
              a += 2;
            }
            if ((l & 0x4000) != 0) {
              XEiJ.busWwe (a, XEiJ.regRn[14]);
              a += 2;
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              XEiJ.busWwe (a, XEiJ.regRn[15]);
              a += 2;
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 0; i <= 15; i++) {
              if ((l & 0x0001 << i) != 0) {
                XEiJ.busWwe (a, XEiJ.regRn[i]);
                a += 2;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 0; l != 0; i++, l <<= 1) {
              if (l < 0) {
                XEiJ.busWwe (a, XEiJ.regRn[i]);
                a += 2;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 0; l != 0; i++, l >>>= 1) {
              if ((l & 1) != 0) {
                XEiJ.busWwe (a, XEiJ.regRn[i]);
                a += 2;
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 0; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              XEiJ.busWwe (a, XEiJ.regRn[i += k]);
              a += 2;
              l = l >>> k & ~1;
            }
          }
        }
        XEiJ.mpuCycleCount += 4 + (a - t << 1);  //2バイト/個→4サイクル/個
      }
    }
  }  //irpMovemToMemWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EXT.L Dr                                        |-|012346|-|-UUUU|-**00|D         |0100_100_011_000_rrr
  //MOVEM.L <list>,<ea>                             |-|012346|-|-----|-----|  M -WXZ  |0100_100_011_mmm_rrr-llllllllllllllll
  public static void irpMovemToMemLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //EXT.L Dr
      XEiJ.mpuCycleCount += 4;
      int z;
      XEiJ.regRn[ea] = z = (short) XEiJ.regRn[ea];
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    } else {  //MOVEM.L <list>,<ea>
      int l = XEiJ.busRwze (XEiJ.regPC);  //pcwze。レジスタリスト。ゼロ拡張
      XEiJ.regPC += 2;
      if (ea >> 3 == XEiJ.MMM_MN) {  //-(Ar)
        //MOVEM.wl <list>,-(Ar)で<list>にArが含まれているとき、000/010は命令開始時のArを、020/030/040/060は命令開始時のAr-オペレーションサイズをメモリに書き込む
        //転送するレジスタが0個のときArは変化しない
        int arr = ea - (XEiJ.EA_MN - 8);
        int a = XEiJ.regRn[arr];
        if ((a & 1) != 0 && l != 0) {  //奇数アドレスで1ワード以上転送する
          M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
          M68kException.m6eAddress = a;
          M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
          M68kException.m6eSize = XEiJ.MPU_SS_LONG;
          throw M68kException.m6eSignal;
        }
        int t = a;
        if (XEiJ.IRP_MOVEM_MAINMEMORY &&  //000のときMOVEMでメインメモリを特別扱いにする
            (DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap)[a - 4 >>> XEiJ.BUS_PAGE_BITS] == MemoryMappedDevice.MMD_MMR &&  //メインメモリ
            4 * 16 <= (a & XEiJ.BUS_PAGE_SIZE - 1)) {  //16個転送してもページを跨がない
          a &= XEiJ.BUS_MOTHER_MASK;
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              a -= 4;
              int x = XEiJ.regRn[15];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0002) != 0) {
              a -= 4;
              int x = XEiJ.regRn[14];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0004) != 0) {
              a -= 4;
              int x = XEiJ.regRn[13];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0008) != 0) {
              a -= 4;
              int x = XEiJ.regRn[12];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0010) != 0) {
              a -= 4;
              int x = XEiJ.regRn[11];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0020) != 0) {
              a -= 4;
              int x = XEiJ.regRn[10];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0040) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 9];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              a -= 4;
              int x = XEiJ.regRn[ 8];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0100) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 7];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0200) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 6];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0400) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 5];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x0800) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 4];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x1000) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 3];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x2000) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 2];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((l & 0x4000) != 0) {
              a -= 4;
              int x = XEiJ.regRn[ 1];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              a -= 4;
              int x = XEiJ.regRn[ 0];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 15; i >= 0; i--) {
              if ((l & 0x8000 >>> i) != 0) {
                a -= 4;
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 24);
                MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
                MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
                MainMemory.mmrM8[a + 3] = (byte)  x;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 15; l != 0; i--, l <<= 1) {
              if (l < 0) {
                a -= 4;
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 24);
                MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
                MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
                MainMemory.mmrM8[a + 3] = (byte)  x;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 15; l != 0; i--, l >>>= 1) {
              if ((l & 1) != 0) {
                a -= 4;
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 24);
                MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
                MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
                MainMemory.mmrM8[a + 3] = (byte)  x;
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 15; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              a -= 4;
              int x = XEiJ.regRn[i -= k];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              l = l >>> k & ~1;
            }
          }
          a = t - (short) (t - a);
        } else {  //メインメモリでないかページを跨ぐ可能性がある
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[15]);
            }
            if ((l & 0x0002) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[14]);
            }
            if ((l & 0x0004) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[13]);
            }
            if ((l & 0x0008) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[12]);
            }
            if ((l & 0x0010) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[11]);
            }
            if ((l & 0x0020) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[10]);
            }
            if ((l & 0x0040) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 9]);
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 8]);
            }
            if ((l & 0x0100) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 7]);
            }
            if ((l & 0x0200) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 6]);
            }
            if ((l & 0x0400) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 5]);
            }
            if ((l & 0x0800) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 4]);
            }
            if ((l & 0x1000) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 3]);
            }
            if ((l & 0x2000) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 2]);
            }
            if ((l & 0x4000) != 0) {
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 1]);
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              XEiJ.busWle (a -= 4, XEiJ.regRn[ 0]);
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 15; i >= 0; i--) {
              if ((l & 0x8000 >>> i) != 0) {
                XEiJ.busWle (a -= 4, XEiJ.regRn[i]);
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 15; l != 0; i--, l <<= 1) {
              if (l < 0) {
                XEiJ.busWle (a -= 4, XEiJ.regRn[i]);
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 15; l != 0; i--, l >>>= 1) {
              if ((l & 1) != 0) {
                XEiJ.busWle (a -= 4, XEiJ.regRn[i]);
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 15; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              XEiJ.busWle (a -= 4, XEiJ.regRn[i -= k]);
              l = l >>> k & ~1;
            }
          }
        }
        XEiJ.regRn[arr] = a;
        XEiJ.mpuCycleCount += 8 + (t - a << 1);  //4バイト/個→8サイクル/個
      } else {  //-(Ar)以外
        int a = efaCltLong (ea);
        if ((a & 1) != 0 && l != 0) {  //奇数アドレスで1ワード以上転送する
          M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
          M68kException.m6eAddress = a;
          M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
          M68kException.m6eSize = XEiJ.MPU_SS_LONG;
          throw M68kException.m6eSignal;
        }
        int t = a;
        if (XEiJ.IRP_MOVEM_MAINMEMORY &&  //000のときMOVEMでメインメモリを特別扱いにする
            (DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap)[a >>> XEiJ.BUS_PAGE_BITS] == MemoryMappedDevice.MMD_MMR &&  //メインメモリ
            (a & XEiJ.BUS_PAGE_SIZE - 1) <= XEiJ.BUS_PAGE_SIZE - 4 * 16) {  //16個転送してもページを跨がない
          a &= XEiJ.BUS_MOTHER_MASK;
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              int x = XEiJ.regRn[ 0];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0002) != 0) {
              int x = XEiJ.regRn[ 1];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0004) != 0) {
              int x = XEiJ.regRn[ 2];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0008) != 0) {
              int x = XEiJ.regRn[ 3];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0010) != 0) {
              int x = XEiJ.regRn[ 4];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0020) != 0) {
              int x = XEiJ.regRn[ 5];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0040) != 0) {
              int x = XEiJ.regRn[ 6];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              int x = XEiJ.regRn[ 7];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0100) != 0) {
              int x = XEiJ.regRn[ 8];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0200) != 0) {
              int x = XEiJ.regRn[ 9];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0400) != 0) {
              int x = XEiJ.regRn[10];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x0800) != 0) {
              int x = XEiJ.regRn[11];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x1000) != 0) {
              int x = XEiJ.regRn[12];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x2000) != 0) {
              int x = XEiJ.regRn[13];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((l & 0x4000) != 0) {
              int x = XEiJ.regRn[14];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              int x = XEiJ.regRn[15];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 0; i <= 15; i++) {
              if ((l & 0x0001 << i) != 0) {
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 24);
                MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
                MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
                MainMemory.mmrM8[a + 3] = (byte)  x;
                a += 4;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 0; l != 0; i++, l <<= 1) {
              if (l < 0) {
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 24);
                MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
                MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
                MainMemory.mmrM8[a + 3] = (byte)  x;
                a += 4;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 0; l != 0; i++, l >>>= 1) {
              if ((l & 1) != 0) {
                int x = XEiJ.regRn[i];
                MainMemory.mmrM8[a    ] = (byte) (x >> 24);
                MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
                MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
                MainMemory.mmrM8[a + 3] = (byte)  x;
                a += 4;
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 0; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              int x = XEiJ.regRn[i += k];
              MainMemory.mmrM8[a    ] = (byte) (x >> 24);
              MainMemory.mmrM8[a + 1] = (byte) (x >> 16);
              MainMemory.mmrM8[a + 2] = (byte) (x >>  8);
              MainMemory.mmrM8[a + 3] = (byte)  x;
              a += 4;
              l = l >>> k & ~1;
            }
          }
          a = t + (short) (a - t);
        } else {  //メインメモリでないかページを跨ぐ可能性がある
          if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
            if ((l & 0x0001) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 0]);
              a += 4;
            }
            if ((l & 0x0002) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 1]);
              a += 4;
            }
            if ((l & 0x0004) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 2]);
              a += 4;
            }
            if ((l & 0x0008) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 3]);
              a += 4;
            }
            if ((l & 0x0010) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 4]);
              a += 4;
            }
            if ((l & 0x0020) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 5]);
              a += 4;
            }
            if ((l & 0x0040) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 6]);
              a += 4;
            }
            if ((byte) l < 0) {  //(l & 0x0080) != 0
              XEiJ.busWle (a, XEiJ.regRn[ 7]);
              a += 4;
            }
            if ((l & 0x0100) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 8]);
              a += 4;
            }
            if ((l & 0x0200) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[ 9]);
              a += 4;
            }
            if ((l & 0x0400) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[10]);
              a += 4;
            }
            if ((l & 0x0800) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[11]);
              a += 4;
            }
            if ((l & 0x1000) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[12]);
              a += 4;
            }
            if ((l & 0x2000) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[13]);
              a += 4;
            }
            if ((l & 0x4000) != 0) {
              XEiJ.busWle (a, XEiJ.regRn[14]);
              a += 4;
            }
            if ((short) l < 0) {  //(l & 0x8000) != 0
              XEiJ.busWle (a, XEiJ.regRn[15]);
              a += 4;
            }
          } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
            for (int i = 0; i <= 15; i++) {
              if ((l & 0x0001 << i) != 0) {
                XEiJ.busWle (a, XEiJ.regRn[i]);
                a += 4;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
            l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
            for (int i = 0; l != 0; i++, l <<= 1) {
              if (l < 0) {
                XEiJ.busWle (a, XEiJ.regRn[i]);
                a += 4;
              }
            }
          } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
            for (int i = 0; l != 0; i++, l >>>= 1) {
              if ((l & 1) != 0) {
                XEiJ.busWle (a, XEiJ.regRn[i]);
                a += 4;
              }
            }
          } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
            for (int i = 0; l != 0; ) {
              int k = Integer.numberOfTrailingZeros (l);
              XEiJ.busWle (a, XEiJ.regRn[i += k]);
              a += 4;
              l = l >>> k & ~1;
            }
          }
        }
        XEiJ.mpuCycleCount += 0 + (a - t << 1);  //4バイト/個→8サイクル/個
      }
    }
  }  //irpMovemToMemLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //TST.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_000_mmm_rrr
  public static void irpTstByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & (ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaMltByte (ea)))];  //ccr_tst_byte。アドレッシングモードに注意
  }  //irpTstByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //TST.W <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_001_mmm_rrr
  public static void irpTstWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRws (efaMltWord (ea));  //アドレッシングモードに注意
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpTstWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //TST.L <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_010_mmm_rrr
  public static void irpTstLong () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRls (efaMltLong (ea));  //アドレッシングモードに注意
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpTstLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //TAS.B <ea>                                      |-|012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_011_mmm_rrr
  //ILLEGAL                                         |-|012346|-|-----|-----|          |0100_101_011_111_100
  public static void irpTas () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int z;
    if (ea < XEiJ.EA_AR) {  //TAS.B Dr
      XEiJ.mpuCycleCount += 4;
      XEiJ.regRn[ea] = 0x80 | (z = XEiJ.regRn[ea]);
    } else if (ea == XEiJ.EA_IM) {  //ILLEGAL
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    } else {  //TAS.B <mem>
      XEiJ.mpuCycleCount += 10;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, 0x80 | (z = XEiJ.busRbs (a)));
    }
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
  }  //irpTas

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SATS.L Dr                                       |-|------|-|-UUUU|-**00|D         |0100_110_010_000_rrr (ISA_B)
  //MOVEM.W <ea>,<list>                             |-|012346|-|-----|-----|  M+ WXZP |0100_110_010_mmm_rrr-llllllllllllllll
  //
  //SATS.L Dr
  //  VがセットされていたらDrを符号が逆で絶対値が最大の値にする(直前のDrに対する演算を飽和演算にする)
  public static void irpMovemToRegWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_AR) {  //SATS.L Dr
      XEiJ.mpuCycleCount += 4;
      int z = XEiJ.regRn[ea];
      if (XEiJ.TEST_BIT_1_SHIFT ? XEiJ.regCCR << 31 - 1 < 0 : (XEiJ.regCCR & XEiJ.REG_CCR_V) != 0) {  //Vがセットされているとき
        XEiJ.regRn[ea] = z = z >> 31 ^ 0x80000000;  //符号が逆で絶対値が最大の値にする
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    } else {  //MOVEM.W <ea>,<list>
      int l = XEiJ.busRwze (XEiJ.regPC);  //pcwze。レジスタリスト。ゼロ拡張
      XEiJ.regPC += 2;
      int arr, a;
      if (ea >> 3 == XEiJ.MMM_MP) {  //(Ar)+
        XEiJ.mpuCycleCount += 12;
        arr = ea - (XEiJ.EA_MP - 8);
        a = XEiJ.regRn[arr];
      } else {  //(Ar)+以外
        XEiJ.mpuCycleCount += 8;
        arr = 16;
        a = efaCntWord (ea);
      }
      if ((a & 1) != 0 && l != 0) {  //奇数アドレスで1ワード以上転送する
        M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_WORD;
        throw M68kException.m6eSignal;
      }
      int t = a;
      if (XEiJ.IRP_MOVEM_MAINMEMORY &&  //000のときMOVEMでメインメモリを特別扱いにする
          (DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap)[a >>> XEiJ.BUS_PAGE_BITS] == MemoryMappedDevice.MMD_MMR &&  //メインメモリ
          (a & XEiJ.BUS_PAGE_SIZE - 1) <= XEiJ.BUS_PAGE_SIZE - 2 * 16) {  //16個転送してもページを跨がない
        a &= XEiJ.BUS_MOTHER_MASK;
        if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
          if ((l & 0x0001) != 0) {
            XEiJ.regRn[ 0] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0002) != 0) {
            XEiJ.regRn[ 1] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0004) != 0) {
            XEiJ.regRn[ 2] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0008) != 0) {
            XEiJ.regRn[ 3] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0010) != 0) {
            XEiJ.regRn[ 4] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0020) != 0) {
            XEiJ.regRn[ 5] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0040) != 0) {
            XEiJ.regRn[ 6] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((byte) l < 0) {  //(l & 0x0080) != 0
            XEiJ.regRn[ 7] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0100) != 0) {
            XEiJ.regRn[ 8] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0200) != 0) {
            XEiJ.regRn[ 9] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0400) != 0) {
            XEiJ.regRn[10] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0800) != 0) {
            XEiJ.regRn[11] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x1000) != 0) {
            XEiJ.regRn[12] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x2000) != 0) {
            XEiJ.regRn[13] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x4000) != 0) {
            XEiJ.regRn[14] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((short) l < 0) {  //(l & 0x8000) != 0
            XEiJ.regRn[15] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //符号拡張して32bit全部書き換える
            a += 2;
          }
        } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
          for (int i = 0; i <= 15; i++) {
            if ((l & 0x0001 << i) != 0) {
              XEiJ.regRn[i] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //(データレジスタも)符号拡張して32bit全部書き換える
              a += 2;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
          l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
          for (int i = 0; l != 0; i++, l <<= 1) {
            if (l < 0) {
              XEiJ.regRn[i] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //(データレジスタも)符号拡張して32bit全部書き換える
              a += 2;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
          for (int i = 0; l != 0; i++, l >>>= 1) {
            if ((l & 1) != 0) {
              XEiJ.regRn[i] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //(データレジスタも)符号拡張して32bit全部書き換える
              a += 2;
            }
          }
        } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
          for (int i = 0; l != 0; ) {
            int k = Integer.numberOfTrailingZeros (l);
            XEiJ.regRn[i += k] = MainMemory.mmrM8[a] << 8 | MainMemory.mmrM8[a + 1] & 255;  //(データレジスタも)符号拡張して32bit全部書き換える
            a += 2;
            l = l >>> k & ~1;
          }
        }
        a = t + (short) (a - t);
      } else {  //メインメモリでないかページを跨ぐ可能性がある
        if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
          if ((l & 0x0001) != 0) {
            XEiJ.regRn[ 0] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0002) != 0) {
            XEiJ.regRn[ 1] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0004) != 0) {
            XEiJ.regRn[ 2] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0008) != 0) {
            XEiJ.regRn[ 3] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0010) != 0) {
            XEiJ.regRn[ 4] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0020) != 0) {
            XEiJ.regRn[ 5] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0040) != 0) {
            XEiJ.regRn[ 6] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((byte) l < 0) {  //(l & 0x0080) != 0
            XEiJ.regRn[ 7] = XEiJ.busRwse (a);  //データレジスタも符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0100) != 0) {
            XEiJ.regRn[ 8] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0200) != 0) {
            XEiJ.regRn[ 9] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0400) != 0) {
            XEiJ.regRn[10] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x0800) != 0) {
            XEiJ.regRn[11] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x1000) != 0) {
            XEiJ.regRn[12] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x2000) != 0) {
            XEiJ.regRn[13] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((l & 0x4000) != 0) {
            XEiJ.regRn[14] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
          if ((short) l < 0) {  //(l & 0x8000) != 0
            XEiJ.regRn[15] = XEiJ.busRwse (a);  //符号拡張して32bit全部書き換える
            a += 2;
          }
        } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
          for (int i = 0; i <= 15; i++) {
            if ((l & 0x0001 << i) != 0) {
              XEiJ.regRn[i] = XEiJ.busRwse (a);  //(データレジスタも)符号拡張して32bit全部書き換える
              a += 2;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
          l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
          for (int i = 0; l != 0; i++, l <<= 1) {
            if (l < 0) {
              XEiJ.regRn[i] = XEiJ.busRwse (a);  //(データレジスタも)符号拡張して32bit全部書き換える
              a += 2;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
          for (int i = 0; l != 0; i++, l >>>= 1) {
            if ((l & 1) != 0) {
              XEiJ.regRn[i] = XEiJ.busRwse (a);  //(データレジスタも)符号拡張して32bit全部書き換える
              a += 2;
            }
          }
        } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
          for (int i = 0; l != 0; ) {
            int k = Integer.numberOfTrailingZeros (l);
            XEiJ.regRn[i += k] = XEiJ.busRwse (a);  //(データレジスタも)符号拡張して32bit全部書き換える
            a += 2;
            l = l >>> k & ~1;
          }
        }
      }
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。MC68000のMOVEM.W <ea>,<list>は1ワード余分にリードするが省略する
        //  MC68000のMOVEM.W <ea>,<list>は1ワード余分にリードするため転送する領域の直後にアクセスできない領域があるとバスエラーが発生する
        //  RAMDISK.SYSを高速化しようとしてデータ転送ルーチンの転送命令をすべてMOVEMに変更してしまうと、
        //  12MBフル実装でないX68000の実機で最後のセクタをアクセスしたときだけバスエラーが出て動かなくなるのはこれが原因
      } else {
        XEiJ.busRws (a);
      }
      //MOVEM.W (Ar)+,<list>で<list>にArが含まれているとき、メモリから読み出したデータを捨ててArをインクリメントする
      XEiJ.regRn[arr] = a;
      XEiJ.mpuCycleCount += a - t << 1;  //2バイト/個→4サイクル/個
    }
  }  //irpMovemToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVEM.L <ea>,<list>                             |-|012346|-|-----|-----|  M+ WXZP |0100_110_011_mmm_rrr-llllllllllllllll
  public static void irpMovemToRegLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    {
      int l = XEiJ.busRwze (XEiJ.regPC);  //pcwze。レジスタリスト。ゼロ拡張
      XEiJ.regPC += 2;
      int arr, a;
      if (ea >> 3 == XEiJ.MMM_MP) {  //(Ar)+
        XEiJ.mpuCycleCount += 8;
        arr = ea - (XEiJ.EA_MP - 8);
        a = XEiJ.regRn[arr];
      } else {  //(Ar)+以外
        XEiJ.mpuCycleCount += 4;
        arr = 16;
        a = efaCntLong (ea);
      }
      if ((a & 1) != 0 && l != 0) {  //奇数アドレスで1ワード以上転送する
        M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
        M68kException.m6eAddress = a;
        M68kException.m6eDirection = XEiJ.MPU_WR_READ;
        M68kException.m6eSize = XEiJ.MPU_SS_LONG;
        throw M68kException.m6eSignal;
      }
      int t = a;
      if (XEiJ.IRP_MOVEM_MAINMEMORY &&  //000のときMOVEMでメインメモリを特別扱いにする
          (DataBreakPoint.DBP_ON ? DataBreakPoint.dbpSuperMap : XEiJ.busSuperMap)[a >>> XEiJ.BUS_PAGE_BITS] == MemoryMappedDevice.MMD_MMR &&  //メインメモリ
          (a & XEiJ.BUS_PAGE_SIZE - 1) <= XEiJ.BUS_PAGE_SIZE - 4 * 16) {  //16個転送してもページを跨がない
        a &= XEiJ.BUS_MOTHER_MASK;
        if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
          if ((l & 0x0001) != 0) {
            XEiJ.regRn[ 0] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0002) != 0) {
            XEiJ.regRn[ 1] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0004) != 0) {
            XEiJ.regRn[ 2] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0008) != 0) {
            XEiJ.regRn[ 3] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0010) != 0) {
            XEiJ.regRn[ 4] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0020) != 0) {
            XEiJ.regRn[ 5] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0040) != 0) {
            XEiJ.regRn[ 6] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((byte) l < 0) {  //(l & 0x0080) != 0
            XEiJ.regRn[ 7] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0100) != 0) {
            XEiJ.regRn[ 8] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0200) != 0) {
            XEiJ.regRn[ 9] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0400) != 0) {
            XEiJ.regRn[10] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x0800) != 0) {
            XEiJ.regRn[11] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x1000) != 0) {
            XEiJ.regRn[12] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x2000) != 0) {
            XEiJ.regRn[13] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((l & 0x4000) != 0) {
            XEiJ.regRn[14] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
          if ((short) l < 0) {  //(l & 0x8000) != 0
            XEiJ.regRn[15] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
          }
        } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
          for (int i = 0; i <= 15; i++) {
            if ((l & 0x0001 << i) != 0) {
              XEiJ.regRn[i] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
              a += 4;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
          l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
          for (int i = 0; l != 0; i++, l <<= 1) {
            if (l < 0) {
              XEiJ.regRn[i] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
              a += 4;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
          for (int i = 0; l != 0; i++, l >>>= 1) {
            if ((l & 1) != 0) {
              XEiJ.regRn[i] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
              a += 4;
            }
          }
        } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
          for (int i = 0; l != 0; ) {
            int k = Integer.numberOfTrailingZeros (l);
            XEiJ.regRn[i += k] = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 255) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | MainMemory.mmrM8[a + 3] & 255);
            a += 4;
            l = l >>> k & ~1;
          }
        }
        a = t + (short) (a - t);
      } else {  //メインメモリでないかページを跨ぐ可能性がある
        if (XEiJ.IRP_MOVEM_EXPAND) {  //16回展開する
          if ((l & 0x0001) != 0) {
            XEiJ.regRn[ 0] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0002) != 0) {
            XEiJ.regRn[ 1] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0004) != 0) {
            XEiJ.regRn[ 2] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0008) != 0) {
            XEiJ.regRn[ 3] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0010) != 0) {
            XEiJ.regRn[ 4] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0020) != 0) {
            XEiJ.regRn[ 5] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0040) != 0) {
            XEiJ.regRn[ 6] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((byte) l < 0) {  //(l & 0x0080) != 0
            XEiJ.regRn[ 7] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0100) != 0) {
            XEiJ.regRn[ 8] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0200) != 0) {
            XEiJ.regRn[ 9] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0400) != 0) {
            XEiJ.regRn[10] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x0800) != 0) {
            XEiJ.regRn[11] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x1000) != 0) {
            XEiJ.regRn[12] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x2000) != 0) {
            XEiJ.regRn[13] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((l & 0x4000) != 0) {
            XEiJ.regRn[14] = XEiJ.busRlse (a);
            a += 4;
          }
          if ((short) l < 0) {  //(l & 0x8000) != 0
            XEiJ.regRn[15] = XEiJ.busRlse (a);
            a += 4;
          }
        } else if (XEiJ.IRP_MOVEM_LOOP) {  //16回ループする。コンパイラが展開する
          for (int i = 0; i <= 15; i++) {
            if ((l & 0x0001 << i) != 0) {
              XEiJ.regRn[i] = XEiJ.busRlse (a);
              a += 4;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_LEFT) {  //0になるまで左にシフトする
          l = XEiJ.MPU_BITREV_TABLE_0[l & 2047] | XEiJ.MPU_BITREV_TABLE_1[l << 10 >>> 21];  //Integer.reverse(l)
          for (int i = 0; l != 0; i++, l <<= 1) {
            if (l < 0) {
              XEiJ.regRn[i] = XEiJ.busRlse (a);
              a += 4;
            }
          }
        } else if (XEiJ.IRP_MOVEM_SHIFT_RIGHT) {  //0になるまで右にシフトする
          for (int i = 0; l != 0; i++, l >>>= 1) {
            if ((l & 1) != 0) {
              XEiJ.regRn[i] = XEiJ.busRlse (a);
              a += 4;
            }
          }
        } else if (XEiJ.IRP_MOVEM_ZEROS) {  //Integer.numberOfTrailingZerosを使う
          for (int i = 0; l != 0; ) {
            int k = Integer.numberOfTrailingZeros (l);
            XEiJ.regRn[i += k] = XEiJ.busRlse (a);
            a += 4;
            l = l >>> k & ~1;
          }
        }
      }
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。MC68000のMOVEM.L <ea>,<list>は1ワード余分にリードするが省略する
        //  MC68000のMOVEM.L <ea>,<list>は1ワード余分にリードするため転送する領域の直後にアクセスできない領域があるとバスエラーが発生する
        //  RAMDISK.SYSを高速化しようとしてデータ転送ルーチンの転送命令をすべてMOVEMに変更してしまうと、
        //  12MBフル実装でないX68000の実機で最後のセクタをアクセスしたときだけバスエラーが出て動かなくなるのはこれが原因
      } else {
        XEiJ.busRws (a);
      }
      //MOVEM.L (Ar)+,<list>で<list>にArが含まれているとき、メモリから読み出したデータを捨ててArをインクリメントする
      XEiJ.regRn[arr] = a;  //XEiJ.regRn[arr]は破壊されているのでXEiJ.regRn[arr]+=a-=tは不可
      XEiJ.mpuCycleCount += a - t << 1;  //4バイト/個→8サイクル/個
    }
  }  //irpMovemToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //TRAP #<vector>                                  |-|012346|-|-----|-----|          |0100_111_001_00v_vvv
  public static void irpTrap () throws M68kException {
    XEiJ.mpuCycleCount += 34;
    if (XEiJ.MPU_INLINE_EXCEPTION) {
      int save_sr = XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
      int sp = XEiJ.regRn[15];
      XEiJ.regSRT1 = XEiJ.mpuTraceFlag = 0;  //srのTビットを消す
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
        XEiJ.mpuUSP = sp;  //USPを保存
        sp = XEiJ.mpuISP;  //SSPを復元
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
        } else {
          XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
        }
        if (InstructionBreakPoint.IBP_ON) {
          InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
        }
      }
      int vectorOffset = XEiJ.regOC - (0x4e40 - M68kException.M6E_TRAP_0_INSTRUCTION_VECTOR) << 2;  //vector offset
      XEiJ.regRn[15] = sp -= 8;  //short format
      XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
      XEiJ.busWl (sp + 2, XEiJ.regPC);  //program counter
      XEiJ.busWw (sp, save_sr);  //status register
      irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
    } else {
      irpException (XEiJ.regOC - (0x4e40 - M68kException.M6E_TRAP_0_INSTRUCTION_VECTOR), XEiJ.regPC, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //pcは次の命令
    }
  }  //irpTrap
  public static void irpTrap15 () throws M68kException {
    if ((XEiJ.regRn[0] & 255) == 0x8e) {  //IOCS _BOOTINF
      MainMemory.mmrCheckHuman ();
    }
    XEiJ.mpuCycleCount += 34;
    if (XEiJ.MPU_INLINE_EXCEPTION) {
      int save_sr = XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
      int sp = XEiJ.regRn[15];
      XEiJ.regSRT1 = XEiJ.mpuTraceFlag = 0;  //srのTビットを消す
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
        XEiJ.mpuUSP = sp;  //USPを保存
        sp = XEiJ.mpuISP;  //SSPを復元
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
        } else {
          XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
        }
        if (InstructionBreakPoint.IBP_ON) {
          InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
        }
      }
      int vectorOffset = M68kException.M6E_TRAP_15_INSTRUCTION_VECTOR << 2;  //vector offset
      XEiJ.regRn[15] = sp -= 8;  //short format
      XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
      XEiJ.busWl (sp + 2, XEiJ.regPC);  //program counter
      XEiJ.busWw (sp, save_sr);  //status register
      irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
    } else {
      irpException (M68kException.M6E_TRAP_15_INSTRUCTION_VECTOR, XEiJ.regPC, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //pcは次の命令
    }
  }  //irpTrap15

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //LINK.W Ar,#<data>                               |-|012346|-|-----|-----|          |0100_111_001_010_rrr-{data}
  //
  //LINK.W Ar,#<data>
  //  PEA.L (Ar);MOVEA.L A7,Ar;ADDA.W #<data>,A7と同じ
  //  LINK.W A7,#<data>はA7をデクリメントする前の値がプッシュされ、A7に#<data>が加算される
  public static void irpLinkWord () throws M68kException {
    XEiJ.mpuCycleCount += 16;
    int arr = XEiJ.regOC - (0b0100_111_001_010_000 - 8);
    //評価順序に注意
    //  wl(r[15]-=4,r[8+rrr])は不可
    int sp = XEiJ.regRn[15] - 4;
    XEiJ.busWl (sp, XEiJ.regRn[arr]);  //pushl
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.regRn[15] = (XEiJ.regRn[arr] = sp) + XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.regRn[15] = (XEiJ.regRn[arr] = sp) + XEiJ.busRwse (t);  //pcws
    }
  }  //irpLinkWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //UNLK Ar                                         |-|012346|-|-----|-----|          |0100_111_001_011_rrr
  //
  //UNLK Ar
  //  MOVEA.L Ar,A7;MOVEA.L (A7)+,Arと同じ
  //  UNLK A7はMOVEA.L A7,A7;MOVEA.L (A7)+,A7すなわちMOVEA.L (A7),A7と同じ
  //  ソースオペランドのポストインクリメントはデスティネーションオペランドが評価される前に完了しているとみなされる
  //    例えばMOVE.L (A0)+,(A0)+はMOVE.L (A0),(4,A0);ADDQ.L #8,A0と同じ
  //    MOVEA.L (A0)+,A0はポストインクリメントされたA0が(A0)から読み出された値で上書きされるのでMOVEA.L (A0),A0と同じ
  //  M68000PRMにUNLK Anの動作はAn→SP;(SP)→An;SP+4→SPだと書かれているがこれはn=7の場合に当てはまらない
  //  余談だが68040の初期のマスクセットはUNLK A7を実行すると固まるらしい
  public static void irpUnlk () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int arr = XEiJ.regOC - (0b0100_111_001_011_000 - 8);
    //評価順序に注意
    int sp = XEiJ.regRn[arr];
    XEiJ.regRn[15] = sp + 4;
    XEiJ.regRn[arr] = XEiJ.busRls (sp);  //popls
  }  //irpUnlk

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L Ar,USP                                   |-|012346|P|-----|-----|          |0100_111_001_100_rrr
  public static void irpMoveToUsp () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 4;
    XEiJ.mpuUSP = XEiJ.regRn[XEiJ.regOC - (0b0100_111_001_100_000 - 8)];
  }  //irpMoveToUsp

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVE.L USP,Ar                                   |-|012346|P|-----|-----|          |0100_111_001_101_rrr
  public static void irpMoveFromUsp () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 4;
    XEiJ.regRn[XEiJ.regOC - (0b0100_111_001_101_000 - 8)] = XEiJ.mpuUSP;
  }  //irpMoveFromUsp

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //RESET                                           |-|012346|P|-----|-----|          |0100_111_001_110_000
  public static void irpReset () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 132;
    XEiJ.irpReset ();
  }  //irpReset

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //NOP                                             |-|012346|-|-----|-----|          |0100_111_001_110_001
  public static void irpNop () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    //何もしない
  }  //irpNop

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //STOP #<data>                                    |-|012346|P|UUUUU|*****|          |0100_111_001_110_010-{data}
  //
  //STOP #<data>
  //    1. #<data>をsrに設定する
  //    2. pcを進める
  //    3. 以下のいずれかの条件が成立するまで停止する
  //      3a. トレース
  //      3b. マスクされているレベルよりも高い割り込み要求
  //      3c. リセット
  //  コアと一緒にデバイスを止めるわけにいかないので、ここでは条件が成立するまで同じ命令を繰り返すループ命令として実装する
  public static void irpStop () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 4;
    irpSetSR (XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。特権違反チェックが先
    if (XEiJ.mpuTraceFlag == 0) {  //トレースまたはマスクされているレベルよりも高い割り込み要求がない
      XEiJ.regPC = XEiJ.regPC0;  //ループ
      //任意の負荷率を100%に設定しているときSTOP命令が軽すぎると動作周波数が大きくなりすぎて割り込みがかかったとき次に進めなくなる
      //負荷率の計算にSTOP命令で止まっていた時間を含めないことにする
      XEiJ.mpuClockTime += XEiJ.TMR_FREQ * 4 / 1000000;  //4μs。10MHzのとき40clk
      XEiJ.mpuLastNano += 4000L;
    }
  }  //irpStop

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //RTE                                             |-|012346|P|UUUUU|*****|          |0100_111_001_110_011
  public static void irpRte () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 20;
    int sp = XEiJ.regRn[15];
    int format = XEiJ.busRws (sp + 6) >>> 12;
    int frameSize;
    if (format == 0x0) {  //010,020,030,040,060
      frameSize = 8;
    } else if (format == 0x8) {  //010
      frameSize = 58;
    } else {
      M68kException.m6eNumber = M68kException.M6E_FORMAT_ERROR;
      throw M68kException.m6eSignal;
    }
    XEiJ.regRn[15] = sp + frameSize;
    int newSR = XEiJ.busRwz (sp);  //popwz。ここでバスエラーが生じる可能性がある
    int newPC = XEiJ.busRls (sp + 2);  //popls
    //irpSetSRでモードが切り替わる場合があるのでその前にr[15]を更新しておくこと
    irpSetSR (newSR);  //ここでユーザモードに戻る場合がある。特権違反チェックが先
    irpSetPC (newPC);  //分岐ログが新しいsrを使う。順序に注意
  }  //irpRte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //RTD #<data>                                     |-|-12346|-|-----|-----|          |0100_111_001_110_100-{data}
  public static void irpRtd () throws M68kException {
    XEiJ.mpuCycleCount += 20;
    int sp = XEiJ.regRn[15];
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      XEiJ.regRn[15] = sp + 4 + XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    } else {
      int t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      XEiJ.regRn[15] = sp + 4 + XEiJ.busRwse (t);  //pcws
    }
    irpSetPC (XEiJ.busRls (sp));  //popls
  }  //irpRtd

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //RTS                                             |-|012346|-|-----|-----|          |0100_111_001_110_101
  public static void irpRts () throws M68kException {
    XEiJ.mpuCycleCount += 16;
    int sp = XEiJ.regRn[15];
    XEiJ.regRn[15] = sp + 4;
    irpSetPC (XEiJ.busRls (sp));  //popls
  }  //irpRts

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //TRAPV                                           |-|012346|-|---*-|-----|          |0100_111_001_110_110
  public static void irpTrapv () throws M68kException {
    if (XEiJ.TEST_BIT_1_SHIFT ? XEiJ.regCCR << 31 - 1 >= 0 : (XEiJ.regCCR & XEiJ.REG_CCR_V) == 0) {  //通過
      XEiJ.mpuCycleCount += 4;
    } else {
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_TRAPV_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpTrapv

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //RTR                                             |-|012346|-|UUUUU|*****|          |0100_111_001_110_111
  public static void irpRtr () throws M68kException {
    XEiJ.mpuCycleCount += 20;
    int sp = XEiJ.regRn[15];
    XEiJ.regRn[15] = sp + 6;
    XEiJ.regCCR = XEiJ.REG_CCR_MASK & XEiJ.busRwz (sp);  //popwz
    irpSetPC (XEiJ.busRlse (sp + 2));  //popls。ccrを読めたのだからspは奇数ではない
  }  //irpRtr

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVEC.L Rc,Rn                                   |-|-12346|P|-----|-----|          |0100_111_001_111_010-rnnncccccccccccc
  public static void irpMovecFromControl () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 10;
    int w;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
    } else {
      w = XEiJ.regPC;
      XEiJ.regPC = w + 2;
      w = XEiJ.busRwze (w);  //pcwz。拡張ワード
    }
    switch (w & 0x0fff) {
    case 0x000:  //SFC
      XEiJ.regRn[w >> 12] = XEiJ.mpuSFC;
      break;
    case 0x001:  //DFC
      XEiJ.regRn[w >> 12] = XEiJ.mpuDFC;
      break;
    case 0x801:  //VBR
      XEiJ.regRn[w >> 12] = XEiJ.mpuVBR;
      break;
    default:
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpMovecFromControl

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MOVEC.L Rn,Rc                                   |-|-12346|P|-----|-----|          |0100_111_001_111_011-rnnncccccccccccc
  public static void irpMovecToControl () throws M68kException {
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      M68kException.m6eNumber = M68kException.M6E_PRIVILEGE_VIOLATION;
      throw M68kException.m6eSignal;
    }
    //以下はスーパーバイザモード
    XEiJ.mpuCycleCount += 12;
    int w;
    if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
      w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
    } else {
      w = XEiJ.regPC;
      XEiJ.regPC = w + 2;
      w = XEiJ.busRwze (w);  //pcwz。拡張ワード
    }
    int d = XEiJ.regRn[w >> 12];
    switch (w & 0x0fff) {
    case 0x000:  //SFC
      XEiJ.mpuSFC = d & 0x00000007;
      break;
    case 0x001:  //DFC
      XEiJ.mpuDFC = d & 0x00000007;
      break;
    case 0x801:  //VBR
      XEiJ.mpuVBR = d & -4;  //4の倍数でないと困る
      break;
    default:
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpMovecToControl

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //JSR <ea>                                        |-|012346|-|-----|-----|  M  WXZP |0100_111_010_mmm_rrr
  //JBSR.L <label>                                  |A|012346|-|-----|-----|          |0100_111_010_111_001-{address}       [JSR <label>]
  public static void irpJsr () throws M68kException {
    XEiJ.mpuCycleCount += 16 - 8;
    int a = efaJmpJsr (XEiJ.regOC & 63);  //プッシュする前に実効アドレスを計算する
    XEiJ.busWl (XEiJ.regRn[15] -= 4, XEiJ.regPC);  //pushl
    irpSetPC (a);
  }  //irpJsr

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //JMP <ea>                                        |-|012346|-|-----|-----|  M  WXZP |0100_111_011_mmm_rrr
  //JBRA.L <label>                                  |A|012346|-|-----|-----|          |0100_111_011_111_001-{address}       [JMP <label>]
  public static void irpJmp () throws M68kException {
    //XEiJ.mpuCycleCount += 8 - 8;
    irpSetPC (efaJmpJsr (XEiJ.regOC & 63));
  }  //irpJmp

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDQ.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_000_mmm_rrr
  //INC.B <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_000_mmm_rrr [ADDQ.B #1,<ea>]
  public static void irpAddqByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y = ((XEiJ.regOC >> 9) - 1 & 7) + 1;  //qqq==0?8:qqq
    int z;
    if (ea < XEiJ.EA_AR) {  //ADDQ.B #<data>,Dr
      XEiJ.mpuCycleCount += 4;
      z = (byte) (XEiJ.regRn[ea] = ~0xff & (x = XEiJ.regRn[ea]) | 0xff & (x = (byte) x) + y);
    } else {  //ADDQ.B #<data>,<mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = (byte) ((x = XEiJ.busRbs (a)) + y));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           (~x & z) >>> 31 << 1 |
           (x & ~z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_addq
  }  //irpAddqByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDQ.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_001_mmm_rrr
  //ADDQ.W #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_001_001_rrr
  //INC.W <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_001_mmm_rrr [ADDQ.W #1,<ea>]
  //INC.W Ar                                        |A|012346|-|-----|-----| A        |0101_001_001_001_rrr [ADDQ.W #1,Ar]
  //
  //ADDQ.W #<data>,Ar
  //  ソースを符号拡張してロングで加算する
  public static void irpAddqWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y = ((XEiJ.regOC >> 9) - 1 & 7) + 1;  //qqq==0?8:qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //ADDQ.W #<data>,Ar
      XEiJ.mpuCycleCount += 8;  //MC68000 User's Manualに4と書いてあるのは8の間違い
      XEiJ.regRn[ea] += y;  //ロングで計算する。このr[ea]はアドレスレジスタ
      //ccrは操作しない
    } else {
      int x;
      int z;
      if (ea < XEiJ.EA_AR) {  //ADDQ.W #<data>,Dr
        XEiJ.mpuCycleCount += 4;
        z = (short) (XEiJ.regRn[ea] = ~0xffff & (x = XEiJ.regRn[ea]) | (char) ((x = (short) x) + y));
      } else {  //ADDQ.W #<data>,<mem>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltWord (ea);
        XEiJ.busWw (a, z = (short) ((x = XEiJ.busRws (a)) + y));
      }
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
             (~x & z) >>> 31 << 1 |
             (x & ~z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_addq
    }
  }  //irpAddqWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDQ.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_010_mmm_rrr
  //ADDQ.L #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_010_001_rrr
  //INC.L <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_010_mmm_rrr [ADDQ.L #1,<ea>]
  //INC.L Ar                                        |A|012346|-|-----|-----| A        |0101_001_010_001_rrr [ADDQ.L #1,Ar]
  public static void irpAddqLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y = ((XEiJ.regOC >> 9) - 1 & 7) + 1;  //qqq==0?8:qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //ADDQ.L #<data>,Ar
      XEiJ.mpuCycleCount += 8;
      XEiJ.regRn[ea] += y;  //このr[ea]はアドレスレジスタ
      //ccrは操作しない
    } else {
      int x;
      int z;
      if (ea < XEiJ.EA_AR) {  //ADDQ.L #<data>,Dr
        XEiJ.mpuCycleCount += 8;
        XEiJ.regRn[ea] = z = (x = XEiJ.regRn[ea]) + y;
      } else {  //ADDQ.L #<data>,<mem>
        XEiJ.mpuCycleCount += 12;
        int a = efaMltLong (ea);
        XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) + y);
      }
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
             (~x & z) >>> 31 << 1 |
             (x & ~z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_addq
    }
  }  //irpAddqLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ST.B <ea>                                       |-|012346|-|-----|-----|D M+-WXZ  |0101_000_011_mmm_rrr
  //SNF.B <ea>                                      |A|012346|-|-----|-----|D M+-WXZ  |0101_000_011_mmm_rrr [ST.B <ea>]
  //DBT.W Dr,<label>                                |-|012346|-|-----|-----|          |0101_000_011_001_rrr-{offset}
  //DBNF.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_011_001_rrr-{offset}        [DBT.W Dr,<label>]
  public static void irpSt () throws M68kException {
    int ea = XEiJ.regOC & 63;
    //DBT.W Dr,<label>よりもST.B Drを優先する
    if (ea < XEiJ.EA_AR) {  //ST.B Dr
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[ea] |= 0xff;
    } else if (ea < XEiJ.EA_MM) {  //DBT.W Dr,<label>
      //条件が成立しているので通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //ST.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), 0xff);
    }
  }  //irpSt

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBQ.B #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_100_mmm_rrr
  //DEC.B <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_100_mmm_rrr [SUBQ.B #1,<ea>]
  public static void irpSubqByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int x;
    int y = ((XEiJ.regOC >> 9) - 1 & 7) + 1;  //qqq==0?8:qqq
    int z;
    if (ea < XEiJ.EA_AR) {  //SUBQ.B #<data>,Dr
      XEiJ.mpuCycleCount += 4;
      z = (byte) (XEiJ.regRn[ea] = ~0xff & (x = XEiJ.regRn[ea]) | 0xff & (x = (byte) x) - y);
    } else {  //SUBQ.B #<data>,<mem>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      XEiJ.busWb (a, z = (byte) ((x = XEiJ.busRbs (a)) - y));
    }
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           (x & ~z) >>> 31 << 1 |
           (~x & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_subq
  }  //irpSubqByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBQ.W #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_101_mmm_rrr
  //SUBQ.W #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_101_001_rrr
  //DEC.W <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_101_mmm_rrr [SUBQ.W #1,<ea>]
  //DEC.W Ar                                        |A|012346|-|-----|-----| A        |0101_001_101_001_rrr [SUBQ.W #1,Ar]
  //
  //SUBQ.W #<data>,Ar
  //  ソースを符号拡張してロングで減算する
  public static void irpSubqWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y = ((XEiJ.regOC >> 9) - 1 & 7) + 1;  //qqq==0?8:qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //SUBQ.W #<data>,Ar
      XEiJ.mpuCycleCount += 8;
      XEiJ.regRn[ea] -= y;  //ロングで計算する。このr[ea]はアドレスレジスタ
      //ccrは操作しない
    } else {
      int x;
      int z;
      if (ea < XEiJ.EA_AR) {  //SUBQ.W #<data>,Dr
        XEiJ.mpuCycleCount += 4;
        z = (short) (XEiJ.regRn[ea] = ~0xffff & (x = XEiJ.regRn[ea]) | (char) ((x = (short) x) - y));
      } else {  //SUBQ.W #<data>,<mem>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltWord (ea);
        XEiJ.busWw (a, z = (short) ((x = XEiJ.busRws (a)) - y));
      }
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
             (x & ~z) >>> 31 << 1 |
             (~x & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_subq
    }
  }  //irpSubqWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBQ.L #<data>,<ea>                             |-|012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_110_mmm_rrr
  //SUBQ.L #<data>,Ar                               |-|012346|-|-----|-----| A        |0101_qqq_110_001_rrr
  //DEC.L <ea>                                      |A|012346|-|UUUUU|*****|D M+-WXZ  |0101_001_110_mmm_rrr [SUBQ.L #1,<ea>]
  //DEC.L Ar                                        |A|012346|-|-----|-----| A        |0101_001_110_001_rrr [SUBQ.L #1,Ar]
  public static void irpSubqLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int y = ((XEiJ.regOC >> 9) - 1 & 7) + 1;  //qqq==0?8:qqq
    if (ea >> 3 == XEiJ.MMM_AR) {  //SUBQ.L #<data>,Ar
      XEiJ.mpuCycleCount += 8;
      XEiJ.regRn[ea] -= y;  //このr[ea]はアドレスレジスタ
      //ccrは操作しない
    } else {
      int x;
      int z;
      if (ea < XEiJ.EA_AR) {  //SUBQ.L #<data>,Dr
        XEiJ.mpuCycleCount += 8;
        XEiJ.regRn[ea] = z = (x = XEiJ.regRn[ea]) - y;
      } else {  //SUBQ.L #<data>,<mem>
        XEiJ.mpuCycleCount += 12;
        int a = efaMltLong (ea);
        XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) - y);
      }
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
             (x & ~z) >>> 31 << 1 |
             (~x & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_subq
    }
  }  //irpSubqLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SF.B <ea>                                       |-|012346|-|-----|-----|D M+-WXZ  |0101_000_111_mmm_rrr
  //SNT.B <ea>                                      |A|012346|-|-----|-----|D M+-WXZ  |0101_000_111_mmm_rrr [SF.B <ea>]
  //DBF.W Dr,<label>                                |-|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}
  //DBNT.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}        [DBF.W Dr,<label>]
  //DBRA.W Dr,<label>                               |A|012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}        [DBF.W Dr,<label>]
  public static void irpSf () throws M68kException {
    int ea = XEiJ.regOC & 63;
    //DBRA.W Dr,<label>よりもSF.B Drを優先する
    if (ea < XEiJ.EA_AR) {  //SF.B Dr
      XEiJ.mpuCycleCount += 4;
      XEiJ.regRn[ea] &= ~0xff;
    } else if (ea < XEiJ.EA_MM) {  //DBRA.W Dr,<label>
      //条件が成立していないのでデクリメント
      int rrr = XEiJ.regOC & 7;
      int t = XEiJ.regRn[rrr];
      if ((short) t == 0) {  //Drの下位16bitが0なので通過
        XEiJ.mpuCycleCount += 14;
        XEiJ.regRn[rrr] = t + 65535;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {  //Drの下位16bitが0でないのでジャンプ
        XEiJ.mpuCycleCount += 10;
        XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
        irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
      }
    } else {  //SF.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), 0x00);
    }
  }  //irpSf

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SHI.B <ea>                                      |-|012346|-|--*-*|-----|D M+-WXZ  |0101_001_011_mmm_rrr
  //SNLS.B <ea>                                     |A|012346|-|--*-*|-----|D M+-WXZ  |0101_001_011_mmm_rrr [SHI.B <ea>]
  //DBHI.W Dr,<label>                               |-|012346|-|--*-*|-----|          |0101_001_011_001_rrr-{offset}
  //DBNLS.W Dr,<label>                              |A|012346|-|--*-*|-----|          |0101_001_011_001_rrr-{offset}        [DBHI.W Dr,<label>]
  public static void irpShi () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBHI.W Dr,<label>
      if (XEiJ.MPU_CC_HI << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SHI.B Dr
      if (XEiJ.MPU_CC_HI << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SHI.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_HI << XEiJ.regCCR >> 31);
    }
  }  //irpShi

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SLS.B <ea>                                      |-|012346|-|--*-*|-----|D M+-WXZ  |0101_001_111_mmm_rrr
  //SNHI.B <ea>                                     |A|012346|-|--*-*|-----|D M+-WXZ  |0101_001_111_mmm_rrr [SLS.B <ea>]
  //DBLS.W Dr,<label>                               |-|012346|-|--*-*|-----|          |0101_001_111_001_rrr-{offset}
  //DBNHI.W Dr,<label>                              |A|012346|-|--*-*|-----|          |0101_001_111_001_rrr-{offset}        [DBLS.W Dr,<label>]
  public static void irpSls () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBLS.W Dr,<label>
      if (XEiJ.MPU_CC_LS << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SLS.B Dr
      if (XEiJ.MPU_CC_LS << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SLS.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_LS << XEiJ.regCCR >> 31);
    }
  }  //irpSls

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SCC.B <ea>                                      |-|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr
  //SHS.B <ea>                                      |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
  //SNCS.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
  //SNLO.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_011_mmm_rrr [SCC.B <ea>]
  //DBCC.W Dr,<label>                               |-|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}
  //DBHS.W Dr,<label>                               |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
  //DBNCS.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
  //DBNLO.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_011_001_rrr-{offset}        [DBCC.W Dr,<label>]
  public static void irpShs () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBHS.W Dr,<label>
      if (XEiJ.MPU_CC_HS << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SHS.B Dr
      if (XEiJ.MPU_CC_HS << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SHS.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_HS << XEiJ.regCCR >> 31);
    }
  }  //irpShs

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SCS.B <ea>                                      |-|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr
  //SLO.B <ea>                                      |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
  //SNCC.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
  //SNHS.B <ea>                                     |A|012346|-|----*|-----|D M+-WXZ  |0101_010_111_mmm_rrr [SCS.B <ea>]
  //DBCS.W Dr,<label>                               |-|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}
  //DBLO.W Dr,<label>                               |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
  //DBNCC.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
  //DBNHS.W Dr,<label>                              |A|012346|-|----*|-----|          |0101_010_111_001_rrr-{offset}        [DBCS.W Dr,<label>]
  public static void irpSlo () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBLO.W Dr,<label>
      if (XEiJ.MPU_CC_LO << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SLO.B Dr
      if (XEiJ.MPU_CC_LO << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SLO.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_LO << XEiJ.regCCR >> 31);
    }
  }  //irpSlo

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SNE.B <ea>                                      |-|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr
  //SNEQ.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
  //SNZ.B <ea>                                      |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
  //SNZE.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_011_mmm_rrr [SNE.B <ea>]
  //DBNE.W Dr,<label>                               |-|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}
  //DBNEQ.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
  //DBNZ.W Dr,<label>                               |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
  //DBNZE.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_011_001_rrr-{offset}        [DBNE.W Dr,<label>]
  public static void irpSne () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBNE.W Dr,<label>
      if (XEiJ.MPU_CC_NE << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SNE.B Dr
      if (XEiJ.MPU_CC_NE << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SNE.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_NE << XEiJ.regCCR >> 31);
    }
  }  //irpSne

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SEQ.B <ea>                                      |-|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr
  //SNNE.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
  //SNNZ.B <ea>                                     |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
  //SZE.B <ea>                                      |A|012346|-|--*--|-----|D M+-WXZ  |0101_011_111_mmm_rrr [SEQ.B <ea>]
  //DBEQ.W Dr,<label>                               |-|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}
  //DBNNE.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
  //DBNNZ.W Dr,<label>                              |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
  //DBZE.W Dr,<label>                               |A|012346|-|--*--|-----|          |0101_011_111_001_rrr-{offset}        [DBEQ.W Dr,<label>]
  public static void irpSeq () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBEQ.W Dr,<label>
      if (XEiJ.MPU_CC_EQ << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SEQ.B Dr
      if (XEiJ.MPU_CC_EQ << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SEQ.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_EQ << XEiJ.regCCR >> 31);
    }
  }  //irpSeq

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SVC.B <ea>                                      |-|012346|-|---*-|-----|D M+-WXZ  |0101_100_011_mmm_rrr
  //SNVS.B <ea>                                     |A|012346|-|---*-|-----|D M+-WXZ  |0101_100_011_mmm_rrr [SVC.B <ea>]
  //DBVC.W Dr,<label>                               |-|012346|-|---*-|-----|          |0101_100_011_001_rrr-{offset}
  //DBNVS.W Dr,<label>                              |A|012346|-|---*-|-----|          |0101_100_011_001_rrr-{offset}        [DBVC.W Dr,<label>]
  public static void irpSvc () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBVC.W Dr,<label>
      if (XEiJ.MPU_CC_VC << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SVC.B Dr
      if (XEiJ.MPU_CC_VC << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SVC.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_VC << XEiJ.regCCR >> 31);
    }
  }  //irpSvc

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SVS.B <ea>                                      |-|012346|-|---*-|-----|D M+-WXZ  |0101_100_111_mmm_rrr
  //SNVC.B <ea>                                     |A|012346|-|---*-|-----|D M+-WXZ  |0101_100_111_mmm_rrr [SVS.B <ea>]
  //DBVS.W Dr,<label>                               |-|012346|-|---*-|-----|          |0101_100_111_001_rrr-{offset}
  //DBNVC.W Dr,<label>                              |A|012346|-|---*-|-----|          |0101_100_111_001_rrr-{offset}        [DBVS.W Dr,<label>]
  public static void irpSvs () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBVS.W Dr,<label>
      if (XEiJ.MPU_CC_VS << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SVS.B Dr
      if (XEiJ.MPU_CC_VS << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SVS.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_VS << XEiJ.regCCR >> 31);
    }
  }  //irpSvs

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SPL.B <ea>                                      |-|012346|-|-*---|-----|D M+-WXZ  |0101_101_011_mmm_rrr
  //SNMI.B <ea>                                     |A|012346|-|-*---|-----|D M+-WXZ  |0101_101_011_mmm_rrr [SPL.B <ea>]
  //DBPL.W Dr,<label>                               |-|012346|-|-*---|-----|          |0101_101_011_001_rrr-{offset}
  //DBNMI.W Dr,<label>                              |A|012346|-|-*---|-----|          |0101_101_011_001_rrr-{offset}        [DBPL.W Dr,<label>]
  public static void irpSpl () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBPL.W Dr,<label>
      if (XEiJ.MPU_CC_PL << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SPL.B Dr
      if (XEiJ.MPU_CC_PL << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SPL.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_PL << XEiJ.regCCR >> 31);
    }
  }  //irpSpl

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SMI.B <ea>                                      |-|012346|-|-*---|-----|D M+-WXZ  |0101_101_111_mmm_rrr
  //SNPL.B <ea>                                     |A|012346|-|-*---|-----|D M+-WXZ  |0101_101_111_mmm_rrr [SMI.B <ea>]
  //DBMI.W Dr,<label>                               |-|012346|-|-*---|-----|          |0101_101_111_001_rrr-{offset}
  //DBNPL.W Dr,<label>                              |A|012346|-|-*---|-----|          |0101_101_111_001_rrr-{offset}        [DBMI.W Dr,<label>]
  public static void irpSmi () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBMI.W Dr,<label>
      if (XEiJ.MPU_CC_MI << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SMI.B Dr
      if (XEiJ.MPU_CC_MI << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SMI.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_MI << XEiJ.regCCR >> 31);
    }
  }  //irpSmi

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SGE.B <ea>                                      |-|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_011_mmm_rrr
  //SNLT.B <ea>                                     |A|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_011_mmm_rrr [SGE.B <ea>]
  //DBGE.W Dr,<label>                               |-|012346|-|-*-*-|-----|          |0101_110_011_001_rrr-{offset}
  //DBNLT.W Dr,<label>                              |A|012346|-|-*-*-|-----|          |0101_110_011_001_rrr-{offset}        [DBGE.W Dr,<label>]
  public static void irpSge () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBGE.W Dr,<label>
      if (XEiJ.MPU_CC_GE << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SGE.B Dr
      if (XEiJ.MPU_CC_GE << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SGE.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_GE << XEiJ.regCCR >> 31);
    }
  }  //irpSge

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SLT.B <ea>                                      |-|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_111_mmm_rrr
  //SNGE.B <ea>                                     |A|012346|-|-*-*-|-----|D M+-WXZ  |0101_110_111_mmm_rrr [SLT.B <ea>]
  //DBLT.W Dr,<label>                               |-|012346|-|-*-*-|-----|          |0101_110_111_001_rrr-{offset}
  //DBNGE.W Dr,<label>                              |A|012346|-|-*-*-|-----|          |0101_110_111_001_rrr-{offset}        [DBLT.W Dr,<label>]
  public static void irpSlt () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBLT.W Dr,<label>
      if (XEiJ.MPU_CC_LT << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SLT.B Dr
      if (XEiJ.MPU_CC_LT << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SLT.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_LT << XEiJ.regCCR >> 31);
    }
  }  //irpSlt

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SGT.B <ea>                                      |-|012346|-|-***-|-----|D M+-WXZ  |0101_111_011_mmm_rrr
  //SNLE.B <ea>                                     |A|012346|-|-***-|-----|D M+-WXZ  |0101_111_011_mmm_rrr [SGT.B <ea>]
  //DBGT.W Dr,<label>                               |-|012346|-|-***-|-----|          |0101_111_011_001_rrr-{offset}
  //DBNLE.W Dr,<label>                              |A|012346|-|-***-|-----|          |0101_111_011_001_rrr-{offset}        [DBGT.W Dr,<label>]
  public static void irpSgt () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBGT.W Dr,<label>
      if (XEiJ.MPU_CC_GT << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SGT.B Dr
      if (XEiJ.MPU_CC_GT << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SGT.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_GT << XEiJ.regCCR >> 31);
    }
  }  //irpSgt

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SLE.B <ea>                                      |-|012346|-|-***-|-----|D M+-WXZ  |0101_111_111_mmm_rrr
  //SNGT.B <ea>                                     |A|012346|-|-***-|-----|D M+-WXZ  |0101_111_111_mmm_rrr [SLE.B <ea>]
  //DBLE.W Dr,<label>                               |-|012346|-|-***-|-----|          |0101_111_111_001_rrr-{offset}
  //DBNGT.W Dr,<label>                              |A|012346|-|-***-|-----|          |0101_111_111_001_rrr-{offset}        [DBLE.W Dr,<label>]
  public static void irpSle () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //DBLE.W Dr,<label>
      if (XEiJ.MPU_CC_LE << XEiJ.regCCR < 0) {
        //条件が成立しているので通過
        XEiJ.mpuCycleCount += 12;
        if (XEiJ.MPU_OMIT_OFFSET_READ) {
          //リードを省略する
        } else {
          XEiJ.busRws (XEiJ.regPC);
        }
        XEiJ.regPC += 2;
      } else {
        //条件が成立していないのでデクリメント
        int rrr = XEiJ.regOC & 7;
        int t = XEiJ.regRn[rrr];
        if ((short) t == 0) {  //Drの下位16bitが0なので通過
          XEiJ.mpuCycleCount += 14;
          XEiJ.regRn[rrr] = t + 65535;
          if (XEiJ.MPU_OMIT_OFFSET_READ) {
            //リードを省略する
          } else {
            XEiJ.busRws (XEiJ.regPC);
          }
          XEiJ.regPC += 2;
        } else {  //Drの下位16bitが0でないのでジャンプ
          XEiJ.mpuCycleCount += 10;
          XEiJ.regRn[rrr] = t - 1;  //下位16bitが0でないので上位16bitは変化しない
          irpSetPC (XEiJ.regPC + XEiJ.busRws (XEiJ.regPC));  //pc==pc0+2
        }
      }
    } else if (ea < XEiJ.EA_AR) {  //SLE.B Dr
      if (XEiJ.MPU_CC_LE << XEiJ.regCCR < 0) {  //セット
        XEiJ.mpuCycleCount += 6;
        XEiJ.regRn[ea] |= 0xff;
      } else {  //クリア
        XEiJ.mpuCycleCount += 4;
        XEiJ.regRn[ea] &= ~0xff;
      }
    } else {  //SLE.B <mem>
      XEiJ.mpuCycleCount += 8;
      XEiJ.busWb (efaMltByte (ea), XEiJ.MPU_CC_LE << XEiJ.regCCR >> 31);
    }
  }  //irpSle

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BRA.W <label>                                   |-|012346|-|-----|-----|          |0110_000_000_000_000-{offset}
  //JBRA.W <label>                                  |A|012346|-|-----|-----|          |0110_000_000_000_000-{offset}        [BRA.W <label>]
  //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_000_sss_sss (s is not equal to 0)
  //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_000_sss_sss (s is not equal to 0)   [BRA.S <label>]
  public static void irpBrasw () throws M68kException {
    XEiJ.mpuCycleCount += 10;
    int t = XEiJ.regPC;  //pc0+2
    int s = (byte) XEiJ.regOC;  //オフセット
    if (s == 0) {  //BRA.W
      XEiJ.regPC = t + 2;
      s = XEiJ.busRwse (t);  //pcws
    } else {  //BRA.S
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
    }
    irpSetPC (t + s);  //pc0+2+オフセット
  }  //irpBrasw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_001_sss_sss
  //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_001_sss_sss [BRA.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BRA.S <label>                                   |-|012346|-|-----|-----|          |0110_000_010_sss_sss
  //JBRA.S <label>                                  |A|012346|-|-----|-----|          |0110_000_010_sss_sss [BRA.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BRA.S <label>                                   |-|01----|-|-----|-----|          |0110_000_011_sss_sss
  //JBRA.S <label>                                  |A|01----|-|-----|-----|          |0110_000_011_sss_sss [BRA.S <label>]
  public static void irpBras () throws M68kException {
    XEiJ.mpuCycleCount += 10;
    int t = XEiJ.regPC;  //pc0+2
    int s = (byte) XEiJ.regOC;  //オフセット
    //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
    //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
    if (XEiJ.MPU_OMIT_EXTRA_READ) {
      //! 軽量化。リードを省略する
    } else {
      XEiJ.busRwse (t);  //pcws
    }
    irpSetPC (t + s);  //pc0+2+オフセット
  }  //irpBras

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BSR.W <label>                                   |-|012346|-|-----|-----|          |0110_000_100_000_000-{offset}
  //JBSR.W <label>                                  |A|012346|-|-----|-----|          |0110_000_100_000_000-{offset}        [BSR.W <label>]
  //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_100_sss_sss (s is not equal to 0)
  //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_100_sss_sss (s is not equal to 0)   [BSR.S <label>]
  public static void irpBsrsw () throws M68kException {
    XEiJ.mpuCycleCount += 18;
    int t = XEiJ.regPC;  //pc0+2
    int s = (byte) XEiJ.regOC;  //オフセット
    if (s == 0) {  //BSR.W
      XEiJ.regPC = t + 2;
      s = XEiJ.busRwse (t);  //pcws
    } else {  //BSR.S
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
    }
    XEiJ.busWl (XEiJ.regRn[15] -= 4, XEiJ.regPC);  //pushl
    irpSetPC (t + s);  //pc0+2+オフセット
  }  //irpBsrsw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_101_sss_sss
  //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_101_sss_sss [BSR.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BSR.S <label>                                   |-|012346|-|-----|-----|          |0110_000_110_sss_sss
  //JBSR.S <label>                                  |A|012346|-|-----|-----|          |0110_000_110_sss_sss [BSR.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BSR.S <label>                                   |-|01----|-|-----|-----|          |0110_000_111_sss_sss
  //JBSR.S <label>                                  |A|01----|-|-----|-----|          |0110_000_111_sss_sss [BSR.S <label>]
  public static void irpBsrs () throws M68kException {
    XEiJ.mpuCycleCount += 18;
    int t = XEiJ.regPC;  //pc0+2
    int s = (byte) XEiJ.regOC;  //オフセット
    //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
    //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
    if (XEiJ.MPU_OMIT_EXTRA_READ) {
      //! 軽量化。リードを省略する
    } else {
      XEiJ.busRwse (t);  //pcws
    }
    XEiJ.busWl (XEiJ.regRn[15] -= 4, XEiJ.regPC);  //pushl
    irpSetPC (t + s);  //pc0+2+オフセット
  }  //irpBsrs

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BHI.W <label>                                   |-|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}
  //BNLS.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
  //JBHI.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
  //JBNLS.W <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_000_000-{offset}        [BHI.W <label>]
  //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)
  //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
  //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
  //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_sss_sss (s is not equal to 0)   [BHI.S <label>]
  //JBLS.L <label>                                  |A|012346|-|--*-*|-----|          |0110_001_000_000_110-0100111011111001-{address}      [BHI.S (*)+8;JMP <label>]
  //JBNHI.L <label>                                 |A|012346|-|--*-*|-----|          |0110_001_000_000_110-0100111011111001-{address}      [BHI.S (*)+8;JMP <label>]
  public static void irpBhisw () throws M68kException {
    if (XEiJ.MPU_CC_HI << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6200) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBhisw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_001_sss_sss
  //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
  //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
  //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_001_sss_sss [BHI.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BHI.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_010_sss_sss
  //BNLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
  //JBHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
  //JBNLS.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_010_sss_sss [BHI.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BHI.S <label>                                   |-|01----|-|--*-*|-----|          |0110_001_011_sss_sss
  //BNLS.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
  //JBHI.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
  //JBNLS.S <label>                                 |A|01----|-|--*-*|-----|          |0110_001_011_sss_sss [BHI.S <label>]
  public static void irpBhis () throws M68kException {
    if (XEiJ.MPU_CC_HI << XEiJ.regCCR < 0) {  //Bcc.Sでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBhis

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLS.W <label>                                   |-|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}
  //BNHI.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
  //JBLS.W <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
  //JBNHI.W <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_000_000-{offset}        [BLS.W <label>]
  //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)
  //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
  //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
  //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_sss_sss (s is not equal to 0)   [BLS.S <label>]
  //JBHI.L <label>                                  |A|012346|-|--*-*|-----|          |0110_001_100_000_110-0100111011111001-{address}      [BLS.S (*)+8;JMP <label>]
  //JBNLS.L <label>                                 |A|012346|-|--*-*|-----|          |0110_001_100_000_110-0100111011111001-{address}      [BLS.S (*)+8;JMP <label>]
  public static void irpBlssw () throws M68kException {
    if (XEiJ.MPU_CC_LS << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6300) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBlssw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_101_sss_sss
  //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
  //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
  //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_101_sss_sss [BLS.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLS.S <label>                                   |-|012346|-|--*-*|-----|          |0110_001_110_sss_sss
  //BNHI.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
  //JBLS.S <label>                                  |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
  //JBNHI.S <label>                                 |A|012346|-|--*-*|-----|          |0110_001_110_sss_sss [BLS.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLS.S <label>                                   |-|01----|-|--*-*|-----|          |0110_001_111_sss_sss
  //BNHI.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
  //JBLS.S <label>                                  |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
  //JBNHI.S <label>                                 |A|01----|-|--*-*|-----|          |0110_001_111_sss_sss [BLS.S <label>]
  public static void irpBlss () throws M68kException {
    if (XEiJ.MPU_CC_LS << XEiJ.regCCR < 0) {  //Bcc.Sでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBlss

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCC.W <label>                                   |-|012346|-|----*|-----|          |0110_010_000_000_000-{offset}
  //BHS.W <label>                                   |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //BNCS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //BNLO.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //JBCC.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //JBHS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //JBNCS.W <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //JBNLO.W <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_000-{offset}        [BCC.W <label>]
  //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)
  //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_000_sss_sss (s is not equal to 0)   [BCC.S <label>]
  //JBCS.L <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
  //JBLO.L <label>                                  |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
  //JBNCC.L <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
  //JBNHS.L <label>                                 |A|012346|-|----*|-----|          |0110_010_000_000_110-0100111011111001-{address}      [BCC.S (*)+8;JMP <label>]
  public static void irpBhssw () throws M68kException {
    if (XEiJ.MPU_CC_HS << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6400) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBhssw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_001_sss_sss
  //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_001_sss_sss [BCC.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCC.S <label>                                   |-|012346|-|----*|-----|          |0110_010_010_sss_sss
  //BHS.S <label>                                   |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //BNCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //BNLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //JBCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //JBHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //JBNCS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //JBNLO.S <label>                                 |A|012346|-|----*|-----|          |0110_010_010_sss_sss [BCC.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCC.S <label>                                   |-|01----|-|----*|-----|          |0110_010_011_sss_sss
  //BHS.S <label>                                   |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  //BNCS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  //BNLO.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  //JBCC.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  //JBHS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  //JBNCS.S <label>                                 |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  //JBNLO.S <label>                                 |A|01----|-|----*|-----|          |0110_010_011_sss_sss [BCC.S <label>]
  public static void irpBhss () throws M68kException {
    if (XEiJ.MPU_CC_HS << XEiJ.regCCR < 0) {  //Bcc.Sでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBhss

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCS.W <label>                                   |-|012346|-|----*|-----|          |0110_010_100_000_000-{offset}
  //BLO.W <label>                                   |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //BNCC.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //BNHS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //JBCS.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //JBLO.W <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //JBNCC.W <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //JBNHS.W <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_000-{offset}        [BCS.W <label>]
  //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)
  //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_100_sss_sss (s is not equal to 0)   [BCS.S <label>]
  //JBCC.L <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
  //JBHS.L <label>                                  |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
  //JBNCS.L <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
  //JBNLO.L <label>                                 |A|012346|-|----*|-----|          |0110_010_100_000_110-0100111011111001-{address}      [BCS.S (*)+8;JMP <label>]
  public static void irpBlosw () throws M68kException {
    if (XEiJ.MPU_CC_LO << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6500) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBlosw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_101_sss_sss
  //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_101_sss_sss [BCS.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCS.S <label>                                   |-|012346|-|----*|-----|          |0110_010_110_sss_sss
  //BLO.S <label>                                   |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //BNCC.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //BNHS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //JBCS.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //JBLO.S <label>                                  |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //JBNCC.S <label>                                 |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //JBNHS.S <label>                                 |A|012346|-|----*|-----|          |0110_010_110_sss_sss [BCS.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BCS.S <label>                                   |-|01----|-|----*|-----|          |0110_010_111_sss_sss
  //BLO.S <label>                                   |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  //BNCC.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  //BNHS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  //JBCS.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  //JBLO.S <label>                                  |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  //JBNCC.S <label>                                 |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  //JBNHS.S <label>                                 |A|01----|-|----*|-----|          |0110_010_111_sss_sss [BCS.S <label>]
  public static void irpBlos () throws M68kException {
    if (XEiJ.MPU_CC_LO << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBlos

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BNE.W <label>                                   |-|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}
  //BNEQ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //BNZ.W <label>                                   |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //BNZE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //JBNE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //JBNEQ.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //JBNZ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //JBNZE.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_000-{offset}        [BNE.W <label>]
  //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)
  //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_sss_sss (s is not equal to 0)   [BNE.S <label>]
  //JBEQ.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  //JBNEQ.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  //JBNNE.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  //JBNNZ.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  //JBNZ.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  //JBNZE.L <label>                                 |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  //JBZE.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_000_000_110-0100111011111001-{address}      [BNE.S (*)+8;JMP <label>]
  public static void irpBnesw () throws M68kException {
    if (XEiJ.MPU_CC_NE << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6600) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBnesw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_001_sss_sss
  //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_001_sss_sss [BNE.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BNE.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_010_sss_sss
  //BNEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //BNZ.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //BNZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //JBNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //JBNEQ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //JBNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //JBNZE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_010_sss_sss [BNE.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BNE.S <label>                                   |-|01----|-|--*--|-----|          |0110_011_011_sss_sss
  //BNEQ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  //BNZ.S <label>                                   |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  //BNZE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  //JBNE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  //JBNEQ.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  //JBNZ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  //JBNZE.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_011_sss_sss [BNE.S <label>]
  public static void irpBnes () throws M68kException {
    if (XEiJ.MPU_CC_NE << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBnes

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BEQ.W <label>                                   |-|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}
  //BNNE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //BNNZ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //BZE.W <label>                                   |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //JBEQ.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //JBNNE.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //JBNNZ.W <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //JBZE.W <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_000-{offset}        [BEQ.W <label>]
  //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)
  //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_sss_sss (s is not equal to 0)   [BEQ.S <label>]
  //JBNE.L <label>                                  |A|012346|-|--*--|-----|          |0110_011_100_000_110-0100111011111001-{address}      [BEQ.S (*)+8;JMP <label>]
  public static void irpBeqsw () throws M68kException {
    if (XEiJ.MPU_CC_EQ << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6700) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBeqsw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_101_sss_sss
  //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_101_sss_sss [BEQ.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BEQ.S <label>                                   |-|012346|-|--*--|-----|          |0110_011_110_sss_sss
  //BNNE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //BNNZ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //BZE.S <label>                                   |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //JBEQ.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //JBNNE.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //JBNNZ.S <label>                                 |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //JBZE.S <label>                                  |A|012346|-|--*--|-----|          |0110_011_110_sss_sss [BEQ.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BEQ.S <label>                                   |-|01----|-|--*--|-----|          |0110_011_111_sss_sss
  //BNNE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  //BNNZ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  //BZE.S <label>                                   |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  //JBEQ.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  //JBNNE.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  //JBNNZ.S <label>                                 |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  //JBZE.S <label>                                  |A|01----|-|--*--|-----|          |0110_011_111_sss_sss [BEQ.S <label>]
  public static void irpBeqs () throws M68kException {
    if (XEiJ.MPU_CC_EQ << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBeqs

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVC.W <label>                                   |-|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}
  //BNVS.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
  //JBNVS.W <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
  //JBVC.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_000-{offset}        [BVC.W <label>]
  //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)
  //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
  //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
  //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_sss_sss (s is not equal to 0)   [BVC.S <label>]
  //JBNVC.L <label>                                 |A|012346|-|---*-|-----|          |0110_100_000_000_110-0100111011111001-{address}      [BVC.S (*)+8;JMP <label>]
  //JBVS.L <label>                                  |A|012346|-|---*-|-----|          |0110_100_000_000_110-0100111011111001-{address}      [BVC.S (*)+8;JMP <label>]
  public static void irpBvcsw () throws M68kException {
    if (XEiJ.MPU_CC_VC << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6800) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBvcsw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_001_sss_sss
  //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
  //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
  //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_001_sss_sss [BVC.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVC.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_010_sss_sss
  //BNVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
  //JBNVS.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
  //JBVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_010_sss_sss [BVC.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVC.S <label>                                   |-|01----|-|---*-|-----|          |0110_100_011_sss_sss
  //BNVS.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
  //JBNVS.S <label>                                 |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
  //JBVC.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_011_sss_sss [BVC.S <label>]
  public static void irpBvcs () throws M68kException {
    if (XEiJ.MPU_CC_VC << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBvcs

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVS.W <label>                                   |-|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}
  //BNVC.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
  //JBNVC.W <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
  //JBVS.W <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_000-{offset}        [BVS.W <label>]
  //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)
  //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
  //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
  //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_sss_sss (s is not equal to 0)   [BVS.S <label>]
  //JBNVS.L <label>                                 |A|012346|-|---*-|-----|          |0110_100_100_000_110-0100111011111001-{address}      [BVS.S (*)+8;JMP <label>]
  //JBVC.L <label>                                  |A|012346|-|---*-|-----|          |0110_100_100_000_110-0100111011111001-{address}      [BVS.S (*)+8;JMP <label>]
  public static void irpBvssw () throws M68kException {
    if (XEiJ.MPU_CC_VS << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6900) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBvssw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_101_sss_sss
  //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
  //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
  //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_101_sss_sss [BVS.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVS.S <label>                                   |-|012346|-|---*-|-----|          |0110_100_110_sss_sss
  //BNVC.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
  //JBNVC.S <label>                                 |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
  //JBVS.S <label>                                  |A|012346|-|---*-|-----|          |0110_100_110_sss_sss [BVS.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BVS.S <label>                                   |-|01----|-|---*-|-----|          |0110_100_111_sss_sss
  //BNVC.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
  //JBNVC.S <label>                                 |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
  //JBVS.S <label>                                  |A|01----|-|---*-|-----|          |0110_100_111_sss_sss [BVS.S <label>]
  public static void irpBvss () throws M68kException {
    if (XEiJ.MPU_CC_VS << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBvss

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BPL.W <label>                                   |-|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}
  //BNMI.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
  //JBNMI.W <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
  //JBPL.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_000-{offset}        [BPL.W <label>]
  //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)
  //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
  //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
  //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_sss_sss (s is not equal to 0)   [BPL.S <label>]
  //JBMI.L <label>                                  |A|012346|-|-*---|-----|          |0110_101_000_000_110-0100111011111001-{address}      [BPL.S (*)+8;JMP <label>]
  //JBNPL.L <label>                                 |A|012346|-|-*---|-----|          |0110_101_000_000_110-0100111011111001-{address}      [BPL.S (*)+8;JMP <label>]
  public static void irpBplsw () throws M68kException {
    if (XEiJ.MPU_CC_PL << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6a00) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBplsw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_001_sss_sss
  //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
  //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
  //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_001_sss_sss [BPL.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BPL.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_010_sss_sss
  //BNMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
  //JBNMI.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
  //JBPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_010_sss_sss [BPL.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BPL.S <label>                                   |-|01----|-|-*---|-----|          |0110_101_011_sss_sss
  //BNMI.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
  //JBNMI.S <label>                                 |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
  //JBPL.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_011_sss_sss [BPL.S <label>]
  public static void irpBpls () throws M68kException {
    if (XEiJ.MPU_CC_PL << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBpls

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BMI.W <label>                                   |-|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}
  //BNPL.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
  //JBMI.W <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
  //JBNPL.W <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_000_000-{offset}        [BMI.W <label>]
  //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)
  //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
  //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
  //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_sss_sss (s is not equal to 0)   [BMI.S <label>]
  //JBNMI.L <label>                                 |A|012346|-|-*---|-----|          |0110_101_100_000_110-0100111011111001-{address}      [BMI.S (*)+8;JMP <label>]
  //JBPL.L <label>                                  |A|012346|-|-*---|-----|          |0110_101_100_000_110-0100111011111001-{address}      [BMI.S (*)+8;JMP <label>]
  public static void irpBmisw () throws M68kException {
    if (XEiJ.MPU_CC_MI << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6b00) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBmisw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_101_sss_sss
  //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
  //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
  //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_101_sss_sss [BMI.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BMI.S <label>                                   |-|012346|-|-*---|-----|          |0110_101_110_sss_sss
  //BNPL.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
  //JBMI.S <label>                                  |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
  //JBNPL.S <label>                                 |A|012346|-|-*---|-----|          |0110_101_110_sss_sss [BMI.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BMI.S <label>                                   |-|01----|-|-*---|-----|          |0110_101_111_sss_sss
  //BNPL.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
  //JBMI.S <label>                                  |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
  //JBNPL.S <label>                                 |A|01----|-|-*---|-----|          |0110_101_111_sss_sss [BMI.S <label>]
  public static void irpBmis () throws M68kException {
    if (XEiJ.MPU_CC_MI << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBmis

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGE.W <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}
  //BNLT.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
  //JBGE.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
  //JBNLT.W <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_000_000-{offset}        [BGE.W <label>]
  //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)
  //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
  //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
  //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_sss_sss (s is not equal to 0)   [BGE.S <label>]
  //JBLT.L <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_000_000_110-0100111011111001-{address}      [BGE.S (*)+8;JMP <label>]
  //JBNGE.L <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_000_000_110-0100111011111001-{address}      [BGE.S (*)+8;JMP <label>]
  public static void irpBgesw () throws M68kException {
    if (XEiJ.MPU_CC_GE << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6c00) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBgesw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_001_sss_sss
  //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
  //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
  //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_001_sss_sss [BGE.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGE.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_010_sss_sss
  //BNLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
  //JBGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
  //JBNLT.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_010_sss_sss [BGE.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGE.S <label>                                   |-|01----|-|-*-*-|-----|          |0110_110_011_sss_sss
  //BNLT.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
  //JBGE.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
  //JBNLT.S <label>                                 |A|01----|-|-*-*-|-----|          |0110_110_011_sss_sss [BGE.S <label>]
  public static void irpBges () throws M68kException {
    if (XEiJ.MPU_CC_GE << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBges

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLT.W <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}
  //BNGE.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
  //JBLT.W <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
  //JBNGE.W <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_000_000-{offset}        [BLT.W <label>]
  //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)
  //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
  //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
  //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_sss_sss (s is not equal to 0)   [BLT.S <label>]
  //JBGE.L <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_100_000_110-0100111011111001-{address}      [BLT.S (*)+8;JMP <label>]
  //JBNLT.L <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_100_000_110-0100111011111001-{address}      [BLT.S (*)+8;JMP <label>]
  public static void irpBltsw () throws M68kException {
    if (XEiJ.MPU_CC_LT << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6d00) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBltsw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_101_sss_sss
  //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
  //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
  //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_101_sss_sss [BLT.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLT.S <label>                                   |-|012346|-|-*-*-|-----|          |0110_110_110_sss_sss
  //BNGE.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
  //JBLT.S <label>                                  |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
  //JBNGE.S <label>                                 |A|012346|-|-*-*-|-----|          |0110_110_110_sss_sss [BLT.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLT.S <label>                                   |-|01----|-|-*-*-|-----|          |0110_110_111_sss_sss
  //BNGE.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
  //JBLT.S <label>                                  |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
  //JBNGE.S <label>                                 |A|01----|-|-*-*-|-----|          |0110_110_111_sss_sss [BLT.S <label>]
  public static void irpBlts () throws M68kException {
    if (XEiJ.MPU_CC_LT << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBlts

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGT.W <label>                                   |-|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}
  //BNLE.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
  //JBGT.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
  //JBNLE.W <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_000_000-{offset}        [BGT.W <label>]
  //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)
  //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
  //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
  //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_sss_sss (s is not equal to 0)   [BGT.S <label>]
  //JBLE.L <label>                                  |A|012346|-|-***-|-----|          |0110_111_000_000_110-0100111011111001-{address}      [BGT.S (*)+8;JMP <label>]
  //JBNGT.L <label>                                 |A|012346|-|-***-|-----|          |0110_111_000_000_110-0100111011111001-{address}      [BGT.S (*)+8;JMP <label>]
  public static void irpBgtsw () throws M68kException {
    if (XEiJ.MPU_CC_GT << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6e00) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBgtsw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_001_sss_sss
  //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
  //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
  //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_001_sss_sss [BGT.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGT.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_010_sss_sss
  //BNLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
  //JBGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
  //JBNLE.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_010_sss_sss [BGT.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BGT.S <label>                                   |-|01----|-|-***-|-----|          |0110_111_011_sss_sss
  //BNLE.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
  //JBGT.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
  //JBNLE.S <label>                                 |A|01----|-|-***-|-----|          |0110_111_011_sss_sss [BGT.S <label>]
  public static void irpBgts () throws M68kException {
    if (XEiJ.MPU_CC_GT << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBgts

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLE.W <label>                                   |-|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}
  //BNGT.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
  //JBLE.W <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
  //JBNGT.W <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_000_000-{offset}        [BLE.W <label>]
  //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)
  //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
  //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
  //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_sss_sss (s is not equal to 0)   [BLE.S <label>]
  //JBGT.L <label>                                  |A|012346|-|-***-|-----|          |0110_111_100_000_110-0100111011111001-{address}      [BLE.S (*)+8;JMP <label>]
  //JBNLE.L <label>                                 |A|012346|-|-***-|-----|          |0110_111_100_000_110-0100111011111001-{address}      [BLE.S (*)+8;JMP <label>]
  public static void irpBlesw () throws M68kException {
    if (XEiJ.MPU_CC_LE << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      if (s == 0) {  //Bcc.Wでジャンプ
        XEiJ.regPC = t + 2;
        s = XEiJ.busRwse (t);  //pcws
      } else {  //Bcc.Sでジャンプ
        //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
        //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
        if (XEiJ.MPU_OMIT_EXTRA_READ) {
          //! 軽量化。リードを省略する
        } else {
          XEiJ.busRwse (t);  //pcws
        }
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else if (XEiJ.regOC == 0x6f00) {  //Bcc.Wで通過
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_OMIT_OFFSET_READ) {
        //リードを省略する
      } else {
        XEiJ.busRws (XEiJ.regPC);
      }
      XEiJ.regPC += 2;
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBlesw

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_101_sss_sss
  //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
  //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
  //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_101_sss_sss [BLE.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLE.S <label>                                   |-|012346|-|-***-|-----|          |0110_111_110_sss_sss
  //BNGT.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
  //JBLE.S <label>                                  |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
  //JBNGT.S <label>                                 |A|012346|-|-***-|-----|          |0110_111_110_sss_sss [BLE.S <label>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //BLE.S <label>                                   |-|01----|-|-***-|-----|          |0110_111_111_sss_sss
  //BNGT.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
  //JBLE.S <label>                                  |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
  //JBNGT.S <label>                                 |A|01----|-|-***-|-----|          |0110_111_111_sss_sss [BLE.S <label>]
  public static void irpBles () throws M68kException {
    if (XEiJ.MPU_CC_LE << XEiJ.regCCR < 0) {  //Bccでジャンプ
      XEiJ.mpuCycleCount += 10;
      int t = XEiJ.regPC;  //pc0+2
      int s = (byte) XEiJ.regOC;  //オフセット
      //MC68000のBRA.S/BSR.S/Bcc.Sは分岐するとき分岐しない方の直後のワードをリードする
      //  2MB搭載機で$1FFFFEに無限ループ$60FE(BRA.S (*))を書いて飛び込むと$200000でバスエラーが出る
      if (XEiJ.MPU_OMIT_EXTRA_READ) {
        //! 軽量化。リードを省略する
      } else {
        XEiJ.busRwse (t);  //pcws
      }
      irpSetPC (t + s);  //pc0+2+オフセット
    } else {  //Bcc.Sで通過
      XEiJ.mpuCycleCount += 8;
    }
  }  //irpBles

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //IOCS <name>                                     |A|012346|-|UUUUU|UUUUU|          |0111_000_0dd_ddd_ddd-0100111001001111        [MOVEQ.L #<data>,D0;TRAP #15]
  //MOVEQ.L #<data>,Dq                              |-|012346|-|-UUUU|-**00|          |0111_qqq_0dd_ddd_ddd
  public static void irpMoveq () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int z;
    XEiJ.regRn[XEiJ.regOC >> 9 & 7] = z = (byte) XEiJ.regOC;
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMoveq

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MVS.B <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr (ISA_B)
  //
  //MVS.B <ea>,Dq
  //  バイトデータをロングに符号拡張してDqの全体を更新する
  public static void irpMvsByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z;
    XEiJ.regRn[XEiJ.regOC >> 9 & 7] = z = ea < XEiJ.EA_AR ? (byte) XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMvsByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MVS.W <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr (ISA_B)
  //
  //MVS.W <ea>,Dq
  //  ワードデータをロングに符号拡張してDqの全体を更新する
  public static void irpMvsWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z;
    XEiJ.regRn[XEiJ.regOC >> 9 & 7] = z = ea < XEiJ.EA_AR ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMvsWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MVZ.B <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr (ISA_B)
  //
  //MVZ.B <ea>,Dq
  //  バイトデータをロングにゼロ拡張してDqの全体を更新する
  public static void irpMvzByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z;
    XEiJ.regRn[XEiJ.regOC >> 9 & 7] = z = ea < XEiJ.EA_AR ? 0xff & XEiJ.regRn[ea] : XEiJ.busRbz (efaAnyByte (ea));
    XEiJ.regCCR = XEiJ.REG_CCR_X & XEiJ.regCCR | (z == 0 ? XEiJ.REG_CCR_Z : 0);
  }  //irpMvzByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MVZ.W <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr (ISA_B)
  //
  //MVZ.W <ea>,Dq
  //  ワードデータをロングにゼロ拡張してDqの全体を更新する
  public static void irpMvzWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z;
    XEiJ.regRn[XEiJ.regOC >> 9 & 7] = z = ea < XEiJ.EA_AR ? (char) XEiJ.regRn[ea] : XEiJ.busRwz (efaAnyWord (ea));
    XEiJ.regCCR = XEiJ.REG_CCR_X & XEiJ.regCCR | (z == 0 ? XEiJ.REG_CCR_Z : 0);
  }  //irpMvzWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //OR.B <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_000_mmm_rrr
  public static void irpOrToRegByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & (XEiJ.regRn[XEiJ.regOC >> 9 & 7] |= 255 & (ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea))))];  //ccr_tst_byte。0拡張してからOR
  }  //irpOrToRegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //OR.W <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_001_mmm_rrr
  public static void irpOrToRegWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z = (short) (XEiJ.regRn[XEiJ.regOC >> 9 & 7] |= ea < XEiJ.EA_AR ? (char) XEiJ.regRn[ea] : XEiJ.busRwz (efaAnyWord (ea)));  //0拡張してからOR
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpOrToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //OR.L <ea>,Dq                                    |-|012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_010_mmm_rrr
  public static void irpOrToRegLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int z;
    if (ea < XEiJ.EA_AR) {  //OR.L Dr,Dq
      XEiJ.mpuCycleCount += 8;
      XEiJ.regRn[qqq] = z = XEiJ.regRn[qqq] | XEiJ.regRn[ea];
    } else {  //OR.L <mem>,Dq
      XEiJ.mpuCycleCount += ea == XEiJ.EA_IM ? 8 : 6;  //ソースが#<data>のとき2増やす
      XEiJ.regRn[qqq] = z = XEiJ.regRn[qqq] | XEiJ.busRls (efaAnyLong (ea));
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpOrToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //DIVU.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_011_mmm_rrr
  //
  //DIVU.W <ea>,Dq
  //  M68000PRMでDIVU.Wのオーバーフローの条件が16bit符号あり整数と書かれているのは16bit符号なし整数の間違い
  public static void irpDivuWord () throws M68kException {
    //  X  変化しない
    //  N  ゼロ除算またはオーバーフローのとき不定。商が負のときセット。それ以外はクリア
    //  Z  ゼロ除算またはオーバーフローのとき不定。商が0のときセット。それ以外はクリア
    //  V  ゼロ除算のとき不定。オーバーフローのときセット。それ以外はクリア
    //  C  常にクリア
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int y = ea < XEiJ.EA_AR ? (char) XEiJ.regRn[ea] : XEiJ.busRwz (efaAnyWord (ea));  //除数
    int x = XEiJ.regRn[qqq];  //被除数
    XEiJ.mpuCycleCount += irpDivuCyclesModified (x, y);
    if (y == 0) {  //ゼロ除算
      //Dqは変化しない
      XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                     (x < 0 ? XEiJ.REG_CCR_N : 0) |  //Nは被除数が負のときセット、さもなくばクリア
                     (x >> 16 == 0 ? XEiJ.REG_CCR_Z : 0) |  //Zは被除数が$0000xxxxのときセット、さもなくばクリア
                     XEiJ.REG_CCR_V  //Vは常にセット
                     );  //Cは常にクリア
      M68kException.m6eNumber = M68kException.M6E_DIVIDE_BY_ZERO;
      throw M68kException.m6eSignal;
    }
    //無理にintで符号なし除算をやろうとするよりもdoubleにキャストしてから割ったほうが速い
    //  intの除算をdoubleの除算器で行うプロセッサならばなおさら
    //被除数を符号なし32ビットとみなすためlongを経由してdoubleに変換する
    //doubleからlongやintへのキャストは小数点以下が切り捨てられ、オーバーフローは表現できる絶対値最大の値になる
    //doubleから直接intに戻しているので0xffffffff/0x0001=0xffffffffが絶対値最大の0x7fffffffになってしまうが、
    //DIVU.Wではオーバーフローになることに変わりはないのでよいことにする
    //  符号なし32ビットの0xffffffffにしたいときは戻すときもlongを経由すればよい
    int z = (int) ((double) ((long) x & 0xffffffffL) / (double) y);  //商
    if (z >>> 16 != 0) {  //オーバーフローあり
      //Dqは変化しない
      XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                     (x < 0 ? XEiJ.REG_CCR_N : 0) |  //Nは被除数が負のときセット、さもなくばクリア
                     //Zは常にクリア
                     XEiJ.REG_CCR_V  //Vは常にセット
                     );  //Cは常にクリア
    } else {  //オーバーフローなし
      XEiJ.regRn[qqq] = x - y * z << 16 | z;  //余り<<16|商
      z = (short) z;
      XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                     (z < 0 ? XEiJ.REG_CCR_N : 0) |  //Nは商が負のときセット、さもなくばクリア
                     (z == 0 ? XEiJ.REG_CCR_Z : 0)  //Zは商が0のときセット、さもなくばクリア
                     //Vは常にクリア
                     );  //Cは常にクリア
    }  //if オーバーフローあり/オーバーフローなし
  }  //irpDivuWord

  //DIVUの実行時間
  //  以下に実効アドレスの時間を加える
  //    ゼロ除算のとき38
  //    オーバーフローのとき10
  //    正常終了のとき76+
  //      商のビット15～1について
  //        被除数のビット16が1で商が1のとき0
  //        被除数のビット16が0で商が1のとき2
  //        被除数のビット16が0で商が0のとき4
  //  補足
  //    商のビット0を計算に含めると最大140になりマニュアルと一致する
  //  参考
  //    https://www.atari-forum.com/viewtopic.php?t=6484
  public static int irpDivuCyclesModified (int x, int y) {
    y &= 0xffff;  //ゼロ拡張
    if (y == 0) {  //ゼロ除算
      return 38;
    }
    int r = x >>> 16;  //余り。符号なし右シフト
    if (y <= r) {  //オーバーフロー
      return 10;
    }
    int c = 76;
    for (int i = 15; 0 < i; i--) {  //ビット0を含まない
      r = r << 1 | ((x >> i) & 1);
      if (0x10000 <= r) {  //被除数のビット16が1で商が1
        r -= y;
      } else if (y <= r) {  //被除数のビット16が0で商が1
        r -= y;
        c += 2;
      } else {  //被除数のビット16が0で商が0
        c += 4;
      }
    }
    return c;
  }

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SBCD.B Dr,Dq                                    |-|012346|-|UUUUU|*U*U*|          |1000_qqq_100_000_rrr
  //SBCD.B -(Ar),-(Aq)                              |-|012346|-|UUUUU|*U*U*|          |1000_qqq_100_001_rrr
  //OR.B Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_100_mmm_rrr
  public static void irpOrToMemByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >= XEiJ.EA_MM) {  //OR.B Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      int z = XEiJ.regRn[XEiJ.regOC >> 9 & 7] | XEiJ.busRbs (a);
      XEiJ.busWb (a, z);
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    } else if (ea < XEiJ.EA_AR) {  //SBCD.B Dr,Dq
      int qqq = XEiJ.regOC >> 9 & 7;
      XEiJ.mpuCycleCount += 6;
      int x;
      XEiJ.regRn[qqq] = ~0xff & (x = XEiJ.regRn[qqq]) | irpSbcd (x, XEiJ.regRn[ea]);
    } else {  //SBCD.B -(Ar),-(Aq)
      XEiJ.mpuCycleCount += 18;
      int y = XEiJ.busRbz (--XEiJ.regRn[ea]);  //このr[ea]はアドレスレジスタ
      int a = --XEiJ.regRn[(XEiJ.regOC >> 9) - (64 - 8)];
      XEiJ.busWb (a, irpSbcd (XEiJ.busRbz (a), y));
    }
  }  //irpOrToMemByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //OR.W Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_101_mmm_rrr
  public static void irpOrToMemWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int z = XEiJ.regRn[XEiJ.regOC >> 9 & 7] | XEiJ.busRws (a);
    XEiJ.busWw (a, z);
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpOrToMemWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //OR.L Dq,<ea>                                    |-|012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_110_mmm_rrr
  public static void irpOrToMemLong () throws M68kException {
    XEiJ.mpuCycleCount += 12;
    int ea = XEiJ.regOC & 63;
    int a = efaMltLong (ea);
    int z;
    XEiJ.busWl (a, z = XEiJ.regRn[XEiJ.regOC >> 9 & 7] | XEiJ.busRls (a));
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpOrToMemLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //DIVS.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_111_mmm_rrr
  //
  //DIVS.W <ea>,Dq
  //  DIVSの余りの符号は被除数と一致
  //  M68000PRMでDIVS.Wのアドレッシングモードがデータ可変と書かれているのはデータの間違い
  public static void irpDivsWord () throws M68kException {
    //  X  変化しない
    //  N  ゼロ除算またはオーバーフローのとき不定。商が負のときセット。それ以外はクリア
    //  Z  ゼロ除算またはオーバーフローのとき不定。商が0のときセット。それ以外はクリア
    //  V  ゼロ除算のとき不定。オーバーフローのときセット。それ以外はクリア
    //  C  常にクリア
    //divsの余りの符号は被除数と一致
    //Javaの除算演算子の挙動
    //   10 /  3 ==  3   10 %  3 ==  1   10 =  3 *  3 +  1
    //   10 / -3 == -3   10 % -3 ==  1   10 = -3 * -3 +  1
    //  -10 /  3 == -3  -10 %  3 == -1  -10 =  3 * -3 + -1
    //  -10 / -3 ==  3  -10 % -3 == -1  -10 = -3 *  3 + -1
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int y = ea < XEiJ.EA_AR ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //除数
    int x = XEiJ.regRn[qqq];  //被除数
    XEiJ.mpuCycleCount += irpDivsCycles (x, y);
    if (y == 0) {  //ゼロ除算
      //Dqは変化しない
      //!!! MC68030はゼロ除算のときオペランド以外の要因でZとVが変化する。その要因がわからないとZとVを正確に再現することができない
      XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                     //Nは常にクリア
                     XEiJ.REG_CCR_Z |  //Zは常にセット
                     (x == 0x00008000 ? ~XEiJ.regCCR & XEiJ.REG_CCR_V : (0 <= x && x != 0x7fffffff) || x == 0x80000000 ? XEiJ.REG_CCR_V : XEiJ.regCCR & XEiJ.REG_CCR_V)  //Vは被除数が$00008000のとき反転、被除数が$7fffffffを除く正または$80000000のときセット、さもなくば変化しない
                     );  //Cは常にクリア
      M68kException.m6eNumber = M68kException.M6E_DIVIDE_BY_ZERO;
      throw M68kException.m6eSignal;
    }
    int z = x / y;  //商
    if ((short) z != z) {  //オーバーフローあり
      //Dqは変化しない
      XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                     (x == 0x80000000 || (z & 0xffff0080) == 0x00000080 || (z & 0xffff0080) == 0xffff0080 ? XEiJ.REG_CCR_N : 0) |  //Nは被除数が$80000000または商が$0000xxyyまたは$ffffxxyyでyyが負のときセット、さもなくばクリア
                     (z == 0x00008000 || (((z & 0xffff00ff) == 0x00000000 || (z & 0xffff00ff) == 0xffff0000) && (z & 0x0000ff00) != 0) ? XEiJ.REG_CCR_Z : 0) |  //Zは商が$00008000または商が$0000xxyyまたは$ffffxxyyでxxが0でなくてyyが0のときセット、さもなくばクリア
                     XEiJ.REG_CCR_V  //Vは常にセット
                     );  //Cは常にクリア
    } else {  //オーバーフローなし
      XEiJ.regRn[qqq] = x - y * z << 16 | (char) z;  //Dqは余り<<16|商&$ffff
      XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                     (z < 0 ? XEiJ.REG_CCR_N : 0) |  //Nは商が負のときセット、さもなくばクリア
                     (z == 0 ? XEiJ.REG_CCR_Z : 0)  //Zは商が0のときセット、さもなくばクリア
                     //Vは常にクリア
                     );  //Cは常にクリア
    }  //if オーバーフローあり/オーバーフローなし
  }  //irpDivsWord

  //DIVSの実行時間
  //  以下に実効アドレスの時間を加える
  //    ゼロ除算のとき38
  //    符号なしオーバーフローのとき
  //      被除数が正のとき16
  //      被除数が負のとき18
  //    正常終了または符号ありオーバーフローのとき
  //      被除数が正で除数が正のとき120+
  //      被除数が正で除数が負のとき122+
  //      被除数が負で除数が正のとき126+
  //      被除数が負で除数が負のとき124+
  //        符号なし商のビット15～1について
  //          符号なし商が1のとき0
  //          符号なし商が0のとき2
  //  補足
  //    符号なし商のビット0を計算に含めると最大158になりマニュアルと一致する
  //  参考
  //    https://www.atari-forum.com/viewtopic.php?t=6484
  public static int irpDivsCycles (int x, int y) {
    y = (short) y;  //符号拡張
    if (y == 0) {  //ゼロ除算
      return 38;
    }
    //符号あり除算だと0x80000000/0xffffffffが0x00000000になる環境があるので
    //符号なし除算を用いる。JavaはInteger.divideUnsigned
    //符号なし商に0x80000000が含まれることに注意
    int q = Integer.divideUnsigned ((x < 0 ? -x : x), (y < 0 ? -y : y));
    if ((q & 0xffff0000) != 0) {  //符号なしオーバーフロー。0xffff<qは不可
      return x < 0 ? 18 : 16;
    }
    int t = ~q;
    t = (t & 0x5554) + ((t >> 1) & 0x5555);  //0x5554に注意。ビット0を含まない
    t = (t & 0x3333) + ((t >> 2) & 0x3333);
    t = (t & 0x0F0F) + ((t >> 4) & 0x0F0F);
    t = (t & 0x00FF) + ((t >> 8) & 0x00FF);
    return (x < 0 ? y < 0 ? 124 : 126 : y < 0 ? 122 : 120) + (t << 1);
  }

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUB.B <ea>,Dq                                   |-|012346|-|UUUUU|*****|D M+-WXZPI|1001_qqq_000_mmm_rrr
  public static void irpSubToRegByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int x, y, z;
    y = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));
    x = XEiJ.regRn[qqq];
    z = x - y;
    XEiJ.regRn[qqq] = ~255 & x | 255 & z;
    XEiJ.regCCR = (XEiJ.MPU_TSTB_TABLE[255 & z] |
           ((x ^ y) & (x ^ z)) >> 6 & XEiJ.REG_CCR_V |
           (byte) (x & (y ^ z) ^ (y | z)) >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub_byte
  }  //irpSubToRegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUB.W <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_001_mmm_rrr
  public static void irpSubToRegWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int x, y, z;
    y = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    x = XEiJ.regRn[qqq];
    z = x - y;
    XEiJ.regRn[qqq] = ~65535 & x | (char) z;
    XEiJ.regCCR = (z >> 12 & XEiJ.REG_CCR_N | (char) z - 1 >> 14 & XEiJ.REG_CCR_Z |
           ((x ^ y) & (x ^ z)) >> 14 & XEiJ.REG_CCR_V |
           (short) (x & (y ^ z) ^ (y | z)) >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub_word
  }  //irpSubToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUB.L <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_010_mmm_rrr
  public static void irpSubToRegLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    XEiJ.mpuCycleCount += ea < XEiJ.EA_MM || ea == XEiJ.EA_IM ? 8 : 6;  //ソースが#<data>のとき2増やす
    int x, y, z;
    y = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    x = XEiJ.regRn[qqq];
    z = x - y;
    XEiJ.regRn[qqq] = z;
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >> 30 & XEiJ.REG_CCR_V |
           (x & (y ^ z) ^ (y | z)) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub
  }  //irpSubToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBA.W <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr
  //SUB.W <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr [SUBA.W <ea>,Aq]
  //CLR.W Ar                                        |A|012346|-|-----|-----| A        |1001_rrr_011_001_rrr [SUBA.W Ar,Ar]
  //
  //SUBA.W <ea>,Aq
  //  ソースを符号拡張してロングで減算する
  public static void irpSubaWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= z;  //r[op >> 9 & 15] -= ea < XEiJ.EA_MM ? (short) r[ea] : rws (efaAnyWord (ea));は不可
    //ccrは変化しない
  }  //irpSubaWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBX.B Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_100_000_rrr
  //SUBX.B -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_100_001_rrr
  //SUB.B Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_100_mmm_rrr
  public static void irpSubToMemByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int a, x, y, z;
    if (ea < XEiJ.EA_MM) {
      if (ea < XEiJ.EA_AR) {  //SUBX.B Dr,Dq
        int qqq = XEiJ.regOC >> 9 & 7;
        XEiJ.mpuCycleCount += 4;
        y = XEiJ.regRn[ea];
        x = XEiJ.regRn[qqq];
        z = x - y - (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.regRn[qqq] = ~255 & x | 255 & z;
      } else {  //SUBX.B -(Ar),-(Aq)
        XEiJ.mpuCycleCount += 18;
        y = XEiJ.busRbs (--XEiJ.regRn[ea]);  //このr[ea]はアドレスレジスタ
        a = --XEiJ.regRn[XEiJ.regOC >> 9 & 15];  //1qqq=aqq
        x = XEiJ.busRbs (a);
        z = x - y - (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.busWb (a, z);
      }
      XEiJ.regCCR = (z >> 4 & XEiJ.REG_CCR_N | (255 & z) - 1 >> 6 & XEiJ.regCCR & XEiJ.REG_CCR_Z |  //SUBXはZをクリアすることはあるがセットすることはない
             ((x ^ y) & (x ^ z)) >> 6 & XEiJ.REG_CCR_V |
             (byte) (x & (y ^ z) ^ (y | z)) >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_subx_byte
    } else {  //SUB.B Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7];
      a = efaMltByte (ea);
      x = XEiJ.busRbs (a);
      z = x - y;
      XEiJ.busWb (a, z);
      XEiJ.regCCR = (XEiJ.MPU_TSTB_TABLE[255 & z] |
             ((x ^ y) & (x ^ z)) >> 6 & XEiJ.REG_CCR_V |
             (byte) (x & (y ^ z) ^ (y | z)) >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub_byte
    }
  }  //irpSubToMemByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBX.W Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_101_000_rrr
  //SUBX.W -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_101_001_rrr
  //SUB.W Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_101_mmm_rrr
  public static void irpSubToMemWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int a, x, y, z;
    if (ea < XEiJ.EA_MM) {
      if (ea < XEiJ.EA_AR) {  //SUBX.W Dr,Dq
        int qqq = XEiJ.regOC >> 9 & 7;
        XEiJ.mpuCycleCount += 4;
        y = XEiJ.regRn[ea];
        x = XEiJ.regRn[qqq];
        z = x - y - (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.regRn[qqq] = ~65535 & x | (char) z;
      } else {  //SUBX.W -(Ar),-(Aq)
        XEiJ.mpuCycleCount += 18;
        y = XEiJ.busRws (XEiJ.regRn[ea] -= 2);  //このr[ea]はアドレスレジスタ
        a = XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= 2;
        x = XEiJ.busRws (a);
        z = x - y - (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.busWw (a, z);
      }
      XEiJ.regCCR = (z >> 12 & XEiJ.REG_CCR_N | (char) z - 1 >> 14 & XEiJ.regCCR & XEiJ.REG_CCR_Z |  //ADDXはZをクリアすることはあるがセットすることはない
             ((x ^ y) & (x ^ z)) >> 14 & XEiJ.REG_CCR_V |
             (short) (x & (y ^ z) ^ (y | z)) >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_subx_word
    } else {  //SUB.W Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      y = (short) XEiJ.regRn[XEiJ.regOC >> 9 & 7];
      a = efaMltWord (ea);
      x = XEiJ.busRws (a);
      z = x - y;
      XEiJ.busWw (a, z);
      XEiJ.regCCR = (z >> 12 & XEiJ.REG_CCR_N | (char) z - 1 >> 14 & XEiJ.REG_CCR_Z |
             ((x ^ y) & (x ^ z)) >> 14 & XEiJ.REG_CCR_V |
             (short) (x & (y ^ z) ^ (y | z)) >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub_word
    }
  }  //irpSubToMemWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBX.L Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1001_qqq_110_000_rrr
  //SUBX.L -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1001_qqq_110_001_rrr
  //SUB.L Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_110_mmm_rrr
  public static void irpSubToMemLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_MM) {
      int x;
      int y;
      int z;
      if (ea < XEiJ.EA_AR) {  //SUBX.L Dr,Dq
        int qqq = XEiJ.regOC >> 9 & 7;
        XEiJ.mpuCycleCount += 8;
        XEiJ.regRn[qqq] = z = (x = XEiJ.regRn[qqq]) - (y = XEiJ.regRn[ea]) - (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
      } else {  //SUBX.L -(Ar),-(Aq)
        XEiJ.mpuCycleCount += 30;
        y = XEiJ.busRls (XEiJ.regRn[ea] -= 4);  //このr[ea]はアドレスレジスタ
        int a = XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= 4;
        XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) - y - (XEiJ.regCCR >> 4));  //Xの左側はすべて0なのでCCR_X&を省略
      }
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_Z : 0) |
             ((x ^ y) & (x ^ z)) >>> 31 << 1 |
             (x & (y ^ z) ^ (y | z)) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_subx
    } else {  //SUB.L Dq,<ea>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltLong (ea);
      int x;
      int y;
      int z;
      XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) - (y = XEiJ.regRn[XEiJ.regOC >> 9 & 7]));
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
             ((x ^ y) & (x ^ z)) >>> 31 << 1 |
             (x & (y ^ z) ^ (y | z)) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_sub
    }
  }  //irpSubToMemLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SUBA.L <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr
  //SUB.L <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr [SUBA.L <ea>,Aq]
  //CLR.L Ar                                        |A|012346|-|-----|-----| A        |1001_rrr_111_001_rrr [SUBA.L Ar,Ar]
  public static void irpSubaLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    XEiJ.mpuCycleCount += ea < XEiJ.EA_MM || ea == XEiJ.EA_IM ? 8 : 6;  //Dr/Ar/#<data>のとき8+、それ以外は6+
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= z;  //r[op >> 9 & 15] -= ea < XEiJ.EA_MM ? r[ea] : rls (efaAnyLong (ea));は不可
    //ccrは変化しない
  }  //irpSubaLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //SXCALL <name>                                   |A|012346|-|UUUUU|*****|          |1010_0dd_ddd_ddd_ddd [ALINE #<data>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ALINE #<data>                                   |-|012346|-|UUUUU|*****|          |1010_ddd_ddd_ddd_ddd (line 1010 emulator)
  public static void irpAline () throws M68kException {
    XEiJ.mpuCycleCount += 34;
    if (XEiJ.MPU_INLINE_EXCEPTION) {
      int save_sr = XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
      int sp = XEiJ.regRn[15];
      XEiJ.regSRT1 = XEiJ.mpuTraceFlag = 0;  //srのTビットを消す
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
        XEiJ.mpuUSP = sp;  //USPを保存
        sp = XEiJ.mpuISP;  //SSPを復元
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
        } else {
          XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
        }
        if (InstructionBreakPoint.IBP_ON) {
          InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
        }
      }
      int vectorOffset = M68kException.M6E_LINE_1010_EMULATOR << 2;  //vector offset
      XEiJ.regRn[15] = sp -= 8;  //short format
      XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
      XEiJ.busWl (sp + 2, XEiJ.regPC0);  //program counter
      XEiJ.busWw (sp, save_sr);  //status register
      irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
    } else {
      irpException (M68kException.M6E_LINE_1010_EMULATOR, XEiJ.regPC0, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //pcは命令の先頭
    }
  }  //irpAline

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMP.B <ea>,Dq                                   |-|012346|-|-UUUU|-****|D M+-WXZPI|1011_qqq_000_mmm_rrr
  public static void irpCmpByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    int z = (byte) ((x = (byte) XEiJ.regRn[XEiJ.regOC >> 9 & 7]) - (y = ea < XEiJ.EA_AR ? (byte) XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea))));
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMP.W <ea>,Dq                                   |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_001_mmm_rrr
  public static void irpCmpWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    int z = (short) ((x = (short) XEiJ.regRn[XEiJ.regOC >> 9 & 7]) - (y = ea < XEiJ.EA_MM ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea))));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMP.L <ea>,Dq                                   |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_010_mmm_rrr
  public static void irpCmpLong () throws M68kException {
    XEiJ.mpuCycleCount += 6;
    int ea = XEiJ.regOC & 63;
    int x;
    int y;
    int z = (x = XEiJ.regRn[XEiJ.regOC >> 9 & 7]) - (y = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea)));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMPA.W <ea>,Aq                                  |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr
  //CMP.W <ea>,Aq                                   |A|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr [CMPA.W <ea>,Aq]
  //
  //CMPA.W <ea>,Aq
  //  ソースを符号拡張してロングで比較する
  public static void irpCmpaWord () throws M68kException {
    XEiJ.mpuCycleCount += 6;
    int ea = XEiJ.regOC & 63;
    //ソースを符号拡張してからロングで比較する
    int y = ea < XEiJ.EA_MM ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    int x;
    int z = (x = XEiJ.regRn[XEiJ.regOC >> 9 & 15]) - y;
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpaWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EOR.B Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_100_mmm_rrr
  //CMPM.B (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_100_001_rrr
  public static void irpEorByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //CMPM.B (Ar)+,(Aq)+
      XEiJ.mpuCycleCount += 12;
      int y = XEiJ.busRbs (XEiJ.regRn[ea]++);  //このr[ea]はアドレスレジスタ
      int x;
      int z = (byte) ((x = XEiJ.busRbs (XEiJ.regRn[XEiJ.regOC >> 9 & 15]++)) - y);
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
             ((x ^ y) & (x ^ z)) >>> 31 << 1 |
             (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
    } else {
      int qqq = XEiJ.regOC >> 9 & 7;
      int z;
      if (ea < XEiJ.EA_AR) {  //EOR.B Dq,Dr
        XEiJ.mpuCycleCount += 4;
        z = XEiJ.regRn[ea] ^= 255 & XEiJ.regRn[qqq];  //0拡張してからEOR
      } else {  //EOR.B Dq,<mem>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltByte (ea);
        XEiJ.busWb (a, z = XEiJ.regRn[qqq] ^ XEiJ.busRbs (a));
      }
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    }
  }  //irpEorByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EOR.W Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_101_mmm_rrr
  //CMPM.W (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_101_001_rrr
  public static void irpEorWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int rrr = XEiJ.regOC & 7;
    int mmm = ea >> 3;
    if (mmm == XEiJ.MMM_AR) {  //CMPM.W (Ar)+,(Aq)+
      XEiJ.mpuCycleCount += 12;
      int y = XEiJ.busRws ((XEiJ.regRn[ea] += 2) - 2);  //このr[ea]はアドレスレジスタ
      int x;
      int z = (short) ((x = XEiJ.busRws ((XEiJ.regRn[XEiJ.regOC >> 9 & 15] += 2) - 2)) - y);
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
             ((x ^ y) & (x ^ z)) >>> 31 << 1 |
             (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
    } else {
      int qqq = XEiJ.regOC >> 9 & 7;
      int z;
      if (ea < XEiJ.EA_AR) {  //EOR.W Dq,Dr
        XEiJ.mpuCycleCount += 4;
        z = XEiJ.regRn[rrr] ^= (char) XEiJ.regRn[qqq];  //0拡張してからEOR
      } else {  //EOR.W Dq,<mem>
        XEiJ.mpuCycleCount += 8;
        int a = efaMltWord (ea);
        XEiJ.busWw (a, z = XEiJ.regRn[qqq] ^ XEiJ.busRws (a));
      }
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    }
  }  //irpEorWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EOR.L Dq,<ea>                                   |-|012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_110_mmm_rrr
  //CMPM.L (Ar)+,(Aq)+                              |-|012346|-|-UUUU|-****|          |1011_qqq_110_001_rrr
  public static void irpEorLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >> 3 == XEiJ.MMM_AR) {  //CMPM.L (Ar)+,(Aq)+
      XEiJ.mpuCycleCount += 20;
      int y = XEiJ.busRls ((XEiJ.regRn[ea] += 4) - 4);  //このr[ea]はアドレスレジスタ
      int x;
      int z = (x = XEiJ.busRls ((XEiJ.regRn[XEiJ.regOC >> 9 & 15] += 4) - 4)) - y;
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
             ((x ^ y) & (x ^ z)) >>> 31 << 1 |
             (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
    } else {
      int qqq = XEiJ.regOC >> 9 & 7;
      int z;
      if (ea < XEiJ.EA_AR) {  //EOR.L Dq,Dr
        XEiJ.mpuCycleCount += 8;
        XEiJ.regRn[ea] = z = XEiJ.regRn[ea] ^ XEiJ.regRn[qqq];
      } else {  //EOR.L Dq,<mem>
        XEiJ.mpuCycleCount += 12;
        int a = efaMltLong (ea);
        XEiJ.busWl (a, z = XEiJ.busRls (a) ^ XEiJ.regRn[qqq]);
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    }
  }  //irpEorLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //CMPA.L <ea>,Aq                                  |-|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr
  //CMP.L <ea>,Aq                                   |A|012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr [CMPA.L <ea>,Aq]
  public static void irpCmpaLong () throws M68kException {
    XEiJ.mpuCycleCount += 6;
    int ea = XEiJ.regOC & 63;
    int y = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    int x;
    int z = (x = XEiJ.regRn[XEiJ.regOC >> 9 & 15]) - y;
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);  //ccr_cmp
  }  //irpCmpaLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //AND.B <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_000_mmm_rrr
  public static void irpAndToRegByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & (XEiJ.regRn[XEiJ.regOC >> 9 & 7] &= ~255 | (ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea))))];  //ccr_tst_byte。1拡張してからAND
  }  //irpAndToRegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //AND.W <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_001_mmm_rrr
  public static void irpAndToRegWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int z = XEiJ.regRn[XEiJ.regOC >> 9 & 7] &= ~65535 | (ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea)));  //1拡張してからAND
    XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
  }  //irpAndToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //AND.L <ea>,Dq                                   |-|012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_010_mmm_rrr
  public static void irpAndToRegLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int z;
    if (ea < XEiJ.EA_AR) {  //AND.L Dr,Dq
      XEiJ.mpuCycleCount += 8;
      z = XEiJ.regRn[qqq] &= XEiJ.regRn[ea];
    } else {  //AND.L <mem>,Dq
      XEiJ.mpuCycleCount += ea == XEiJ.EA_IM ? 8 : 6;  //ソースが#<data>のとき2増やす
      z = XEiJ.regRn[qqq] &= XEiJ.busRls (efaAnyLong (ea));
    }
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpAndToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MULU.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_011_mmm_rrr
  public static void irpMuluWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int y = ea < XEiJ.EA_AR ? (char) XEiJ.regRn[ea] : XEiJ.busRwz (efaAnyWord (ea));
    //muluの所要サイクル数は38+2n
    //nはソースに含まれる1の数
    int s = y & 0x5555;
    s += y - s >> 1;
    int t = s & 0x3333;
    t += s - t >> 2;
    t += t >> 4;
    XEiJ.mpuCycleCount += 38 + (((t & 15) + (t >> 8 & 15)) << 1);  //38+2n
    //XEiJ.mpuCycleCount += 38 + (Integer.bitCount (y) << 1);  //少し遅くなる
    int z;
    XEiJ.regRn[qqq] = z = (char) XEiJ.regRn[qqq] * y;  //積の下位32ビット。オーバーフローは無視
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMuluWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ABCD.B Dr,Dq                                    |-|012346|-|UUUUU|*U*U*|          |1100_qqq_100_000_rrr
  //ABCD.B -(Ar),-(Aq)                              |-|012346|-|UUUUU|*U*U*|          |1100_qqq_100_001_rrr
  //AND.B Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_100_mmm_rrr
  public static void irpAndToMemByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea >= XEiJ.EA_MM) {  //AND.B Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltByte (ea);
      int z = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & XEiJ.busRbs (a);
      XEiJ.busWb (a, z);
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.MPU_TSTB_TABLE[255 & z];  //ccr_tst_byte
    } else if (ea < XEiJ.EA_AR) {  //ABCD.B Dr,Dq
      int qqq = XEiJ.regOC >> 9 & 7;
      XEiJ.mpuCycleCount += 6;
      XEiJ.regRn[qqq] = ~0xff & XEiJ.regRn[qqq] | irpAbcd (XEiJ.regRn[qqq], XEiJ.regRn[ea]);
    } else {  //ABCD.B -(Ar),-(Aq)
      XEiJ.mpuCycleCount += 18;
      int y = XEiJ.busRbz (--XEiJ.regRn[ea]);  //このr[ea]はアドレスレジスタ
      int a = --XEiJ.regRn[(XEiJ.regOC >> 9) - (96 - 8)];
      XEiJ.busWb (a, irpAbcd (XEiJ.busRbz (a), y));
    }
  }  //irpAndToMemByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EXG.L Dq,Dr                                     |-|012346|-|-----|-----|          |1100_qqq_101_000_rrr
  //EXG.L Aq,Ar                                     |-|012346|-|-----|-----|          |1100_qqq_101_001_rrr
  //AND.W Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_101_mmm_rrr
  public static void irpAndToMemWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_MM) {  //EXG
      XEiJ.mpuCycleCount += 6;
      if (ea < XEiJ.EA_AR) {  //EXG.L Dq,Dr
        int qqq = XEiJ.regOC >> 9 & 7;
        int t = XEiJ.regRn[qqq];
        XEiJ.regRn[qqq] = XEiJ.regRn[ea];
        XEiJ.regRn[ea] = t;
      } else {  //EXG.L Aq,Ar
        int aqq = (XEiJ.regOC >> 9) - (96 - 8);
        int t = XEiJ.regRn[aqq];
        XEiJ.regRn[aqq] = XEiJ.regRn[ea];  //このr[ea]アドレスレジスタ
        XEiJ.regRn[ea] = t;  //このr[ea]はアドレスレジスタ
      }
    } else {  //AND.W Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      int a = efaMltWord (ea);
      int z = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & XEiJ.busRws (a);
      XEiJ.busWw (a, z);
      XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (char) z - 1 >> 31 & XEiJ.REG_CCR_Z | ((short) z < 0 ? XEiJ.REG_CCR_N : 0);  //ccr_tst_word
    }
  }  //irpAndToMemWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //EXG.L Dq,Ar                                     |-|012346|-|-----|-----|          |1100_qqq_110_001_rrr
  //AND.L Dq,<ea>                                   |-|012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_110_mmm_rrr
  public static void irpAndToMemLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    if (ea >> 3 == XEiJ.MMM_AR) {  //EXG.L Dq,Ar
      XEiJ.mpuCycleCount += 6;
      int t = XEiJ.regRn[qqq];
      XEiJ.regRn[qqq] = XEiJ.regRn[ea];  //このr[ea]はアドレスレジスタ
      XEiJ.regRn[ea] = t;  //このr[ea]はアドレスレジスタ
    } else {  //AND.L Dq,<ea>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltLong (ea);
      int z;
      XEiJ.busWl (a, z = XEiJ.busRls (a) & XEiJ.regRn[qqq]);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
    }
  }  //irpAndToMemLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //MULS.W <ea>,Dq                                  |-|012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_111_mmm_rrr
  public static void irpMulsWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int y = ea < XEiJ.EA_AR ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));
    int t = y << 1 ^ y;  //右側が1である0と右側が0または末尾である1は1、それ以外は0。ソースは符号拡張されているので上位16ビットはすべて0
    //mulsの所要サイクル数は38+2n
    //nはソースの末尾に0を付け加えた17ビットに含まれる10または01の数
    int s = t & 0x5555;
    s += t - s >> 1;
    t = s & 0x3333;
    t += s - t >> 2;
    t += t >> 4;
    XEiJ.mpuCycleCount += 38 + (((t & 15) + (t >> 8 & 15)) << 1);  //38+2n
    int z;
    XEiJ.regRn[qqq] = z = (short) XEiJ.regRn[qqq] * y;  //積の下位32ビット。オーバーフローは無視
    XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X);  //ccr_tst
  }  //irpMulsWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADD.B <ea>,Dq                                   |-|012346|-|UUUUU|*****|D M+-WXZPI|1101_qqq_000_mmm_rrr
  public static void irpAddToRegByte () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int x, y, z;
    y = ea < XEiJ.EA_AR ? XEiJ.regRn[ea] : XEiJ.busRbs (efaAnyByte (ea));
    x = XEiJ.regRn[qqq];
    z = x + y;
    XEiJ.regRn[qqq] = ~255 & x | 255 & z;
    XEiJ.regCCR = (XEiJ.MPU_TSTB_TABLE[255 & z] |
           ((x ^ z) & (y ^ z)) >> 6 & XEiJ.REG_CCR_V |
           (byte) ((x | y) ^ (x ^ y) & z) >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add_byte
  }  //irpAddToRegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADD.W <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_001_mmm_rrr
  public static void irpAddToRegWord () throws M68kException {
    XEiJ.mpuCycleCount += 4;
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    int x, y, z;
    y = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    x = XEiJ.regRn[qqq];
    z = x + y;
    XEiJ.regRn[qqq] = ~65535 & x | (char) z;
    XEiJ.regCCR = (z >> 12 & XEiJ.REG_CCR_N | (char) z - 1 >> 14 & XEiJ.REG_CCR_Z |
           ((x ^ z) & (y ^ z)) >> 14 & XEiJ.REG_CCR_V |
           (short) ((x | y) ^ (x ^ y) & z) >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add_word
  }  //irpAddToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADD.L <ea>,Dq                                   |-|012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_010_mmm_rrr
  public static void irpAddToRegLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int qqq = XEiJ.regOC >> 9 & 7;
    XEiJ.mpuCycleCount += ea < XEiJ.EA_MM || ea == XEiJ.EA_IM ? 8 : 6;  //ソースが#<data>のとき2増やす
    int x, y, z;
    y = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ
    x = XEiJ.regRn[qqq];
    z = x + y;
    XEiJ.regRn[qqq] = z;
    XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >> 30 & XEiJ.REG_CCR_V |
           ((x | y) ^ (x ^ y) & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add
  }  //irpAddToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDA.W <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr
  //ADD.W <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr [ADDA.W <ea>,Aq]
  //
  //ADDA.W <ea>,Aq
  //  ソースを符号拡張してロングで加算する
  public static void irpAddaWord () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int z = ea < XEiJ.EA_MM ? (short) XEiJ.regRn[ea] : XEiJ.busRws (efaAnyWord (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.regRn[XEiJ.regOC >> 9 & 15] += z;  //r[op >> 9 & 15] += ea < XEiJ.EA_MM ? (short) r[ea] : rws (efaAnyWord (ea));は不可
    //ccrは変化しない
  }  //irpAddaWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDX.B Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_100_000_rrr
  //ADDX.B -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_100_001_rrr
  //ADD.B Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_100_mmm_rrr
  public static void irpAddToMemByte () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int a, x, y, z;
    if (ea < XEiJ.EA_MM) {
      if (ea < XEiJ.EA_AR) {  //ADDX.B Dr,Dq
        int qqq = XEiJ.regOC >> 9 & 7;
        XEiJ.mpuCycleCount += 4;
        y = XEiJ.regRn[ea];
        x = XEiJ.regRn[qqq];
        z = x + y + (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.regRn[qqq] = ~255 & x | 255 & z;
      } else {  //ADDX.B -(Ar),-(Aq)
        XEiJ.mpuCycleCount += 18;
        y = XEiJ.busRbs (--XEiJ.regRn[ea]);  //このr[ea]はアドレスレジスタ
        a = --XEiJ.regRn[XEiJ.regOC >> 9 & 15];  //1qqq=aqq
        x = XEiJ.busRbs (a);
        z = x + y + (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.busWb (a, z);
      }
      XEiJ.regCCR = (z >> 4 & XEiJ.REG_CCR_N | (255 & z) - 1 >> 6 & XEiJ.regCCR & XEiJ.REG_CCR_Z |  //ADDXはZをクリアすることはあるがセットすることはない
             ((x ^ z) & (y ^ z)) >> 6 & XEiJ.REG_CCR_V |
             (byte) ((x | y) ^ (x ^ y) & z) >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_addx_byte
    } else {  //ADD.B Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7];
      a = efaMltByte (ea);
      x = XEiJ.busRbs (a);
      z = x + y;
      XEiJ.busWb (a, z);
      XEiJ.regCCR = (XEiJ.MPU_TSTB_TABLE[255 & z] |
             ((x ^ z) & (y ^ z)) >> 6 & XEiJ.REG_CCR_V |
             (byte) ((x | y) ^ (x ^ y) & z) >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add_byte
    }
  }  //irpAddToMemByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDX.W Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_101_000_rrr
  //ADDX.W -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_101_001_rrr
  //ADD.W Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_101_mmm_rrr
  public static void irpAddToMemWord () throws M68kException {
    int ea = XEiJ.regOC & 63;
    int a, x, y, z;
    if (ea < XEiJ.EA_MM) {
      if (ea < XEiJ.EA_AR) {  //ADDX.W Dr,Dq
        int qqq = XEiJ.regOC >> 9 & 7;
        XEiJ.mpuCycleCount += 4;
        y = XEiJ.regRn[ea];
        x = XEiJ.regRn[qqq];
        z = x + y + (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.regRn[qqq] = ~65535 & x | (char) z;
      } else {  //ADDX.W -(Ar),-(Aq)
        XEiJ.mpuCycleCount += 18;
        y = XEiJ.busRws (XEiJ.regRn[ea] -= 2);  //このr[ea]はアドレスレジスタ
        a = XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= 2;
        x = XEiJ.busRws (a);
        z = x + y + (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
        XEiJ.busWw (a, z);
      }
      XEiJ.regCCR = (z >> 12 & XEiJ.REG_CCR_N | (char) z - 1 >> 14 & XEiJ.regCCR & XEiJ.REG_CCR_Z |  //ADDXはZをクリアすることはあるがセットすることはない
             ((x ^ z) & (y ^ z)) >> 14 & XEiJ.REG_CCR_V |
             (short) ((x | y) ^ (x ^ y) & z) >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_addx_word
    } else {  //ADD.W Dq,<ea>
      XEiJ.mpuCycleCount += 8;
      a = efaMltWord (ea);
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7];
      x = XEiJ.busRws (a);
      z = x + y;
      XEiJ.busWw (a, z);
      XEiJ.regCCR = (z >> 12 & XEiJ.REG_CCR_N | (char) z - 1 >> 14 & XEiJ.REG_CCR_Z |
             ((x ^ z) & (y ^ z)) >> 14 & XEiJ.REG_CCR_V |
             (short) ((x | y) ^ (x ^ y) & z) >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add_word
    }
  }  //irpAddToMemWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDX.L Dr,Dq                                    |-|012346|-|*UUUU|*****|          |1101_qqq_110_000_rrr
  //ADDX.L -(Ar),-(Aq)                              |-|012346|-|*UUUU|*****|          |1101_qqq_110_001_rrr
  //ADD.L Dq,<ea>                                   |-|012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_110_mmm_rrr
  public static void irpAddToMemLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    if (ea < XEiJ.EA_MM) {
      int x;
      int y;
      int z;
      if (ea < XEiJ.EA_AR) {  //ADDX.L Dr,Dq
        int qqq = XEiJ.regOC >> 9 & 7;
        XEiJ.mpuCycleCount += 8;
        XEiJ.regRn[qqq] = z = (x = XEiJ.regRn[qqq]) + (y = XEiJ.regRn[ea]) + (XEiJ.regCCR >> 4);  //Xの左側はすべて0なのでCCR_X&を省略
      } else {  //ADDX.L -(Ar),-(Aq)
        XEiJ.mpuCycleCount += 30;
        y = XEiJ.busRls (XEiJ.regRn[ea] -= 4);  //このr[ea]はアドレスレジスタ
        int a = XEiJ.regRn[XEiJ.regOC >> 9 & 15] -= 4;
        XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) + y + (XEiJ.regCCR >> 4));  //Xの左側はすべて0なのでCCR_X&を省略
      }
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_Z : 0) |
             ((x ^ z) & (y ^ z)) >>> 31 << 1 |
             ((x | y) ^ (x ^ y) & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_addx
    } else {  //ADD.L Dq,<ea>
      XEiJ.mpuCycleCount += 12;
      int a = efaMltLong (ea);
      int x;
      int y;
      int z;
      XEiJ.busWl (a, z = (x = XEiJ.busRls (a)) + (y = XEiJ.regRn[XEiJ.regOC >> 9 & 7]));
      XEiJ.regCCR = (z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) |
             ((x ^ z) & (y ^ z)) >>> 31 << 1 |
             ((x | y) ^ (x ^ y) & z) >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //ccr_add
    }
  }  //irpAddToMemLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ADDA.L <ea>,Aq                                  |-|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr
  //ADD.L <ea>,Aq                                   |A|012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr [ADDA.L <ea>,Aq]
  public static void irpAddaLong () throws M68kException {
    int ea = XEiJ.regOC & 63;
    XEiJ.mpuCycleCount += ea < XEiJ.EA_MM || ea == XEiJ.EA_IM ? 8 : 6;  //Dr/Ar/#<data>のとき8+、それ以外は6+
    int z = ea < XEiJ.EA_MM ? XEiJ.regRn[ea] : XEiJ.busRls (efaAnyLong (ea));  //このr[ea]はデータレジスタまたはアドレスレジスタ。ここでAqが変化する可能性があることに注意
    XEiJ.regRn[XEiJ.regOC >> 9 & 15] += z;  //r[op >> 9 & 15] += ea < XEiJ.EA_MM ? r[ea] : rls (efaAnyLong (ea));は不可
    //ccrは変化しない
  }  //irpAddaLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASR.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_000_000_rrr
  //LSR.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_000_001_rrr
  //ROXR.B #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_000_010_rrr
  //ROR.B #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_000_011_rrr
  //ASR.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_000_100_rrr
  //LSR.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_000_101_rrr
  //ROXR.B Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_000_110_rrr
  //ROR.B Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_000_111_rrr
  //ASR.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_000_000_rrr [ASR.B #1,Dr]
  //LSR.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_000_001_rrr [LSR.B #1,Dr]
  //ROXR.B Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_000_010_rrr [ROXR.B #1,Dr]
  //ROR.B Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_000_011_rrr [ROR.B #1,Dr]
  //
  //ASR.B #<data>,Dr
  //ASR.B Dq,Dr
  //  算術右シフトバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*00 Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................ｱｱｲｳｴｵｶｷ ｸｱ*0ｸ Z=ｱｲｳｴｵｶｷ==0
  //     2 ........................ｱｱｱｲｳｴｵｶ ｷｱ*0ｷ Z=ｱｲｳｴｵｶ==0
  //     3 ........................ｱｱｱｱｲｳｴｵ ｶｱ*0ｶ Z=ｱｲｳｴｵ==0
  //     4 ........................ｱｱｱｱｱｲｳｴ ｵｱ*0ｵ Z=ｱｲｳｴ==0
  //     5 ........................ｱｱｱｱｱｱｲｳ ｴｱ*0ｴ Z=ｱｲｳ==0
  //     6 ........................ｱｱｱｱｱｱｱｲ ｳｱ*0ｳ Z=ｱｲ==0
  //     7 ........................ｱｱｱｱｱｱｱｱ ｲｱ*0ｲ Z=ｱ==0
  //     8 ........................ｱｱｱｱｱｱｱｱ ｱｱ*0ｱ Z=ｱ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //LSR.B #<data>,Dr
  //LSR.B Dq,Dr
  //  論理右シフトバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*00 Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................0ｱｲｳｴｵｶｷ ｸ0*0ｸ Z=ｱｲｳｴｵｶｷ==0
  //     2 ........................00ｱｲｳｴｵｶ ｷ0*0ｷ Z=ｱｲｳｴｵｶ==0
  //     3 ........................000ｱｲｳｴｵ ｶ0*0ｶ Z=ｱｲｳｴｵ==0
  //     4 ........................0000ｱｲｳｴ ｵ0*0ｵ Z=ｱｲｳｴ==0
  //     5 ........................00000ｱｲｳ ｴ0*0ｴ Z=ｱｲｳ==0
  //     6 ........................000000ｱｲ ｳ0*0ｳ Z=ｱｲ==0
  //     7 ........................0000000ｱ ｲ0*0ｲ Z=ｱ==0
  //     8 ........................00000000 ｱ010ｱ
  //     9 ........................00000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //ROR.B #<data>,Dr
  //ROR.B Dq,Dr
  //  右ローテートバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*00 Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................ｸｱｲｳｴｵｶｷ Xｸ*0ｸ Z=ｱｲｳｴｵｶｷｸ==0
  //     :
  //     7 ........................ｲｳｴｵｶｷｸｱ Xｲ*0ｲ Z=ｱｲｳｴｵｶｷｸ==0
  //     8 ........................ｱｲｳｴｵｶｷｸ Xｱ*0ｱ Z=ｱｲｳｴｵｶｷｸ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最上位ビット
  //
  //ROXR.B #<data>,Dr
  //ROXR.B Dq,Dr
  //  拡張右ローテートバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*0X Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................Xｱｲｳｴｵｶｷ ｸX*0ｸ Z=ｱｲｳｴｵｶｷX==0
  //     2 ........................ｸXｱｲｳｴｵｶ ｷｸ*0ｷ Z=ｱｲｳｴｵｶｸX==0
  //     3 ........................ｷｸXｱｲｳｴｵ ｶｷ*0ｶ Z=ｱｲｳｴｵｷｸX==0
  //     4 ........................ｶｷｸXｱｲｳｴ ｵｶ*0ｵ Z=ｱｲｳｴｶｷｸX==0
  //     5 ........................ｵｶｷｸXｱｲｳ ｴｵ*0ｴ Z=ｱｲｳｵｶｷｸX==0
  //     6 ........................ｴｵｶｷｸXｱｲ ｳｴ*0ｳ Z=ｱｲｴｵｶｷｸX==0
  //     7 ........................ｳｴｵｶｷｸXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸX==0
  //     8 ........................ｲｳｴｵｶｷｸX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸX==0
  //     9 ........................ｱｲｳｴｵｶｷｸ Xｱ*0X Z=ｱｲｳｴｵｶｷｸ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpXxrToRegByte () throws M68kException {
    int rrr;
    int x = XEiJ.regRn[rrr = XEiJ.regOC & 7];
    int y;
    int z;
    int t;
    switch (XEiJ.regOC >> 3 & 0b111_000 >> 3) {
    case 0b000_000 >> 3:  //ASR.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (t = (byte) x >> y) >> 1);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b001_000 >> 3:  //LSR.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xff & x | (z = (t = (0xff & x) >>> y) >>> 1);
      XEiJ.regCCR = (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b010_000 >> 3:  //ROXR.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      z = (XEiJ.regCCR & XEiJ.REG_CCR_X) << 7 - 4 | (0xff & x) >>> 1;
      if (y == 1 - 1) {  //y=data-1=1-1
        t = x;
      } else {  //y=data-1=2-1～8-1
        z = x << 9 - 1 - y | (t = z >>> y - (2 - 1)) >>> 1;
      }
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) z);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b011_000 >> 3:  //ROR.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) (x << 7 - y | (0xff & x) >>> y + 1));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | z >>> 7 & 1;  //Xは変化しない。Cは結果の最上位ビット
      break;
    case 0b100_000 >> 3:  //ASR.B Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {  //y=data=0
        z = (byte) x;
        t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (t = (byte) x >> (y <= 8 ? y - 1 : 7)) >> 1);
        t = -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b101_000 >> 3:  //LSR.B Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {  //y=data=0
        z = (byte) x;
        XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (z < 0 ? XEiJ.REG_CCR_N : z == 0 ? XEiJ.REG_CCR_Z : 0);  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = ~0xff & x | (z = (t = y <= 8 ? (0xff & x) >>> y - 1 : 0) >>> 1);
        XEiJ.regCCR = (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      break;
    case 0b110_000 >> 3:  //ROXR.B Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      //y %= 9;
      y = (y & 7) - (y >> 3);  //y=data=-7～7
      y += y >> 3 & 9;  //y=data=0～8
      if (y == 0) {  //y=data=0
        z = (byte) x;
        t = -(XEiJ.regCCR >> 4 & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //Xは変化しない。CはXのコピー
      } else {  //y=data=1～8
        z = (XEiJ.regCCR & XEiJ.REG_CCR_X) << 7 - 4 | (0xff & x) >>> 1;
        if (y == 1) {  //y=data=1
          t = x;  //Cは最後に押し出されたビット
        } else {  //y=data=2～8
          z = x << 9 - y | (t = z >>> y - 2) >>> 1;
        }
        XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) z);
        t = -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b111_000 >> 3:  //ROR.B Dq,Dr
    default:
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {
        z = (byte) x;
        t = 0;  //Cはクリア
      } else {
        y &= 7;  //y=data=0～7
        XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) (x << 8 - y | (0xff & x) >>> y));
        t = z >>> 7 & 1;  //Cは結果の最上位ビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | t;  //Xは変化しない
    }
  }  //irpXxrToRegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASR.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_001_000_rrr
  //LSR.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_001_001_rrr
  //ROXR.W #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_001_010_rrr
  //ROR.W #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_001_011_rrr
  //ASR.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_001_100_rrr
  //LSR.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_001_101_rrr
  //ROXR.W Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_001_110_rrr
  //ROR.W Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_001_111_rrr
  //ASR.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_001_000_rrr [ASR.W #1,Dr]
  //LSR.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_001_001_rrr [LSR.W #1,Dr]
  //ROXR.W Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_001_010_rrr [ROXR.W #1,Dr]
  //ROR.W Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_001_011_rrr [ROR.W #1,Dr]
  //
  //ASR.W #<data>,Dr
  //ASR.W Dq,Dr
  //ASR.W <ea>
  //  算術右シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀｱ*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ==0
  //     :
  //    15 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲｱ*0ｲ Z=ｱ==0
  //    16 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱｱ*0ｱ Z=ｱ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //LSR.W #<data>,Dr
  //LSR.W Dq,Dr
  //LSR.W <ea>
  //  論理右シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ0*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ==0
  //     :
  //    15 ................000000000000000ｱ ｲ0*0ｲ Z=ｱ==0
  //    16 ................0000000000000000 ｱ010ｱ
  //    17 ................0000000000000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //ROR.W #<data>,Dr
  //ROR.W Dq,Dr
  //ROR.W <ea>
  //  右ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ Xﾀ*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     :
  //    15 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ Xｲ*0ｲ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //    16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0ｱ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最上位ビット
  //
  //ROXR.W #<data>,Dr
  //ROXR.W Dq,Dr
  //ROXR.W <ea>
  //  拡張右ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀX*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿX==0
  //     2 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿﾀ*0ｿ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾﾀX==0
  //     :
  //    15 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //    16 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //    17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpXxrToRegWord () throws M68kException {
    int rrr;
    int x = XEiJ.regRn[rrr = XEiJ.regOC & 7];
    int y;
    int z;
    int t;
    switch (XEiJ.regOC >> 3 & 0b111_000 >> 3) {
    case 0b000_000 >> 3:  //ASR.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (t = (short) x >> y) >> 1);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b001_000 >> 3:  //LSR.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xffff & x | (z = (t = (char) x >>> y) >>> 1);
      XEiJ.regCCR = (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b010_000 >> 3:  //ROXR.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      z = (XEiJ.regCCR & XEiJ.REG_CCR_X) << 15 - 4 | (char) x >>> 1;
      if (y == 1 - 1) {  //y=data-1=1-1
        t = x;
      } else {  //y=data-1=2-1～8-1
        z = x << 17 - 1 - y | (t = z >>> y - (2 - 1)) >>> 1;
      }
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) z);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b011_000 >> 3:  //ROR.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) (x << 16 - 1 - y | (char) x >>> y + 1));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | z >>> 15 & 1;  //Xは変化しない。Cは結果の最上位ビット
      break;
    case 0b100_000 >> 3:  //ASR.W Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {  //y=data=0
        z = (short) x;
        t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (t = (short) x >> (y <= 16 ? y - 1 : 15)) >> 1);
        t = -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b101_000 >> 3:  //LSR.W Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {  //y=data=0
        z = (short) x;
        XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (z < 0 ? XEiJ.REG_CCR_N : z == 0 ? XEiJ.REG_CCR_Z : 0);  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = ~0xffff & x | (z = (t = y <= 16 ? (char) x >>> y - 1 : 0) >>> 1);
        XEiJ.regCCR = (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      break;
    case 0b110_000 >> 3:  //ROXR.W Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      //y %= 17;
      y = (y & 15) - (y >> 4);  //y=data=-3～15
      y += y >> 4 & 17;  //y=data=0～16
      if (y == 0) {  //y=data=0
        z = (short) x;
        t = -(XEiJ.regCCR >> 4 & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //Xは変化しない。CはXのコピー
      } else {  //y=data=1～16
        z = (XEiJ.regCCR & XEiJ.REG_CCR_X) << 15 - 4 | (char) x >>> 1;
        if (y == 1) {  //y=data=1
          t = x;  //Cは最後に押し出されたビット
        } else {  //y=data=2～16
          z = x << 17 - y | (t = z >>> y - 2) >>> 1;
        }
        XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) z);
        t = -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b111_000 >> 3:  //ROR.W Dq,Dr
    default:
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {
        z = (short) x;
        t = 0;  //Cはクリア
      } else {
        y &= 15;  //y=data=0～15
        XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) (x << 16 - y | (char) x >>> y));
        t = z >>> 15 & 1;  //Cは結果の最上位ビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | t;  //Xは変化しない
    }
  }  //irpXxrToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASR.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_010_000_rrr
  //LSR.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_010_001_rrr
  //ROXR.L #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_010_010_rrr
  //ROR.L #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_010_011_rrr
  //ASR.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_010_100_rrr
  //LSR.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_010_101_rrr
  //ROXR.L Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_010_110_rrr
  //ROR.L Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_010_111_rrr
  //ASR.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_010_000_rrr [ASR.L #1,Dr]
  //LSR.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_010_001_rrr [LSR.L #1,Dr]
  //ROXR.L Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_010_010_rrr [ROXR.L #1,Dr]
  //ROR.L Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_010_011_rrr [ROR.L #1,Dr]
  //
  //ASR.L #<data>,Dr
  //ASR.L Dq,Dr
  //  算術右シフトロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐｱ*0ﾐ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ==0
  //     :
  //    31 ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲｱ*0ｲ Z=ｱ==0
  //    32 ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱｱ*0ｱ Z=ｱ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //LSR.L #<data>,Dr
  //LSR.L Dq,Dr
  //  論理右シフトロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ0*0ﾐ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ==0
  //     :
  //    31 0000000000000000000000000000000ｱ ｲ0*0ｲ Z=ｱ==0
  //    32 00000000000000000000000000000000 ｱ010ｱ
  //    33 00000000000000000000000000000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //ROR.L #<data>,Dr
  //ROR.L Dq,Dr
  //  右ローテートロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 ﾐｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ Xﾐ*0ﾐ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     :
  //    31 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐｱ Xｲ*0ｲ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //    32 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*0ｱ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最上位ビット
  //
  //ROXR.L #<data>,Dr
  //ROXR.L Dq,Dr
  //  拡張右ローテートロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐX*0ﾐ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏX==0
  //     2 ﾐXｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ ﾏﾐ*0ﾏ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾐX==0
  //     :
  //    31 ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX==0
  //    32 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX==0
  //    33 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpXxrToRegLong () throws M68kException {
    int rrr;
    int x = XEiJ.regRn[rrr = XEiJ.regOC & 7];
    int y;
    int z;
    int t;
    switch (XEiJ.regOC >> 3 & 0b111_000 >> 3) {
    case 0b000_000 >> 3:  //ASR.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = z = (t = x >> y) >> 1;
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b001_000 >> 3:  //LSR.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = z = (t = x >>> y) >>> 1;
      XEiJ.regCCR = (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b010_000 >> 3:  //ROXR.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      z = (XEiJ.regCCR & XEiJ.REG_CCR_X) << 31 - 4 | x >>> 1;
      if (y == 1 - 1) {  //y=data-1=1-1
        t = x;
      } else {  //y=data-1=2-1～8-1
        z = x << -y | (t = z >>> y - (2 - 1)) >>> 1;  //Javaのシフト演算子は5ビットでマスクされるので33-1-yを-yに省略
      }
      XEiJ.regRn[rrr] = z;
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b011_000 >> 3:  //ROR.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = z = x << ~y | x >>> y + 1;  //Javaのシフト演算子は5ビットでマスクされるので32-1-yを~yに省略
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | z >>> 31;  //Xは変化しない。Cは結果の最上位ビット
      break;
    case 0b100_000 >> 3:  //ASR.L Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      if (y == 0) {  //y=data=0
        z = x;
        t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = z = (t = x >> (y <= 32 ? y - 1 : 31)) >> 1;
        t = -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b101_000 >> 3:  //LSR.L Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      if (y == 0) {  //y=data=0
        z = x;
        XEiJ.regCCR = XEiJ.regCCR & XEiJ.REG_CCR_X | (z < 0 ? XEiJ.REG_CCR_N : z == 0 ? XEiJ.REG_CCR_Z : 0);  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = z = (t = y <= 32 ? x >>> y - 1 : 0) >>> 1;
        XEiJ.regCCR = (z == 0 ? XEiJ.REG_CCR_Z : 0) | -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      break;
    case 0b110_000 >> 3:  //ROXR.L Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      //y %= 33;
      y -= 32 - y >> 6 & 33;  //y=data=0～32
      if (y == 0) {  //y=data=0
        z = x;
        t = -(XEiJ.regCCR >> 4 & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //Xは変化しない。CはXのコピー
      } else {  //y=data=1～32
        z = (XEiJ.regCCR & XEiJ.REG_CCR_X) << 31 - 4 | x >>> 1;
        if (y == 1) {  //y=data=1
          t = x;  //Cは最後に押し出されたビット
        } else {  //y=data=2～32
          z = x << 33 - y | (t = z >>> y - 2) >>> 1;
        }
        XEiJ.regRn[rrr] = z;
        t = -(t & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b111_000 >> 3:  //ROR.L Dq,Dr
    default:
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      if (y == 0) {
        z = x;
        t = 0;  //Cはクリア
      } else {
        y &= 31;  //y=data=0～31
        XEiJ.regRn[rrr] = z = x << -y | x >>> y;  //Javaのシフト演算子は5ビットでマスクされるので32-yを-yに省略。y=32のときx|xになるが問題ない
        t = z >>> 31;  //Cは結果の最上位ビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | t;  //Xは変化しない
    }
  }  //irpXxrToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASR.W <ea>                                      |-|012346|-|UUUUU|***0*|  M+-WXZ  |1110_000_011_mmm_rrr
  //
  //ASR.W #<data>,Dr
  //ASR.W Dq,Dr
  //ASR.W <ea>
  //  算術右シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀｱ*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ==0
  //     :
  //    15 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲｱ*0ｲ Z=ｱ==0
  //    16 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱｱ*0ｱ Z=ｱ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  public static void irpAsrToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRws (a);
    int z = x >> 1;
    XEiJ.busWw (a, z);
    XEiJ.regCCR = ((z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   -(x & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //XとCは最後に押し出されたビット
  }  //irpAsrToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASL.B #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_100_000_rrr
  //LSL.B #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_100_001_rrr
  //ROXL.B #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_100_010_rrr
  //ROL.B #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_100_011_rrr
  //ASL.B Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_100_100_rrr
  //LSL.B Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_100_101_rrr
  //ROXL.B Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_100_110_rrr
  //ROL.B Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_100_111_rrr
  //ASL.B Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_100_000_rrr [ASL.B #1,Dr]
  //LSL.B Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_100_001_rrr [LSL.B #1,Dr]
  //ROXL.B Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_100_010_rrr [ROXL.B #1,Dr]
  //ROL.B Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_100_011_rrr [ROL.B #1,Dr]
  //
  //ASL.B #<data>,Dr
  //ASL.B Dq,Dr
  //  算術左シフトバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*00 Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................ｲｳｴｵｶｷｸ0 ｱｲ**ｱ Z=ｲｳｴｵｶｷｸ==0,V=ｱｲ!=0/-1
  //     :
  //     7 ........................ｸ0000000 ｷｸ**ｷ Z=ｸ==0,V=ｱｲｳｴｵｶｷｸ!=0/-1
  //     8 ........................00000000 ｸ01*ｸ V=ｱｲｳｴｵｶｷｸ!=0
  //     9 ........................00000000 001*0 V=ｱｲｳｴｵｶｷｸ!=0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  ASRで元に戻せないときセット。他はクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //LSL.B #<data>,Dr
  //LSL.B Dq,Dr
  //  論理左シフトバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*00 Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................ｲｳｴｵｶｷｸ0 ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸ==0
  //     :
  //     7 ........................ｸ0000000 ｷｸ*0ｷ Z=ｸ==0
  //     8 ........................00000000 ｸ010ｸ
  //     9 ........................00000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //ROL.B #<data>,Dr
  //ROL.B Dq,Dr
  //  左ローテートバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*00 Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................ｲｳｴｵｶｷｸｱ Xｲ*0ｱ Z=ｱｲｳｴｵｶｷｸ==0
  //     :
  //     7 ........................ｸｱｲｳｴｵｶｷ Xｸ*0ｷ Z=ｱｲｳｴｵｶｷｸ==0
  //     8 ........................ｱｲｳｴｵｶｷｸ Xｱ*0ｸ Z=ｱｲｳｴｵｶｷｸ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最下位ビット
  //
  //ROXL.B #<data>,Dr
  //ROXL.B Dq,Dr
  //  拡張左ローテートバイト
  //       ........................ｱｲｳｴｵｶｷｸ XNZVC
  //     0 ........................ｱｲｳｴｵｶｷｸ Xｱ*0X Z=ｱｲｳｴｵｶｷｸ==0
  //     1 ........................ｲｳｴｵｶｷｸX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸX==0
  //     2 ........................ｳｴｵｶｷｸXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸX==0
  //     :
  //     7 ........................ｸXｱｲｳｴｵｶ ｷｸ*0ｷ Z=ｱｲｳｴｵｶｸX==0
  //     8 ........................Xｱｲｳｴｵｶｷ ｸX*0ｸ Z=ｱｲｳｴｵｶｷX==0
  //     9 ........................ｱｲｳｴｵｶｷｸ Xｱ*0X Z=ｱｲｳｴｵｶｷｸ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpXxlToRegByte () throws M68kException {
    int rrr;
    int x = XEiJ.regRn[rrr = XEiJ.regOC & 7];
    int y;
    int z;
    int t;
    switch (XEiJ.regOC >> 3 & 0b111_000 >> 3) {
    case 0b000_000 >> 3:  //ASL.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) ((t = x << y) << 1));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (z >> y + 1 != (byte) x ? XEiJ.REG_CCR_V : 0) | (byte) t >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //VはASRで元に戻せないときセット。XとCは最後に押し出されたビット
      break;
    case 0b001_000 >> 3:  //LSL.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) ((t = x << y) << 1));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (byte) t >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b010_000 >> 3:  //ROXL.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      z = x << 1 | XEiJ.regCCR >> 4 & 1;
      if (y == 1 - 1) {  //y=data-1=1-1
        t = x;
      } else {  //y=data-1=2-1～8-1
        z = (t = z << y - (2 - 1)) << 1 | (0xff & x) >>> 9 - 1 - y;
      }
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) z);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (byte) t >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b011_000 >> 3:  //ROL.B #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) (x << y + 1 | (0xff & x) >>> 7 - y));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | z & 1;  //Xは変化しない。Cは結果の最下位ビット
      break;
    case 0b100_000 >> 3:  //ASL.B Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y <= 7) {  //y=data=0～7
        if (y == 0) {  //y=data=0
          z = (byte) x;
          t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。VとCはクリア
        } else {  //y=data=1～7
          XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) ((t = x << y - 1) << 1));
          t = (z >> y != (byte) x ? XEiJ.REG_CCR_V : 0) | (byte) t >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //VはASRで元に戻せないときセット。XとCは最後に押し出されたビット
        }
        XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      } else {  //y=data=8～63
        XEiJ.regRn[rrr] = ~0xff & x;
        XEiJ.regCCR = XEiJ.REG_CCR_Z | ((byte) x != 0 ? XEiJ.REG_CCR_V : 0) | (y == 8 ? -(x & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C) : 0);
      }
      break;
    case 0b101_000 >> 3:  //LSL.B Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {  //y=data=0
        z = (byte) x;
        t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) ((t = y <= 8 ? x << y - 1 : 0) << 1));
        t = (byte) t >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b110_000 >> 3:  //ROXL.B Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      //y %= 9;
      y = (y & 7) - (y >> 3);  //y=data=-7～7
      y += y >> 3 & 9;  //y=data=0～8
      if (y == 0) {  //y=data=0
        z = (byte) x;
        t = -(XEiJ.regCCR >> 4 & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //Xは変化しない。CはXのコピー
      } else {  //y=data=1～8
        z = x << 1 | XEiJ.regCCR >> 4 & 1;
        if (y == 1) {  //y=data=1
          t = x;  //Cは最後に押し出されたビット
        } else {  //y=data=2～8
          z = (t = z << y - 2) << 1 | (0xff & x) >>> 9 - y;
        }
        XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) z);
        t = (byte) t >> 7 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b111_000 >> 3:  //ROL.B Dq,Dr
    default:
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {
        z = (byte) x;
        t = 0;  //Cはクリア
      } else {
        y &= 7;  //y=data=0～7
        XEiJ.regRn[rrr] = ~0xff & x | 0xff & (z = (byte) (x << y | (0xff & x) >>> 8 - y));
        t = z & 1;  //Cは結果の最下位ビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | t;  //Xは変化しない
    }
  }  //irpXxlToRegByte

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASL.W #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_101_000_rrr
  //LSL.W #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_101_001_rrr
  //ROXL.W #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_101_010_rrr
  //ROL.W #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_101_011_rrr
  //ASL.W Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_101_100_rrr
  //LSL.W Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_101_101_rrr
  //ROXL.W Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_101_110_rrr
  //ROL.W Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_101_111_rrr
  //ASL.W Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_101_000_rrr [ASL.W #1,Dr]
  //LSL.W Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_101_001_rrr [LSL.W #1,Dr]
  //ROXL.W Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_101_010_rrr [ROXL.W #1,Dr]
  //ROL.W Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_101_011_rrr [ROL.W #1,Dr]
  //
  //ASL.W #<data>,Dr
  //ASL.W Dq,Dr
  //ASL.W <ea>
  //  算術左シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱｲ**ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0,V=ｱｲ!=0/-1
  //     :
  //    15 ................ﾀ000000000000000 ｿﾀ**ｿ Z=ﾀ==0,V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ!=0/-1
  //    16 ................0000000000000000 ﾀ01*ﾀ V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ!=0
  //    17 ................0000000000000000 001*0 V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ!=0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  ASRで元に戻せないときセット。他はクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //LSL.W #<data>,Dr
  //LSL.W Dq,Dr
  //LSL.W <ea>
  //  論理左シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     :
  //    15 ................ﾀ000000000000000 ｿﾀ*0ｿ Z=ﾀ==0
  //    16 ................0000000000000000 ﾀ010ﾀ
  //    17 ................0000000000000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //ROL.W #<data>,Dr
  //ROL.W Dq,Dr
  //ROL.W <ea>
  //  左ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ Xｲ*0ｱ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     :
  //    15 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ Xﾀ*0ｿ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //    16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最下位ビット
  //
  //ROXL.W #<data>,Dr
  //ROXL.W Dq,Dr
  //ROXL.W <ea>
  //  拡張左ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //     2 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //     :
  //    15 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿﾀ*0ｿ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾﾀX==0
  //    16 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀX*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿX==0
  //    17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpXxlToRegWord () throws M68kException {
    int rrr;
    int x = XEiJ.regRn[rrr = XEiJ.regOC & 7];
    int y;
    int z;
    int t;
    switch (XEiJ.regOC >> 3 & 0b111_000 >> 3) {
    case 0b000_000 >> 3:  //ASL.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) ((t = x << y) << 1));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (z >> y + 1 != (short) x ? XEiJ.REG_CCR_V : 0) | (short) t >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //VはASRで元に戻せないときセット。XとCは最後に押し出されたビット
      break;
    case 0b001_000 >> 3:  //LSL.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) ((t = x << y) << 1));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (short) t >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b010_000 >> 3:  //ROXL.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      z = x << 1 | XEiJ.regCCR >> 4 & 1;
      if (y == 1 - 1) {  //y=data-1=1-1
        t = x;
      } else {  //y=data-1=2-1～8-1
        z = (t = z << y - (2 - 1)) << 1 | (char) x >>> 17 - 1 - y;
      }
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) z);
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (short) t >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b011_000 >> 3:  //ROL.W #<data>,Dr
      XEiJ.mpuCycleCount += 6 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) (x << y + 1 | (char) x >>> 16 - 1 - y));
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | z & 1;  //Xは変化しない。Cは結果の最下位ビット
      break;
    case 0b100_000 >> 3:  //ASL.W Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y <= 15) {  //y=data=0～15
        if (y == 0) {  //y=data=0
          z = (short) x;
          t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。VとCはクリア
        } else {  //y=data=1～15
          XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) ((t = x << y - 1) << 1));
          t = (z >> y != (short) x ? XEiJ.REG_CCR_V : 0) | (short) t >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //VはASRで元に戻せないときセット。XとCは最後に押し出されたビット
        }
        XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      } else {  //y=data=16～63
        XEiJ.regRn[rrr] = ~0xffff & x;
        XEiJ.regCCR = XEiJ.REG_CCR_Z | ((short) x != 0 ? XEiJ.REG_CCR_V : 0) | (y == 16 ? -(x & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C) : 0);
      }
      break;
    case 0b101_000 >> 3:  //LSL.W Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {  //y=data=0
        z = (short) x;
        t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) ((t = y <= 16 ? x << y - 1 : 0) << 1));
        t = (short) t >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b110_000 >> 3:  //ROXL.W Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      //y %= 17;
      y = (y & 15) - (y >> 4);  //y=data=-3～15
      y += y >> 4 & 17;  //y=data=0～16
      if (y == 0) {  //y=data=0
        z = (short) x;
        t = -(XEiJ.regCCR >> 4 & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //Xは変化しない。CはXのコピー
      } else {  //y=data=1～16
        z = x << 1 | XEiJ.regCCR >> 4 & 1;
        if (y == 1) {  //y=data=1
          t = x;  //Cは最後に押し出されたビット
        } else {  //y=data=2～16
          z = (t = z << y - 2) << 1 | (char) x >>> 17 - y;
        }
        XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) z);
        t = (short) t >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b111_000 >> 3:  //ROL.W Dq,Dr
    default:
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 6 + (y << 1);
      if (y == 0) {
        z = (short) x;
        t = 0;  //Cはクリア
      } else {
        y &= 15;  //y=data=0～15
        XEiJ.regRn[rrr] = ~0xffff & x | (char) (z = (short) (x << y | (char) x >>> 16 - y));
        t = z & 1;  //Cは結果の最下位ビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | t;  //Xは変化しない
    }
  }  //irpXxlToRegWord

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASL.L #<data>,Dr                                |-|012346|-|UUUUU|*****|          |1110_qqq_110_000_rrr
  //LSL.L #<data>,Dr                                |-|012346|-|UUUUU|***0*|          |1110_qqq_110_001_rrr
  //ROXL.L #<data>,Dr                               |-|012346|-|*UUUU|***0*|          |1110_qqq_110_010_rrr
  //ROL.L #<data>,Dr                                |-|012346|-|-UUUU|-**0*|          |1110_qqq_110_011_rrr
  //ASL.L Dq,Dr                                     |-|012346|-|UUUUU|*****|          |1110_qqq_110_100_rrr
  //LSL.L Dq,Dr                                     |-|012346|-|UUUUU|***0*|          |1110_qqq_110_101_rrr
  //ROXL.L Dq,Dr                                    |-|012346|-|*UUUU|***0*|          |1110_qqq_110_110_rrr
  //ROL.L Dq,Dr                                     |-|012346|-|-UUUU|-**0*|          |1110_qqq_110_111_rrr
  //ASL.L Dr                                        |A|012346|-|UUUUU|*****|          |1110_001_110_000_rrr [ASL.L #1,Dr]
  //LSL.L Dr                                        |A|012346|-|UUUUU|***0*|          |1110_001_110_001_rrr [LSL.L #1,Dr]
  //ROXL.L Dr                                       |A|012346|-|*UUUU|***0*|          |1110_001_110_010_rrr [ROXL.L #1,Dr]
  //ROL.L Dr                                        |A|012346|-|-UUUU|-**0*|          |1110_001_110_011_rrr [ROL.L #1,Dr]
  //
  //ASL.L #<data>,Dr
  //ASL.L Dq,Dr
  //  算術左シフトロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ**0 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ0 ｱｲ**ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0,V=ｱｲ!=0/-1
  //     :
  //    31 ﾐ0000000000000000000000000000000 ﾏﾐ**ﾏ Z=ﾐ==0,V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ!=0/-1
  //    32 00000000000000000000000000000000 ﾐ01*ﾐ V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ!=0
  //    33 00000000000000000000000000000000 001*0 V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ!=0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  ASRで元に戻せないときセット。他はクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //LSL.L #<data>,Dr
  //LSL.L Dq,Dr
  //  論理左シフトロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ0 ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     :
  //    31 ﾐ0000000000000000000000000000000 ﾏﾐ*0ﾏ Z=ﾐ==0
  //    32 00000000000000000000000000000000 ﾐ010ﾐ
  //    33 00000000000000000000000000000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //
  //ROL.L #<data>,Dr
  //ROL.L Dq,Dr
  //  左ローテートロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐｱ Xｲ*0ｱ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     :
  //    31 ﾐｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ Xﾐ*0ﾏ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //    32 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*0ﾐ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最下位ビット
  //
  //ROXL.L #<data>,Dr
  //ROXL.L Dq,Dr
  //  拡張左ローテートロング
  //       ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ XNZVC
  //     0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //     1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX==0
  //     2 ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX==0
  //     :
  //    31 ﾐXｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ ﾏﾐ*0ﾏ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾐX==0
  //    32 Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐX*0ﾐ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏX==0
  //    33 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpXxlToRegLong () throws M68kException {
    int rrr;
    int x = XEiJ.regRn[rrr = XEiJ.regOC & 7];
    int y;
    int z;
    int t;
    switch (XEiJ.regOC >> 3 & 0b111_000 >> 3) {
    case 0b000_000 >> 3:  //ASL.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = z = (t = x << y) << 1;
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | (z >> y + 1 != x ? XEiJ.REG_CCR_V : 0) | t >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //VはASRで元に戻せないときセット。XとCは最後に押し出されたビット
      break;
    case 0b001_000 >> 3:  //LSL.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = z = (t = x << y) << 1;
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b010_000 >> 3:  //ROXL.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      z = x << 1 | XEiJ.regCCR >> 4 & 1;
      if (y == 1 - 1) {  //y=data-1=1-1
        t = x;
      } else {  //y=data-1=2-1～8-1
        z = (t = z << y - (2 - 1)) << 1 | x >>> -y;  //Javaのシフト演算子は5ビットでマスクされるので33-1-yを-yに省略
      }
      XEiJ.regRn[rrr] = z;
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      break;
    case 0b011_000 >> 3:  //ROL.L #<data>,Dr
      XEiJ.mpuCycleCount += 8 + 2 + ((y = (XEiJ.regOC >> 9) - 1 & 7) << 1);  //y=data-1=1-1～8-1
      XEiJ.regRn[rrr] = z = x << y + 1 | x >>> ~y;  //Javaのシフト演算子は5ビットでマスクされるので32-1-yを~yに省略
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | z & 1;  //Xは変化しない。Cは結果の最下位ビット
      break;
    case 0b100_000 >> 3:  //ASL.L Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      if (y <= 31) {  //y=data=0～31
        if (y == 0) {  //y=data=0
          z = x;
          t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。VとCはクリア
        } else {  //y=data=1～31
          XEiJ.regRn[rrr] = z = (t = x << y - 1) << 1;
          t = (z >> y != x ? XEiJ.REG_CCR_V : 0) | t >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //VはASRで元に戻せないときセット。XとCは最後に押し出されたビット
        }
        XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      } else {  //y=data=32～63
        XEiJ.regRn[rrr] = 0;
        XEiJ.regCCR = XEiJ.REG_CCR_Z | (x != 0 ? XEiJ.REG_CCR_V : 0) | (y == 32 ? -(x & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C) : 0);
      }
      break;
    case 0b101_000 >> 3:  //LSL.L Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      if (y == 0) {  //y=data=0
        z = x;
        t = XEiJ.regCCR & XEiJ.REG_CCR_X;  //Xは変化しない。Cはクリア
      } else {  //y=data=1～63
        XEiJ.regRn[rrr] = z = (t = y <= 32 ? x << y - 1 : 0) << 1;
        t = t >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b110_000 >> 3:  //ROXL.L Dq,Dr
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      //y %= 33;
      y -= 32 - y >> 6 & 33;  //y=data=0～32
      if (y == 0) {  //y=data=0
        z = x;
        t = -(XEiJ.regCCR >> 4 & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //Xは変化しない。CはXのコピー
      } else {  //y=data=1～32
        z = x << 1 | XEiJ.regCCR >> 4 & 1;
        if (y == 1) {  //y=data=1
          t = x;  //Cは最後に押し出されたビット
        } else {  //y=data=2～32
          z = (t = z << y - 2) << 1 | x >>> 33 - y;
        }
        XEiJ.regRn[rrr] = z;
        t = t >> 31 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);  //XとCは最後に押し出されたビット
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.REG_CCR_Z : 0) | t;
      break;
    case 0b111_000 >> 3:  //ROL.L Dq,Dr
    default:
      y = XEiJ.regRn[XEiJ.regOC >> 9 & 7] & 63;  //y=0～63。Javaのシフト演算子は5ビットでマスクされることに注意
      XEiJ.mpuCycleCount += 8 + (y << 1);
      if (y == 0) {
        z = x;
        t = 0;  //Cはクリア
      } else {
        XEiJ.regRn[rrr] = z = x << y | x >>> -y;  //Javaのシフト演算子は5ビットでマスクされるのでy&31をyに、32-(y&31)を-yに省略。y=32のときx|xになるが問題ない
        t = z & 1;
      }
      XEiJ.regCCR = z >> 28 & XEiJ.REG_CCR_N | (z == 0 ? XEiJ.regCCR & XEiJ.REG_CCR_X | XEiJ.REG_CCR_Z : XEiJ.regCCR & XEiJ.REG_CCR_X) | t;  //Xは変化しない
    }
  }  //irpXxlToRegLong

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ASL.W <ea>                                      |-|012346|-|UUUUU|*****|  M+-WXZ  |1110_000_111_mmm_rrr
  //
  //ASL.W #<data>,Dr
  //ASL.W Dq,Dr
  //ASL.W <ea>
  //  算術左シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱｲ**ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0,V=ｱｲ!=0/-1
  //     :
  //    15 ................ﾀ000000000000000 ｿﾀ**ｿ Z=ﾀ==0,V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ!=0/-1
  //    16 ................0000000000000000 ﾀ01*ﾀ V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ!=0
  //    17 ................0000000000000000 001*0 V=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ!=0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  ASRで元に戻せないときセット。他はクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  public static void irpAslToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRws (a);
    int z = (short) (x << 1);
    XEiJ.busWw (a, z);
    XEiJ.regCCR = ((z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   (x ^ z) >>> 31 << 1 |  //Vは最上位ビットが変化したときセット
                   x >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //XとCは最後に押し出されたビット
  }  //irpAslToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //LSR.W <ea>                                      |-|012346|-|UUUUU|*0*0*|  M+-WXZ  |1110_001_011_mmm_rrr
  //
  //LSR.W #<data>,Dr
  //LSR.W Dq,Dr
  //LSR.W <ea>
  //  論理右シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ0*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ==0
  //     :
  //    15 ................000000000000000ｱ ｲ0*0ｲ Z=ｱ==0
  //    16 ................0000000000000000 ｱ010ｱ
  //    17 ................0000000000000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  public static void irpLsrToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRwz (a);
    int z = x >>> 1;
    XEiJ.busWw (a, z);
    XEiJ.regCCR = ((z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   -(x & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //XとCは最後に押し出されたビット
  }  //irpLsrToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //LSL.W <ea>                                      |-|012346|-|UUUUU|***0*|  M+-WXZ  |1110_001_111_mmm_rrr
  //
  //LSL.W #<data>,Dr
  //LSL.W Dq,Dr
  //LSL.W <ea>
  //  論理左シフトワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     :
  //    15 ................ﾀ000000000000000 ｿﾀ*0ｿ Z=ﾀ==0
  //    16 ................0000000000000000 ﾀ010ﾀ
  //    17 ................0000000000000000 00100
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  public static void irpLslToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRws (a);
    int z = (short) (x << 1);
    XEiJ.busWw (a, z);
    XEiJ.regCCR = ((z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   x >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //XとCは最後に押し出されたビット
  }  //irpLslToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ROXR.W <ea>                                     |-|012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_011_mmm_rrr
  //
  //ROXR.W #<data>,Dr
  //ROXR.W Dq,Dr
  //ROXR.W <ea>
  //  拡張右ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀX*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿX==0
  //     2 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿﾀ*0ｿ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾﾀX==0
  //     :
  //    15 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //    16 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //    17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpRoxrToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRwz (a);
    int z = -(XEiJ.regCCR & XEiJ.REG_CCR_X) << 15 - 4 | x >>> 1;
    XEiJ.busWw (a, z);
    XEiJ.regCCR = ((z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   -(x & 1) & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //XとCは最後に押し出されたビット
  }  //irpRoxrToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ROXL.W <ea>                                     |-|012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_111_mmm_rrr
  //
  //ROXL.W #<data>,Dr
  //ROXL.W Dq,Dr
  //ROXL.W <ea>
  //  拡張左ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱｲ*0ｱ Z=ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //     2 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲｳ*0ｲ Z=ｱｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX==0
  //     :
  //    15 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿﾀ*0ｿ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾﾀX==0
  //    16 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀX*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿX==0
  //    17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0X Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  public static void irpRoxlToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRws (a);
    int z = (short) (x << 1 | XEiJ.regCCR >> 4 & 1);
    XEiJ.busWw (a, z);
    XEiJ.regCCR = ((z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   x >> 15 & (XEiJ.REG_CCR_X | XEiJ.REG_CCR_C));  //XとCは最後に押し出されたビット
  }  //irpRoxlToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ROR.W <ea>                                      |-|012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_011_mmm_rrr
  //
  //ROR.W #<data>,Dr
  //ROR.W Dq,Dr
  //ROR.W <ea>
  //  右ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ Xﾀ*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     :
  //    15 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ Xｲ*0ｲ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //    16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0ｱ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最上位ビット
  public static void irpRorToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRwz (a);
    int z = (short) (x << 15 | x >>> 1);
    XEiJ.busWw (a, z);
    XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                   (z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   z >>> 31);  //Cは結果の最上位ビット
  }  //irpRorToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //ROL.W <ea>                                      |-|012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_111_mmm_rrr
  //
  //ROL.W #<data>,Dr
  //ROL.W Dq,Dr
  //ROL.W <ea>
  //  左ローテートワード
  //       ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ XNZVC
  //     0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*00 Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ Xｲ*0ｱ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //     :
  //    15 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ Xﾀ*0ｿ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //    16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ Xｱ*0ﾀ Z=ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ==0
  //  CCR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最下位ビット
  public static void irpRolToMem () throws M68kException {
    XEiJ.mpuCycleCount += 8;
    int ea = XEiJ.regOC & 63;
    int a = efaMltWord (ea);
    int x = XEiJ.busRwz (a);
    int z = (short) (x << 1 | x >>> 15);
    XEiJ.busWw (a, z);
    XEiJ.regCCR = (XEiJ.regCCR & XEiJ.REG_CCR_X |  //Xは変化しない
                   (z < 0 ? XEiJ.REG_CCR_N : 0) |
                   (z == 0 ? XEiJ.REG_CCR_Z : 0) |
                   z & 1);  //Cは結果の最下位ビット
  }  //irpRolToMem

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //FPACK <data>                                    |A|012346|-|UUUUU|*****|          |1111_111_0dd_ddd_ddd [FLINE #<data>]
  public static void irpFpack () throws M68kException {
    if (!MainMemory.mmrFEfuncActivated) {
      irpFline ();
      return;
    }
    StringBuilder sb;
    int a0;
    if (FEFunction.FPK_DEBUG_TRACE) {
      sb = new StringBuilder ();
      String name = Disassembler.DIS_FPACK_NAME[XEiJ.regOC & 255];
      if (name.length () == 0) {
        XEiJ.fmtHex4 (sb.append ('$'), XEiJ.regOC);
      } else {
        sb.append (name);
      }
      sb.append ('\n');
      XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (sb.append ("  D0="), XEiJ.regRn[0]).append (" D1="), XEiJ.regRn[1]).append (" D2="), XEiJ.regRn[2]).append (" D3="), XEiJ.regRn[3]);
      a0 = XEiJ.regRn[8];
      MainMemory.mmrRstr (sb.append (" (A0)=\""), a0, MainMemory.mmrStrlen (a0, 20)).append ("\"\n");
    }
    XEiJ.mpuCycleCount += FEFunction.FPK_CLOCK;  //一律にFEFunction.FPK_CLOCKサイクルかかることにする
    switch (XEiJ.regOC & 255) {
    case 0x00: FEFunction.fpkLMUL (); break;
    case 0x01: FEFunction.fpkLDIV (); break;
    case 0x02: FEFunction.fpkLMOD (); break;
      //case 0x03: break;
    case 0x04: FEFunction.fpkUMUL (); break;
    case 0x05: FEFunction.fpkUDIV (); break;
    case 0x06: FEFunction.fpkUMOD (); break;
      //case 0x07: break;
    case 0x08: FEFunction.fpkIMUL (); break;
    case 0x09: FEFunction.fpkIDIV (); break;
      //case 0x0a: break;
      //case 0x0b: break;
    case 0x0c: FEFunction.fpkRANDOMIZE (); break;
    case 0x0d: FEFunction.fpkSRAND (); break;
    case 0x0e: FEFunction.fpkRAND (); break;
      //case 0x0f: break;
    case 0x10: FEFunction.fpkSTOL (); break;
    case 0x11: FEFunction.fpkLTOS (); break;
    case 0x12: FEFunction.fpkSTOH (); break;
    case 0x13: FEFunction.fpkHTOS (); break;
    case 0x14: FEFunction.fpkSTOO (); break;
    case 0x15: FEFunction.fpkOTOS (); break;
    case 0x16: FEFunction.fpkSTOB (); break;
    case 0x17: FEFunction.fpkBTOS (); break;
    case 0x18: FEFunction.fpkIUSING (); break;
      //case 0x19: break;
    case 0x1a: FEFunction.fpkLTOD (); break;
    case 0x1b: FEFunction.fpkDTOL (); break;
    case 0x1c: FEFunction.fpkLTOF (); break;
    case 0x1d: FEFunction.fpkFTOL (); break;
    case 0x1e: FEFunction.fpkFTOD (); break;
    case 0x1f: FEFunction.fpkDTOF (); break;
    case 0x20: FEFunction.fpkVAL (); break;
    case 0x21: FEFunction.fpkUSING (); break;
    case 0x22: FEFunction.fpkSTOD (); break;
    case 0x23: FEFunction.fpkDTOS (); break;
    case 0x24: FEFunction.fpkECVT (); break;
    case 0x25: FEFunction.fpkFCVT (); break;
    case 0x26: FEFunction.fpkGCVT (); break;
      //case 0x27: break;
    case 0x28: FEFunction.fpkDTST (); break;
    case 0x29: FEFunction.fpkDCMP (); break;
    case 0x2a: FEFunction.fpkDNEG (); break;
    case 0x2b: FEFunction.fpkDADD (); break;
    case 0x2c: FEFunction.fpkDSUB (); break;
    case 0x2d: FEFunction.fpkDMUL (); break;
    case 0x2e: FEFunction.fpkDDIV (); break;
    case 0x2f: FEFunction.fpkDMOD (); break;
    case 0x30: FEFunction.fpkDABS (); break;
    case 0x31: FEFunction.fpkDCEIL (); break;
    case 0x32: FEFunction.fpkDFIX (); break;
    case 0x33: FEFunction.fpkDFLOOR (); break;
    case 0x34: FEFunction.fpkDFRAC (); break;
    case 0x35: FEFunction.fpkDSGN (); break;
    case 0x36: FEFunction.fpkSIN (); break;
    case 0x37: FEFunction.fpkCOS (); break;
    case 0x38: FEFunction.fpkTAN (); break;
    case 0x39: FEFunction.fpkATAN (); break;
    case 0x3a: FEFunction.fpkLOG (); break;
    case 0x3b: FEFunction.fpkEXP (); break;
    case 0x3c: FEFunction.fpkSQR (); break;
    case 0x3d: FEFunction.fpkPI (); break;
    case 0x3e: FEFunction.fpkNPI (); break;
    case 0x3f: FEFunction.fpkPOWER (); break;
    case 0x40: FEFunction.fpkRND (); break;
    case 0x41: FEFunction.fpkSINH (); break;
    case 0x42: FEFunction.fpkCOSH (); break;
    case 0x43: FEFunction.fpkTANH (); break;
    case 0x44: FEFunction.fpkATANH (); break;
    case 0x45: FEFunction.fpkASIN (); break;
    case 0x46: FEFunction.fpkACOS (); break;
    case 0x47: FEFunction.fpkLOG10 (); break;
    case 0x48: FEFunction.fpkLOG2 (); break;
    case 0x49: FEFunction.fpkDFREXP (); break;
    case 0x4a: FEFunction.fpkDLDEXP (); break;
    case 0x4b: FEFunction.fpkDADDONE (); break;
    case 0x4c: FEFunction.fpkDSUBONE (); break;
    case 0x4d: FEFunction.fpkDDIVTWO (); break;
    case 0x4e: FEFunction.fpkDIEECNV (); break;
    case 0x4f: FEFunction.fpkIEEDCNV (); break;
    case 0x50: FEFunction.fpkFVAL (); break;
    case 0x51: FEFunction.fpkFUSING (); break;
    case 0x52: FEFunction.fpkSTOF (); break;
    case 0x53: FEFunction.fpkFTOS (); break;
    case 0x54: FEFunction.fpkFECVT (); break;
    case 0x55: FEFunction.fpkFFCVT (); break;
    case 0x56: FEFunction.fpkFGCVT (); break;
      //case 0x57: break;
    case 0x58: FEFunction.fpkFTST (); break;
    case 0x59: FEFunction.fpkFCMP (); break;
    case 0x5a: FEFunction.fpkFNEG (); break;
    case 0x5b: FEFunction.fpkFADD (); break;
    case 0x5c: FEFunction.fpkFSUB (); break;
    case 0x5d: FEFunction.fpkFMUL (); break;
    case 0x5e: FEFunction.fpkFDIV (); break;
    case 0x5f: FEFunction.fpkFMOD (); break;
    case 0x60: FEFunction.fpkFABS (); break;
    case 0x61: FEFunction.fpkFCEIL (); break;
    case 0x62: FEFunction.fpkFFIX (); break;
    case 0x63: FEFunction.fpkFFLOOR (); break;
    case 0x64: FEFunction.fpkFFRAC (); break;
    case 0x65: FEFunction.fpkFSGN (); break;
    case 0x66: FEFunction.fpkFSIN (); break;
    case 0x67: FEFunction.fpkFCOS (); break;
    case 0x68: FEFunction.fpkFTAN (); break;
    case 0x69: FEFunction.fpkFATAN (); break;
    case 0x6a: FEFunction.fpkFLOG (); break;
    case 0x6b: FEFunction.fpkFEXP (); break;
    case 0x6c: FEFunction.fpkFSQR (); break;
    case 0x6d: FEFunction.fpkFPI (); break;
    case 0x6e: FEFunction.fpkFNPI (); break;
    case 0x6f: FEFunction.fpkFPOWER (); break;
    case 0x70: FEFunction.fpkFRND (); break;
    case 0x71: FEFunction.fpkFSINH (); break;
    case 0x72: FEFunction.fpkFCOSH (); break;
    case 0x73: FEFunction.fpkFTANH (); break;
    case 0x74: FEFunction.fpkFATANH (); break;
    case 0x75: FEFunction.fpkFASIN (); break;
    case 0x76: FEFunction.fpkFACOS (); break;
    case 0x77: FEFunction.fpkFLOG10 (); break;
    case 0x78: FEFunction.fpkFLOG2 (); break;
    case 0x79: FEFunction.fpkFFREXP (); break;
    case 0x7a: FEFunction.fpkFLDEXP (); break;
    case 0x7b: FEFunction.fpkFADDONE (); break;
    case 0x7c: FEFunction.fpkFSUBONE (); break;
    case 0x7d: FEFunction.fpkFDIVTWO (); break;
    case 0x7e: FEFunction.fpkFIEECNV (); break;
    case 0x7f: FEFunction.fpkIEEFCNV (); break;
      //case 0x80: break;
      //case 0x81: break;
      //case 0x82: break;
      //case 0x83: break;
      //case 0x84: break;
      //case 0x85: break;
      //case 0x86: break;
      //case 0x87: break;
      //case 0x88: break;
      //case 0x89: break;
      //case 0x8a: break;
      //case 0x8b: break;
      //case 0x8c: break;
      //case 0x8d: break;
      //case 0x8e: break;
      //case 0x8f: break;
      //case 0x90: break;
      //case 0x91: break;
      //case 0x92: break;
      //case 0x93: break;
      //case 0x94: break;
      //case 0x95: break;
      //case 0x96: break;
      //case 0x97: break;
      //case 0x98: break;
      //case 0x99: break;
      //case 0x9a: break;
      //case 0x9b: break;
      //case 0x9c: break;
      //case 0x9d: break;
      //case 0x9e: break;
      //case 0x9f: break;
      //case 0xa0: break;
      //case 0xa1: break;
      //case 0xa2: break;
      //case 0xa3: break;
      //case 0xa4: break;
      //case 0xa5: break;
      //case 0xa6: break;
      //case 0xa7: break;
      //case 0xa8: break;
      //case 0xa9: break;
      //case 0xaa: break;
      //case 0xab: break;
      //case 0xac: break;
      //case 0xad: break;
      //case 0xae: break;
      //case 0xaf: break;
      //case 0xb0: break;
      //case 0xb1: break;
      //case 0xb2: break;
      //case 0xb3: break;
      //case 0xb4: break;
      //case 0xb5: break;
      //case 0xb6: break;
      //case 0xb7: break;
      //case 0xb8: break;
      //case 0xb9: break;
      //case 0xba: break;
      //case 0xbb: break;
      //case 0xbc: break;
      //case 0xbd: break;
      //case 0xbe: break;
      //case 0xbf: break;
      //case 0xc0: break;
      //case 0xc1: break;
      //case 0xc2: break;
      //case 0xc3: break;
      //case 0xc4: break;
      //case 0xc5: break;
      //case 0xc6: break;
      //case 0xc7: break;
      //case 0xc8: break;
      //case 0xc9: break;
      //case 0xca: break;
      //case 0xcb: break;
      //case 0xcc: break;
      //case 0xcd: break;
      //case 0xce: break;
      //case 0xcf: break;
      //case 0xd0: break;
      //case 0xd1: break;
      //case 0xd2: break;
      //case 0xd3: break;
      //case 0xd4: break;
      //case 0xd5: break;
      //case 0xd6: break;
      //case 0xd7: break;
      //case 0xd8: break;
      //case 0xd9: break;
      //case 0xda: break;
      //case 0xdb: break;
      //case 0xdc: break;
      //case 0xdd: break;
      //case 0xde: break;
      //case 0xdf: break;
    case 0xe0: FEFunction.fpkCLMUL (); break;
    case 0xe1: FEFunction.fpkCLDIV (); break;
    case 0xe2: FEFunction.fpkCLMOD (); break;
    case 0xe3: FEFunction.fpkCUMUL (); break;
    case 0xe4: FEFunction.fpkCUDIV (); break;
    case 0xe5: FEFunction.fpkCUMOD (); break;
    case 0xe6: FEFunction.fpkCLTOD (); break;
    case 0xe7: FEFunction.fpkCDTOL (); break;
    case 0xe8: FEFunction.fpkCLTOF (); break;
    case 0xe9: FEFunction.fpkCFTOL (); break;
    case 0xea: FEFunction.fpkCFTOD (); break;
    case 0xeb: FEFunction.fpkCDTOF (); break;
    case 0xec: FEFunction.fpkCDCMP (); break;
    case 0xed: FEFunction.fpkCDADD (); break;
    case 0xee: FEFunction.fpkCDSUB (); break;
    case 0xef: FEFunction.fpkCDMUL (); break;
    case 0xf0: FEFunction.fpkCDDIV (); break;
    case 0xf1: FEFunction.fpkCDMOD (); break;
    case 0xf2: FEFunction.fpkCFCMP (); break;
    case 0xf3: FEFunction.fpkCFADD (); break;
    case 0xf4: FEFunction.fpkCFSUB (); break;
    case 0xf5: FEFunction.fpkCFMUL (); break;
    case 0xf6: FEFunction.fpkCFDIV (); break;
    case 0xf7: FEFunction.fpkCFMOD (); break;
    case 0xf8: FEFunction.fpkCDTST (); break;
    case 0xf9: FEFunction.fpkCFTST (); break;
    case 0xfa: FEFunction.fpkCDINC (); break;
    case 0xfb: FEFunction.fpkCFINC (); break;
    case 0xfc: FEFunction.fpkCDDEC (); break;
    case 0xfd: FEFunction.fpkCFDEC (); break;
    case 0xfe: FEFunction.fpkFEVARG (); break;
    //case 0xff: FEFunction.fpkFEVECS (); break;  //FLOATn.Xに処理させる
    default:
      XEiJ.mpuCycleCount -= FEFunction.FPK_CLOCK;  //戻す
      irpFline ();
    }
    if (FEFunction.FPK_DEBUG_TRACE) {
      int i = sb.length ();
      XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (sb.append ("  D0="), XEiJ.regRn[0]).append (" D1="), XEiJ.regRn[1]).append (" D2="), XEiJ.regRn[2]).append (" D3="), XEiJ.regRn[3]);
      int l = MainMemory.mmrStrlen (a0, 20);
      sb.append (" (A0)=\"");
      i = sb.length () - i;
      MainMemory.mmrRstr (sb, a0, l).append ("\"\n");
      if (a0 <= XEiJ.regRn[8] && XEiJ.regRn[8] <= a0 + l) {
        for (i += sb.length () + XEiJ.regRn[8] - a0; sb.length () < i; ) {
          sb.append (' ');
        }
        sb.append ('^');
      }
      System.out.println (sb.toString ());
    }
  }  //irpFpack

  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //DOS <data>                                      |A|012346|-|UUUUU|UUUUU|          |1111_111_1dd_ddd_ddd [FLINE #<data>]
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //FLINE #<data>                                   |-|012346|-|UUUUU|UUUUU|          |1111_ddd_ddd_ddd_ddd (line 1111 emulator)
  public static void irpFline () throws M68kException {
    XEiJ.mpuCycleCount += 34;
    if (XEiJ.MPU_INLINE_EXCEPTION) {
      int save_sr = XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
      int sp = XEiJ.regRn[15];
      XEiJ.regSRT1 = XEiJ.mpuTraceFlag = 0;  //srのTビットを消す
      if (XEiJ.regSRS == 0) {  //ユーザモードのとき
        XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
        XEiJ.mpuUSP = sp;  //USPを保存
        sp = XEiJ.mpuISP;  //SSPを復元
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
        } else {
          XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
        }
        if (InstructionBreakPoint.IBP_ON) {
          InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
        }
      }
      int vectorOffset = M68kException.M6E_LINE_1111_EMULATOR << 2;  //vector offset
      XEiJ.regRn[15] = sp -= 8;  //short format
      XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
      XEiJ.busWl (sp + 2, XEiJ.regPC0);  //program counter
      XEiJ.busWw (sp, save_sr);  //status register
      irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
    } else {
      irpException (M68kException.M6E_LINE_1111_EMULATOR, XEiJ.regPC0, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //pcは命令の先頭
    }
  }  //irpFline

  //irpIllegal ()
  //  オペコードの上位10bitで分類されなかった未実装命令
  //  0x4afcのILLEGAL命令はここには来ない
  public static void irpIllegal () throws M68kException {
    if (true) {
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpIllegal

  //z = irpAbcd (x, y)
  //  ABCD
  public static int irpAbcd (int x, int y) {
    int c = XEiJ.regCCR >> 4;
    int t = (x & 0xff) + (y & 0xff) + c;  //仮の結果
    int z = t;  //結果
    if (0x0a <= (x & 0x0f) + (y & 0x0f) + c) {  //ハーフキャリー
      z += 0x10 - 0x0a;
    }
    //XとCはキャリーがあるときセット、さもなくばクリア
    if (0xa0 <= z) {  //キャリー
      z += 0x100 - 0xa0;
      XEiJ.regCCR |= XEiJ.REG_CCR_X | XEiJ.REG_CCR_C;
    } else {
      XEiJ.regCCR &= ~(XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);
    }
    //Zは結果が0でないときクリア、さもなくば変化しない
    z &= 0xff;
    if (z != 0x00) {
      XEiJ.regCCR &= ~XEiJ.REG_CCR_Z;
    }
    if (true) {
      //000/030のときNは結果の最上位ビット
      if ((z & 0x80) != 0) {
        XEiJ.regCCR |= XEiJ.REG_CCR_N;
      } else {
        XEiJ.regCCR &= ~XEiJ.REG_CCR_N;
      }
      //000のときVは補正値の加算でオーバーフローしたときセット、さもなくばクリア
      int a = z - t;  //補正値
      if ((((t ^ z) & (a ^ z)) & 0x80) != 0) {
        XEiJ.regCCR |= XEiJ.REG_CCR_V;
      } else {
        XEiJ.regCCR &= ~XEiJ.REG_CCR_V;
      }
    } else if (false) {
      //000/030のときNは結果の最上位ビット
      if ((z & 0x80) != 0) {
        XEiJ.regCCR |= XEiJ.REG_CCR_N;
      } else {
        XEiJ.regCCR &= ~XEiJ.REG_CCR_N;
      }
      //030のときVはクリア
      XEiJ.regCCR &= ~XEiJ.REG_CCR_V;
    } else {
      //060のときNとVは変化しない
    }
    return z;
  }  //irpAbcd

  //z = irpSbcd (x, y)
  //  SBCD
  public static int irpSbcd (int x, int y) {
    int b = XEiJ.regCCR >> 4;
    int t = (x & 0xff) - (y & 0xff) - b;  //仮の結果
    int z = t;  //結果
    if ((x & 0x0f) - (y & 0x0f) - b < 0) {  //ハーフボロー
      z -= 0x10 - 0x0a;
    }
    //XとCはボローがあるときセット、さもなくばクリア
    if (z < 0) {  //ボロー
      if (t < 0) {
        z -= 0x100 - 0xa0;
      }
      XEiJ.regCCR |= XEiJ.REG_CCR_X | XEiJ.REG_CCR_C;
    } else {
      XEiJ.regCCR &= ~(XEiJ.REG_CCR_X | XEiJ.REG_CCR_C);
    }
    //Zは結果が0でないときクリア、さもなくば変化しない
    z &= 0xff;
    if (z != 0x00) {
      XEiJ.regCCR &= ~XEiJ.REG_CCR_Z;
    }
    if (true) {
      //000/030のときNは結果の最上位ビット
      if ((z & 0x80) != 0) {
        XEiJ.regCCR |= XEiJ.REG_CCR_N;
      } else {
        XEiJ.regCCR &= ~XEiJ.REG_CCR_N;
      }
      //000のときVは補正値の加算でオーバーフローしたときセット、さもなくばクリア
      int a = z - t;  //補正値
      if ((((t ^ z) & (a ^ z)) & 0x80) != 0) {
        XEiJ.regCCR |= XEiJ.REG_CCR_V;
      } else {
        XEiJ.regCCR &= ~XEiJ.REG_CCR_V;
      }
    } else if (false) {
      //000/030のときNは結果の最上位ビット
      if ((z & 0x80) != 0) {
        XEiJ.regCCR |= XEiJ.REG_CCR_N;
      } else {
        XEiJ.regCCR &= ~XEiJ.REG_CCR_N;
      }
      //030のときVはクリア
      XEiJ.regCCR &= ~XEiJ.REG_CCR_V;
    } else {
      //060のときNとVは変化しない
    }
    return z;
  }  //irpSbcd



  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //                                                | |  MPU | |CCin |CCout|addressing|     1st opcode         2nd opcode
  //                           A:alias P:privileged |A|012346|P|XNZVC|XNZVC|DAM+-WXZPI|bbbb_bbb_bbb_bbb_bbb-bbbbbbbbbbbbbbbb
  //------------------------------------------------+-+------+-+-----+-----+----------+-------------------------------------
  //HFSBOOT                                         |-|012346|-|-----|-----|          |0100_111_000_000_000
  //HFSINST                                         |-|012346|-|-----|-----|          |0100_111_000_000_001
  //HFSSTR                                          |-|012346|-|-----|-----|          |0100_111_000_000_010
  //HFSINT                                          |-|012346|-|-----|-----|          |0100_111_000_000_011
  //EMXNOP                                          |-|012346|-|-----|-----|          |0100_111_000_000_100
  //  エミュレータ拡張命令
  public static void irpEmx () throws M68kException {
    switch (XEiJ.regOC & 63) {
    case XEiJ.EMX_OPCODE_HFSBOOT & 63:
      XEiJ.mpuCycleCount += 40;
      if (HFS.hfsIPLBoot ()) {
        //JMP $6800.W
        irpSetPC (0x00006800);
      }
      break;
    case XEiJ.EMX_OPCODE_HFSINST & 63:
      XEiJ.mpuCycleCount += 40;
      HFS.hfsInstall ();
      break;
    case XEiJ.EMX_OPCODE_HFSSTR & 63:
      XEiJ.mpuCycleCount += 40;
      HFS.hfsStrategy ();
      break;
    case XEiJ.EMX_OPCODE_HFSINT & 63:
      XEiJ.mpuCycleCount += 40;
      //XEiJ.mpuClockTime += (int) (TMR_FREQ / 100000L);  //0.01ms
      if (HFS.hfsInterrupt ()) {
        //WAIT
        XEiJ.mpuTraceFlag = 0;  //トレース例外を発生させない
        XEiJ.regPC = XEiJ.regPC0;  //ループ
        XEiJ.mpuClockTime += XEiJ.TMR_FREQ * 4 / 1000000;  //4μs。10MHzのとき40clk
        XEiJ.mpuLastNano += 4000L;
      }
      break;
    case XEiJ.EMX_OPCODE_EMXNOP & 63:
      XEiJ.emxNop ();
      break;
    case XEiJ.EMX_OPCODE_EMXWAIT & 63:
      WaitInstruction.execute ();  //待機命令を実行する
      break;
    default:
      XEiJ.mpuCycleCount += 34;
      M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
      throw M68kException.m6eSignal;
    }
  }  //irpEmx



  //irpSetPC (a)
  //  pcへデータを書き込む
  //  奇数のときはアドレスエラーが発生する
  public static void irpSetPC (int a) throws M68kException {
    if (XEiJ.TEST_BIT_0_SHIFT ? a << 31 - 0 < 0 : (a & 1) != 0) {
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = XEiJ.MPU_WR_READ;
      M68kException.m6eSize = XEiJ.MPU_SS_LONG;
      throw M68kException.m6eSignal;
    }
    if (BranchLog.BLG_ON) {
      BranchLog.blgJump (a);  //分岐ログに分岐レコードを追加する
    } else {
      XEiJ.regPC = a;
    }
  }  //irpSetPC

  //irpSetSR (newSr)
  //  srへデータを書き込む
  //  ori to sr/andi to sr/eori to sr/move to sr/stop/rteで使用される
  //  スーパーバイザモードになっていることを確認してから呼び出すこと
  //  rteではr[15]が指すアドレスからsrとpcを取り出してr[15]を更新してから呼び出すこと
  //  スーパーバイザモード→ユーザモードのときは移行のための処理を行う
  //  新しい割り込みマスクレベルよりも高い割り込み処理の終了をデバイスに通知する
  public static void irpSetSR (int newSr) {
    XEiJ.regSRT1 = XEiJ.REG_SR_T1 & newSr;
    if ((XEiJ.regSRS = XEiJ.REG_SR_S & newSr) == 0) {  //スーパーバイザモード→ユーザモード
      XEiJ.mpuISP = XEiJ.regRn[15];  //XEiJ.mpuISPを保存
      XEiJ.regRn[15] = XEiJ.mpuUSP;  //XEiJ.mpuUSPを復元
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpUserMap;  //ユーザメモリマップに切り替える
      } else {
        XEiJ.busMemoryMap = XEiJ.busUserMap;  //ユーザメモリマップに切り替える
      }
      if (InstructionBreakPoint.IBP_ON) {
        InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1UserMap;
      }
    }
    int t = (XEiJ.mpuIMR = 0x7f >> ((XEiJ.regSRI = XEiJ.REG_SR_I & newSr) >> 8)) & XEiJ.mpuISR;  //XEiJ.mpuISRで1→0とするビット
    if (t != 0) {  //終了する割り込みがあるとき
      XEiJ.mpuISR ^= t;
      //デバイスに割り込み処理の終了を通知する
      if (t == XEiJ.MPU_MFP_INTERRUPT_MASK) {  //MFPのみ
        MC68901.mfpDone ();
      } else if (t == XEiJ.MPU_DMA_INTERRUPT_MASK) {  //DMAのみ
        HD63450.dmaDone ();
      } else if (t == XEiJ.MPU_SCC_INTERRUPT_MASK) {  //SCCのみ
        Z8530.sccDone ();
      } else if (t == XEiJ.MPU_IOI_INTERRUPT_MASK) {  //IOIのみ
        IOInterrupt.ioiDone ();
      } else if (t == XEiJ.MPU_EB2_INTERRUPT_MASK) {  //EB2のみ
        XEiJ.eb2Done ();
      } else {  //SYSのみまたは複数
        if (XEiJ.TEST_BIT_1_SHIFT ? t << 24 + XEiJ.MPU_MFP_INTERRUPT_LEVEL < 0 : (t & XEiJ.MPU_MFP_INTERRUPT_MASK) != 0) {
          MC68901.mfpDone ();
        }
        if (t << 24 + XEiJ.MPU_DMA_INTERRUPT_LEVEL < 0) {  //(t & XEiJ.MPU_DMA_INTERRUPT_MASK) != 0
          HD63450.dmaDone ();
        }
        if (XEiJ.TEST_BIT_2_SHIFT ? t << 24 + XEiJ.MPU_SCC_INTERRUPT_LEVEL < 0 : (t & XEiJ.MPU_SCC_INTERRUPT_MASK) != 0) {
          Z8530.sccDone ();
        }
        if (t << 24 + XEiJ.MPU_IOI_INTERRUPT_LEVEL < 0) {  //(t & XEiJ.MPU_IOI_INTERRUPT_MASK) != 0
          IOInterrupt.ioiDone ();
        }
        if (t << 24 + XEiJ.MPU_EB2_INTERRUPT_LEVEL < 0) {  //(t & XEiJ.MPU_EB2_INTERRUPT_MASK) != 0
          XEiJ.eb2Done ();
        }
        if (XEiJ.TEST_BIT_0_SHIFT ? t << 24 + XEiJ.MPU_SYS_INTERRUPT_LEVEL < 0 : (t & XEiJ.MPU_SYS_INTERRUPT_MASK) != 0) {
          XEiJ.sysDone ();
        }
      }
    }
    XEiJ.mpuIMR |= ~XEiJ.mpuISR & XEiJ.MPU_SYS_INTERRUPT_MASK;  //割り込みマスクレベルが7のときレベル7割り込みの処理中でなければレベル7割り込みを許可する
    XEiJ.regCCR = XEiJ.REG_CCR_MASK & newSr;
  }  //irpSetSR

  //irpInterrupt (vectorNumber, level)
  //  割り込み処理を開始する
  public static void irpInterrupt (int vectorNumber, int level) throws M68kException {
    if (XEiJ.regOC == 0b0100_111_001_110_010) {  //最後に実行した命令はSTOP命令
      XEiJ.regPC = XEiJ.regPC0 + 4;  //次の命令に進む
    }
    XEiJ.mpuClockTime += XEiJ.mpuModifiedUnit * 44;
    int save_sr = XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR;
    XEiJ.regSRI = level << 8;  //割り込みマスクを要求されたレベルに変更する
    XEiJ.mpuIMR = 0x7f >> level;
    XEiJ.mpuISR |= 0x80 >> level;
    int sp = XEiJ.regRn[15];
    XEiJ.regSRT1 = 0;  //srのTビットを消す
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
      XEiJ.mpuUSP = sp;  //USPを保存
      sp = XEiJ.mpuISP;  //SSPを復元
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
      } else {
        XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
      }
      if (InstructionBreakPoint.IBP_ON) {
        InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
      }
    }
    int vectorOffset = vectorNumber << 2;  //vector offset
    XEiJ.regRn[15] = sp -= 8;  //short format
    XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
    XEiJ.busWl (sp + 2, XEiJ.regPC);  //program counter
    XEiJ.busWw (sp, save_sr);  //status register
    if (BranchLog.BLG_ON) {
      XEiJ.regPC0 = XEiJ.regPC;  //rteによる割り込み終了と同時に次の割り込みを受け付けたとき間でpc0を更新しないと2番目の分岐レコードの終了アドレスが1番目と同じになっておかしな分岐レコードができてしまう
    }
    irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
  }  //irpInterrupt

  //irpException (vectorNumber, save_pc, save_sr)
  //  例外処理を開始する
  //  スタックへのプッシュ、ベクタの取り出し、ジャンプのいずれかでバスエラーまたはアドレスエラーが発生する場合がある
  public static void irpException (int vectorNumber, int save_pc, int save_sr) throws M68kException {
    int sp = XEiJ.regRn[15];
    XEiJ.regSRT1 = XEiJ.mpuTraceFlag = 0;  //srのTビットを消す
    if (XEiJ.regSRS == 0) {  //ユーザモードのとき
      XEiJ.regSRS = XEiJ.REG_SR_S;  //スーパーバイザモードへ移行する
      XEiJ.mpuUSP = sp;  //USPを保存
      sp = XEiJ.mpuISP;  //SSPを復元
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;  //スーパーバイザメモリマップに切り替える
      } else {
        XEiJ.busMemoryMap = XEiJ.busSuperMap;  //スーパーバイザメモリマップに切り替える
      }
      if (InstructionBreakPoint.IBP_ON) {
        InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
      }
    }
    int vectorOffset = vectorNumber << 2;  //vector offset
    XEiJ.regRn[15] = sp -= 8;  //short format
    XEiJ.busWw (sp + 6, 0x0000 | vectorOffset);  //format and vector offset
    XEiJ.busWl (sp + 2, save_pc);  //program counter
    XEiJ.busWw (sp, save_sr);  //status register
    irpSetPC (XEiJ.busRlsf (XEiJ.mpuVBR + vectorOffset));  //例外ベクタを取り出してジャンプする
  }  //irpException



  //a = efaAnyByte (ea)  //|  M+-WXZPI|
  //  任意のモードのバイトオペランドの実効アドレスを求める
  //  (A7)+と-(A7)はA7を奇偶に関わらず2変化させ、跨いだワードの上位バイト(アドレスの小さい方)を参照する
  //  #<data>はオペコードに続くワードの下位バイトを参照する。上位バイトは不定なので参照してはならない
  @SuppressWarnings ("fallthrough") public static int efaAnyByte (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8]++;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9]++;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10]++;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11]++;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12]++;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13]++;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14]++;
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b011_000 - 8)]++;
      }
    case 0b011_111:  //(A7)+
      XEiJ.mpuCycleCount += 4;
      return (XEiJ.regRn[15] += 2) - 2;
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[10];
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[11];
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[12];
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[13];
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[14];
      } else {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ea - (0b100_000 - 8)];
      }
    case 0b100_111:  //-(A7)
      XEiJ.mpuCycleCount += 6;
      return XEiJ.regRn[15] -= 2;
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_100:  //#<data>
      XEiJ.mpuCycleCount += 4;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regPC += 2) - 1;  //下位バイト
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return t + 1;  //下位バイト
      }
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaAnyByte

  //a = efaMemByte (ea)  //|  M+-WXZP |
  //  メモリモードのバイトオペランドの実効アドレスを求める
  //  efaAnyByteとの違いは#<data>がないこと
  @SuppressWarnings ("fallthrough") public static int efaMemByte (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8]++;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9]++;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10]++;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11]++;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12]++;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13]++;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14]++;
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b011_000 - 8)]++;
      }
    case 0b011_111:  //(A7)+
      XEiJ.mpuCycleCount += 4;
      return (XEiJ.regRn[15] += 2) - 2;
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[10];
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[11];
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[12];
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[13];
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[14];
      } else {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ea - (0b100_000 - 8)];
      }
    case 0b100_111:  //-(A7)
      XEiJ.mpuCycleCount += 6;
      return XEiJ.regRn[15] -= 2;
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMemByte

  //a = efaMltByte (ea)  //|  M+-WXZ  |
  //  メモリ可変モードのバイトオペランドの実効アドレスを求める
  //  efaMemByteとの違いは(d16,PC)と(d8,PC,Rn.wl)がないこと
  @SuppressWarnings ("fallthrough") public static int efaMltByte (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8]++;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9]++;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10]++;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11]++;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12]++;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13]++;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14]++;
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b011_000 - 8)]++;
      }
    case 0b011_111:  //(A7)+
      XEiJ.mpuCycleCount += 4;
      return (XEiJ.regRn[15] += 2) - 2;
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[10];
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[11];
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[12];
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[13];
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[14];
      } else {
        XEiJ.mpuCycleCount += 6;
        return --XEiJ.regRn[ea - (0b100_000 - 8)];
      }
    case 0b100_111:  //-(A7)
      XEiJ.mpuCycleCount += 6;
      return XEiJ.regRn[15] -= 2;
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMltByte

  //a = efaCntByte (ea)  //|  M  WXZP |
  //  制御モードのロングオペランドの実効アドレスを求める
  //  efaMemByteとの違いは(Ar)+と-(Ar)がないこと
  @SuppressWarnings ("fallthrough") public static int efaCntByte (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaCntByte

  //a = efaAnyWord (ea)  //|  M+-WXZPI|
  //  任意のモードのワードオペランドの実効アドレスを求める
  //  efaAnyByteとの違いは(Ar)+と-(Ar)がArを2変化させることと、(A7)+と-(A7)と#<data>の特別な動作がないこと
  @SuppressWarnings ("fallthrough") public static int efaAnyWord (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ 8] += 2) - 2;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ 9] += 2) - 2;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[10] += 2) - 2;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[11] += 2) - 2;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[12] += 2) - 2;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[13] += 2) - 2;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[14] += 2) - 2;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[15] += 2) - 2;
      } else {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 2) - 2;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ 8] -= 2;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ 9] -= 2;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[10] -= 2;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[11] -= 2;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[12] -= 2;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[13] -= 2;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[14] -= 2;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[15] -= 2;
      } else {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 2;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_100:  //#<data>
      XEiJ.mpuCycleCount += 4;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regPC += 2) - 2;
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return t;
      }
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaAnyWord

  //a = efaMemWord (ea)  //|  M+-WXZP |
  //  メモリモードのワードオペランドの実効アドレスを求める
  //  efaAnyWordとの違いは#<data>がないこと
  @SuppressWarnings ("fallthrough") public static int efaMemWord (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ 8] += 2) - 2;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ 9] += 2) - 2;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[10] += 2) - 2;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[11] += 2) - 2;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[12] += 2) - 2;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[13] += 2) - 2;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[14] += 2) - 2;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[15] += 2) - 2;
      } else {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 2) - 2;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ 8] -= 2;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ 9] -= 2;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[10] -= 2;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[11] -= 2;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[12] -= 2;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[13] -= 2;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[14] -= 2;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[15] -= 2;
      } else {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 2;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMemWord

  //a = efaMltWord (ea)  //|  M+-WXZ  |
  //  メモリ可変モードのワードオペランドの実効アドレスを求める
  //  efaMemWordとの違いは(d16,PC)と(d8,PC,Rn.wl)がないこと
  @SuppressWarnings ("fallthrough") public static int efaMltWord (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ 8] += 2) - 2;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ 9] += 2) - 2;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[10] += 2) - 2;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[11] += 2) - 2;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[12] += 2) - 2;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[13] += 2) - 2;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[14] += 2) - 2;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[15] += 2) - 2;
      } else {
        XEiJ.mpuCycleCount += 4;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 2) - 2;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ 8] -= 2;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ 9] -= 2;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[10] -= 2;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[11] -= 2;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[12] -= 2;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[13] -= 2;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[14] -= 2;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[15] -= 2;
      } else {
        XEiJ.mpuCycleCount += 6;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 2;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMltWord

  //a = efaCntWord (ea)  //|  M  WXZP |
  //  制御モードのワードオペランドの実効アドレスを求める
  //  efaMemWordとの違いは(Ar)+と-(Ar)がないこと
  @SuppressWarnings ("fallthrough") public static int efaCntWord (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaCntWord

  //a = efaCltWord (ea)  //|  M  WXZ  |
  //  制御可変モードのワードオペランドの実効アドレスを求める
  //  efaCntWordとの違いは(d16,PC)と(d8,PC,Rn.wl)がないこと
  @SuppressWarnings ("fallthrough") public static int efaCltWord (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaCltWord

  //a = efaAnyLong (ea)  //|  M+-WXZPI|
  //  任意のモードのロングオペランドの実効アドレスを求める
  //  efaAnyWordとの違いは(Ar)+と-(Ar)がArを4変化させることと、#<data>がPCを4変化させることと、
  //  オペランドのアクセスが1ワード増える分の4サイクルが追加されていること
  @SuppressWarnings ("fallthrough") public static int efaAnyLong (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ 8] += 4) - 4;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ 9] += 4) - 4;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[10] += 4) - 4;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[11] += 4) - 4;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[12] += 4) - 4;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[13] += 4) - 4;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[14] += 4) - 4;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[15] += 4) - 4;
      } else {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 4) - 4;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ 8] -= 4;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ 9] -= 4;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[10] -= 4;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[11] -= 4;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[12] -= 4;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[13] -= 4;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[14] -= 4;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[15] -= 4;
      } else {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 4;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 16;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 12;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_100:  //#<data>
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regPC += 4) - 4;
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 4;
        return t;
      }
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaAnyLong

  //a = efaMemLong (ea)  //|  M+-WXZP |
  //  メモリモードのロングオペランドの実効アドレスを求める
  //  efaAnyLongとの違いは#<data>がないこと
  @SuppressWarnings ("fallthrough") public static int efaMemLong (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ 8] += 4) - 4;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ 9] += 4) - 4;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[10] += 4) - 4;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[11] += 4) - 4;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[12] += 4) - 4;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[13] += 4) - 4;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[14] += 4) - 4;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[15] += 4) - 4;
      } else {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 4) - 4;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ 8] -= 4;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ 9] -= 4;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[10] -= 4;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[11] -= 4;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[12] -= 4;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[13] -= 4;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[14] -= 4;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[15] -= 4;
      } else {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 4;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 16;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 12;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMemLong

  //a = efaMltLong (ea)  //|  M+-WXZ  |
  //  メモリ可変モードのロングオペランドの実効アドレスを求める
  //  efaMemLongとの違いは(d16,PC)と(d8,PC,Rn.wl)がないこと
  @SuppressWarnings ("fallthrough") public static int efaMltLong (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ 8] += 4) - 4;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ 9] += 4) - 4;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[10] += 4) - 4;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[11] += 4) - 4;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[12] += 4) - 4;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[13] += 4) - 4;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[14] += 4) - 4;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[15] += 4) - 4;
      } else {
        XEiJ.mpuCycleCount += 8;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 4) - 4;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ 8] -= 4;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ 9] -= 4;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[10] -= 4;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[11] -= 4;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[12] -= 4;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[13] -= 4;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[14] -= 4;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[15] -= 4;
      } else {
        XEiJ.mpuCycleCount += 10;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 4;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 16;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMltLong

  //a = efaCntLong (ea)  //|  M  WXZP |
  //  制御モードのロングオペランドの実効アドレスを求める
  //  efaMemLongとの違いは(Ar)+と-(Ar)がないこと
  @SuppressWarnings ("fallthrough") public static int efaCntLong (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 16;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 12;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaCntLong

  //a = efaCltLong (ea)  //|  M  WXZ  |
  //  制御可変モードのワードオペランドの実効アドレスを求める
  //  efaCntLongとの違いは(d16,PC)と(d8,PC,Rn.wl)がないこと
  @SuppressWarnings ("fallthrough") public static int efaCltLong (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 16;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaCltLong

  //a = efaAnyQuad (ea)  //|  M+-WXZPI|
  //  任意のモードのクワッドオペランドの実効アドレスを求める
  //  efaAnyLongとの違いは(Ar)+と-(Ar)がArを8変化させることと、#<data>がPCを8変化させることと、
  //  オペランドのアクセスが2ワード増える分の8サイクルが追加されていること
  @SuppressWarnings ("fallthrough") public static int efaAnyQuad (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[ 8] += 8) - 8;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[ 9] += 8) - 8;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[10] += 8) - 8;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[11] += 8) - 8;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[12] += 8) - 8;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[13] += 8) - 8;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[14] += 8) - 8;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[15] += 8) - 8;
      } else {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 8) - 8;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[ 8] -= 8;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[ 9] -= 8;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[10] -= 8;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[11] -= 8;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[12] -= 8;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[13] -= 8;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[14] -= 8;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[15] -= 8;
      } else {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 8;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 20;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 22;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 20;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 24;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 20;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 22;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_100:  //#<data>
      XEiJ.mpuCycleCount += 16;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regPC += 8) - 8;
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 8;
        return t;
      }
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaAnyQuad

  //a = efaMltQuad (ea)  //|  M+-WXZ  |
  //  メモリ可変モードのクワッドオペランドの実効アドレスを求める
  //  efaMltLongとの違いは(Ar)+と-(Ar)がArを8変化させることと、#<data>がPCを8変化させることと、
  //  オペランドのアクセスが2ワード増える分の8サイクルが追加されていること
  @SuppressWarnings ("fallthrough") public static int efaMltQuad (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 16;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[ 8] += 8) - 8;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[ 9] += 8) - 8;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[10] += 8) - 8;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[11] += 8) - 8;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[12] += 8) - 8;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[13] += 8) - 8;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[14] += 8) - 8;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[15] += 8) - 8;
      } else {
        XEiJ.mpuCycleCount += 16;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 8) - 8;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[ 8] -= 8;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[ 9] -= 8;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[10] -= 8;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[11] -= 8;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[12] -= 8;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[13] -= 8;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[14] -= 8;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[15] -= 8;
      } else {
        XEiJ.mpuCycleCount += 18;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 8;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 20;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 22;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 20;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 24;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMltQuad

  //a = efaAnyExtd (ea)  //|  M+-WXZPI|
  //  任意のモードのエクステンデッドオペランドの実効アドレスを求める
  //  efaAnyQuadとの違いは(Ar)+と-(Ar)がArを12変化させることと、#<data>がPCを12変化させることと、
  //  オペランドのアクセスが2ワード増える分の8サイクルが追加されていること
  @SuppressWarnings ("fallthrough") public static int efaAnyExtd (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[ 8] += 12) - 12;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[ 9] += 12) - 12;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[10] += 12) - 12;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[11] += 12) - 12;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[12] += 12) - 12;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[13] += 12) - 12;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[14] += 12) - 12;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[15] += 12) - 12;
      } else {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 12) - 12;
      }
    case 0b100_000:  //-(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[ 8] -= 12;
      }
      //fallthrough
    case 0b100_001:  //-(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[ 9] -= 12;
      }
      //fallthrough
    case 0b100_010:  //-(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[10] -= 12;
      }
      //fallthrough
    case 0b100_011:  //-(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[11] -= 12;
      }
      //fallthrough
    case 0b100_100:  //-(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[12] -= 12;
      }
      //fallthrough
    case 0b100_101:  //-(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[13] -= 12;
      }
      //fallthrough
    case 0b100_110:  //-(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[14] -= 12;
      }
      //fallthrough
    case 0b100_111:  //-(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[15] -= 12;
      } else {
        XEiJ.mpuCycleCount += 26;
        return XEiJ.regRn[ea - (0b100_000 - 8)] -= 12;
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 28;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 30;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 28;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 32;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 28;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 30;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_100:  //#<data>
      XEiJ.mpuCycleCount += 24;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regPC += 12) - 12;
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 12;
        return t;
      }
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaAnyExtd

  //a = efaMltExtd (ea)  //|  M+-WXZ  |
  //  メモリ可変モードのエクステンデッドオペランドの実効アドレスを求める
  //  efaMltQuadとの違いは(Ar)+と-(Ar)がArを12変化させることと、#<data>がPCを12変化させることと、
  //  オペランドのアクセスが2ワード増える分の8サイクルが追加されていること
  @SuppressWarnings ("fallthrough") public static int efaMltExtd (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 24;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b011_000:  //(A0)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[ 8] += 12) - 12;
      }
      //fallthrough
    case 0b011_001:  //(A1)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[ 9] += 12) - 12;
      }
      //fallthrough
    case 0b011_010:  //(A2)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[10] += 12) - 12;
      }
      //fallthrough
    case 0b011_011:  //(A3)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[11] += 12) - 12;
      }
      //fallthrough
    case 0b011_100:  //(A4)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[12] += 12) - 12;
      }
      //fallthrough
    case 0b011_101:  //(A5)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[13] += 12) - 12;
      }
      //fallthrough
    case 0b011_110:  //(A6)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[14] += 12) - 12;
      }
      //fallthrough
    case 0b011_111:  //(A7)+
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[15] += 12) - 12;
      } else {
        XEiJ.mpuCycleCount += 24;
        return (XEiJ.regRn[ea - (0b011_000 - 8)] += 12) - 12;
      }
    case 0b100_000:  //-(A0)
    case 0b100_001:  //-(A1)
    case 0b100_010:  //-(A2)
    case 0b100_011:  //-(A3)
    case 0b100_100:  //-(A4)
    case 0b100_101:  //-(A5)
    case 0b100_110:  //-(A6)
    case 0b100_111:  //-(A7)
      XEiJ.mpuCycleCount += 26;
      return XEiJ.regRn[ea - (0b100_000 - 8)] -= 12;
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 28;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 30;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 28;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 32;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaMltExtd

  //a = efaLeaPea (ea)  //|  M  WXZP |
  //  LEA命令とPEA命令のオペランドの実効アドレスを求める
  //  efaCntWordとの違いはサイクル数のみ
  //  LEA命令のベースサイクル数4を含んでいるのでLEA命令ではベースサイクル数を加えなくてよい
  //  PEA命令のベースサイクル数は12-4=8
  @SuppressWarnings ("fallthrough") public static int efaLeaPea (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 4;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 8;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 12;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 8;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 8;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 12;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaLeaPea

  //a = efaJmpJsr (ea)  //|  M  WXZP |
  //  JMP命令とJSR命令のオペランドの実効アドレスを求める
  //  efaCntWordとの違いはサイクル数のみ
  //  JMP命令のベースサイクル数8を含んでいるのでJMP命令ではベースサイクル数を加えなくてよい
  //  JSR命令のベースサイクル数は16-8=8
  @SuppressWarnings ("fallthrough") public static int efaJmpJsr (int ea) throws M68kException {
    int t, w;
    switch (ea) {
    case 0b010_000:  //(A0)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 8];
      }
      //fallthrough
    case 0b010_001:  //(A1)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ 9];
      }
      //fallthrough
    case 0b010_010:  //(A2)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[10];
      }
      //fallthrough
    case 0b010_011:  //(A3)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[11];
      }
      //fallthrough
    case 0b010_100:  //(A4)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[12];
      }
      //fallthrough
    case 0b010_101:  //(A5)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[13];
      }
      //fallthrough
    case 0b010_110:  //(A6)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[14];
      }
      //fallthrough
    case 0b010_111:  //(A7)
      if (XEiJ.EFA_SEPARATE_AR) {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[15];
      } else {
        XEiJ.mpuCycleCount += 8;
        return XEiJ.regRn[ea - (0b010_000 - 8)];
      }
    case 0b101_000:  //(d16,A0)
    case 0b101_001:  //(d16,A1)
    case 0b101_010:  //(d16,A2)
    case 0b101_011:  //(d16,A3)
    case 0b101_100:  //(d16,A4)
    case 0b101_101:  //(d16,A5)
    case 0b101_110:  //(d16,A6)
    case 0b101_111:  //(d16,A7)
      XEiJ.mpuCycleCount += 10;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse ((XEiJ.regPC += 2) - 2));  //pcws。ワードディスプレースメント
      } else {
        t = XEiJ.regPC;
        XEiJ.regPC = t + 2;
        return (XEiJ.regRn[ea - (0b101_000 - 8)]  //ベースレジスタ
                + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
      }
    case 0b110_000:  //(d8,A0,Rn.wl)
    case 0b110_001:  //(d8,A1,Rn.wl)
    case 0b110_010:  //(d8,A2,Rn.wl)
    case 0b110_011:  //(d8,A3,Rn.wl)
    case 0b110_100:  //(d8,A4,Rn.wl)
    case 0b110_101:  //(d8,A5,Rn.wl)
    case 0b110_110:  //(d8,A6,Rn.wl)
    case 0b110_111:  //(d8,A7,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      if (XEiJ.MPU_COMPOUND_POSTINCREMENT) {
        w = XEiJ.busRwze ((XEiJ.regPC += 2) - 2);  //pcwz。拡張ワード
      } else {
        w = XEiJ.regPC;
        XEiJ.regPC = w + 2;
        w = XEiJ.busRwze (w);  //pcwz。拡張ワード
      }
      return (XEiJ.regRn[ea - (0b110_000 - 8)]  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    case 0b111_000:  //(xxx).W
      XEiJ.mpuCycleCount += 10;
      return XEiJ.busRwse ((XEiJ.regPC += 2) - 2);  //pcws
    case 0b111_001:  //(xxx).L
      XEiJ.mpuCycleCount += 12;
      return XEiJ.busRlse ((XEiJ.regPC += 4) - 4);  //pcls
    case 0b111_010:  //(d16,PC)
      XEiJ.mpuCycleCount += 10;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      return (t  //ベースレジスタ
              + XEiJ.busRwse (t));  //pcws。ワードディスプレースメント
    case 0b111_011:  //(d8,PC,Rn.wl)
      XEiJ.mpuCycleCount += 14;
      t = XEiJ.regPC;
      XEiJ.regPC = t + 2;
      w = XEiJ.busRwze (t);  //pcwz。拡張ワード
      return (t  //ベースレジスタ
              + (byte) w  //バイトディスプレースメント
              + (w << 31 - 11 >= 0 ? (short) XEiJ.regRn[w >> 12] :  //ワードインデックス
                 XEiJ.regRn[w >> 12]));  //ロングインデックス
    }  //switch
    XEiJ.mpuCycleCount += 34;
    M68kException.m6eNumber = M68kException.M6E_ILLEGAL_INSTRUCTION;
    throw M68kException.m6eSignal;
  }  //efaJmpJsr



}  //class MC68010



