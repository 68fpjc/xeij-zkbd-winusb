//========================================================================================
//  InstructionBenchmark.java
//    en:Instruction Benchmark
//    ja:命令ベンチマーク
//  Copyright (C) 2003-2021 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  make test PARAM="-bench=all"
//  Bitrev                  1.43ns  0x0f801f3c
//  Byterev                 0.20ns  0x782750ec
//  BtstReg                 3.62ns  0x764fd938
//  MoveToDRWord            3.31ns  0x06d60d1c
//  TstByte                 1.98ns  0x2c8154cc
//  SubToRegByte            4.73ns  0x7904466e
//  SubToRegWord            4.73ns  0xc7f21ecc
//  SubToRegLong            3.63ns  0xeb91420d
//  AddToRegByte            4.81ns  0xa71b4bb9
//  AddToRegWord            4.80ns  0x94701f5f
//  AddToRegLong            3.90ns  0x91d02c44
//  AddToMemByte            9.46ns  0x75421c12
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class InstructionBenchmark {

  public static final int IRB_N = 100000000;
  public static final int IRB_M = 5;

  public static void irbBench (String choice) {
    XEiJ.mpuInit ();
    if (InstructionBreakPoint.IBP_ON) {
      InstructionBreakPoint.ibpInit ();  //IBP 命令ブレークポイント
    }
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpInit ();  //DBP データブレークポイント
    }
    XEiJ.busInit ();  //BUS バスコントローラ
    MainMemory.mmrInit ();  //MMR メインメモリ

    boolean all = choice.equals ("all");
    try {
      for (IRBEnum irb : IRBEnum.values ()) {
        String name = irb.name ();
        if (all || name.equals (choice)) {
          int sum = 0;
          long best = 0x7fffffffffffffffL;
          for (int i = 0; i < IRB_M; i++) {
            long t0 = System.nanoTime ();
            sum = irb.bench (false);
            long t1 = System.nanoTime ();
            sum = irb.bench (true);
            t0 = System.nanoTime ();
            sum = irb.bench (false);
            t1 = System.nanoTime ();
            sum = irb.bench (true);
            long t2 = System.nanoTime ();
            best = Math.min (best, (t2 - t1) - (t1 - t0));
          }
          System.out.printf ("%-20s  %6.2fns  0x%08x\n", name, best / (double) IRB_N, sum);
        }
      }
    } catch (M68kException e) {
    }
  }  //irbBench(String)

}  //class InstructionBenchmark



enum IRBEnum {

  Bitrev {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b0000_000_011_000_000 | 0 << 0;  //BITREV.L D0
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpCmp2Chk2Byte ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  Byterev {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b0000_001_011_000_000 | 0 << 0;  //BYTEREV.L D0
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpCmp2Chk2Word ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  BtstReg {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b0000_000_100_000_000 | 1 << 9 | XEiJ.EA_DR | 0 << 0;  //BTST.L D1,D0
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regRn[1] = i << -i | i >>> i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpBtstReg ();
         }
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  MoveToDRWord {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b0011_000_000_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //MOVE.W D1,D0
       for (int i = 0; i < InstructionBenchmark.IRB_N / 2; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regRn[1] = i << -i | i >>> i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpMoveToDRWord ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       XEiJ.regOC = 0b0011_000_000_000_000 | 0 << 9 | XEiJ.EA_MM | 0 << 0;  //MOVE.W (A0),D0
       XEiJ.regRn[8] = 0x00200000;
       for (int i = 0; i < InstructionBenchmark.IRB_N / 2; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         int t = i << -i | i >>> i;
         MainMemory.mmrM8[0x00200000] = (byte) (t >> 8);
         MainMemory.mmrM8[0x00200001] = (byte) t;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpMoveToDRWord ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  TstByte {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b0100_101_000_000_000;  //TST.B D0
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpTstByte ();
         }
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  TstLong {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b0100_101_010_000_000;  //TST.L D0
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpTstLong ();
         }
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  SubToRegByte {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1001_000_000_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //SUB.B D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpSubToRegByte ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },
  SubToRegWord {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1001_000_001_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //SUB.W D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpSubToRegWord ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },
  SubToRegLong {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1001_000_010_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //SUB.L D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpSubToRegLong ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  AddToRegByte {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1101_000_000_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //ADD.B D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpAddToRegByte ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },
  AddToRegWord {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1101_000_001_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //ADD.W D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpAddToRegWord ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },
  AddToRegLong {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1101_000_010_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //ADD.L D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpAddToRegLong ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  },

  AddToMemByte {
    @Override public int bench (boolean f) throws M68kException {
       int sum = 0;
       XEiJ.regOC = 0b1101_000_100_000_000 | 0 << 9 | XEiJ.EA_DR | 1 << 0;  //ADDX.B D1,D0
       XEiJ.regRn[0] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N / 2; i++) {
         XEiJ.regRn[1] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpAddToMemByte ();
         }
         sum = sum * 3 + XEiJ.regRn[0];
         sum = sum * 3 + XEiJ.regCCR;
       }
       XEiJ.regOC = 0b1101_000_100_000_000 | 0 << 9 | XEiJ.EA_MM | 0 << 0;  //ADD.B D0,(A0)
       XEiJ.regRn[8] = 0x00200000;
       MainMemory.mmrM8[0x00200000] = 0;
       for (int i = 0; i < InstructionBenchmark.IRB_N / 2; i++) {
         XEiJ.regRn[0] = i << i | i >>> -i;
         XEiJ.regCCR = XEiJ.REG_CCR_MASK & i;
         if (f) {
           MC68000.irpAddToMemByte ();
         }
         sum = sum * 3 + (MainMemory.mmrM8[0x00200000] & 255);
         sum = sum * 3 + XEiJ.regCCR;
       }
       return sum;
     }
  };

  public abstract int bench (boolean f) throws M68kException;

}  //enum IRBEnum



