//========================================================================================
//  IOInterrupt.java
//    en:I/O Interrupt
//    ja:I/O割り込み
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

public class IOInterrupt {

  //デバッグ
  public static final boolean IOI_DEBUG_TRACE = false;

  //ポート
  public static final int IOI_STATUS = 0x00e9c001;
  public static final int IOI_VECTOR = 0x00e9c003;

  //割り込み信号
  public static final int IOI_SPC_SIGNAL = 0x4000;
  public static final int IOI_FDC_SIGNAL = 0x0080;
  public static final int IOI_FDD_SIGNAL = 0x0040;
  public static final int IOI_PRN_SIGNAL = 0x0020;
  public static final int IOI_HDC_SIGNAL = 0x0010;

  //割り込み要求
  public static final int IOI_SPC_REQUEST = 0x4000;
  public static final int IOI_HDC_REQUEST = 0x0008;
  public static final int IOI_FDC_REQUEST = 0x0004;
  public static final int IOI_FDD_REQUEST = 0x0002;
  public static final int IOI_PRN_REQUEST = 0x0001;

  //割り込みベクタ
  public static final int IOI_SPC_VECTOR = 0x6c;  //内蔵SCSI
  public static final int IOI_FDC_VECTOR = 0x00;
  public static final int IOI_FDD_VECTOR = 0x01;
  public static final int IOI_HDC_VECTOR = 0x02;
  public static final int IOI_PRN_VECTOR = 0x03;

  //割り込み許可
  //  割り込みステータスレジスタの下位4ビット
  //  0b00010000_00000000  1=SPC割り込み許可。SPC割り込みは常に許可
  //  0b00000000_00001000  1=HDC割り込み許可
  //  0b00000000_00000100  1=FDC割り込み許可
  //  0b00000000_00000010  1=FDD割り込み許可
  //  0b00000000_00000001  1=PRN割り込み許可
  public static int ioiEnable;  //割り込み許可

  //割り込み処理中
  //  現在処理中の割り込みを示す
  //  0b00010000_00000000  1=SPC割り込み処理中
  //  0b00000000_00001000  1=HDC割り込み処理中
  //  0b00000000_00000100  1=FDC割り込み処理中
  //  0b00000000_00000010  1=FDD割り込み処理中
  //  0b00000000_00000001  1=PRN割り込み処理中
  public static int ioiInService;  //割り込み処理中

  //割り込み要求
  //  0b00010000_00000000  1=SPC割り込み要求あり
  //  0b00000000_00001000  1=HDC割り込み要求あり
  //  0b00000000_00000100  1=FDC割り込み要求あり
  //  0b00000000_00000010  1=FDD割り込み要求あり
  //  0b00000000_00000001  1=PRN割り込み要求あり
  public static int ioiRequest;  //割り込み要求

  //割り込み信号
  //  割り込みステータスレジスタの上位4ビット
  //  0b00010000_00000000  1=SPC割り込み要求あり
  //  0b00000000_10000000  1=FDC割り込み要求あり。リザルトステータス読み取り要求、コマンド起動要求
  //  0b00000000_01000000  1=FDD割り込み要求あり。メディア挿入、メディア排出
  //  0b00000000_00100000  1=プリンタレディ。1のとき0x00e8c001にデータをセットしてプリンタストローブ(0x00e8c003のbit0)を0→1で出力
  //  0b00000000_00010000  1=HDC割り込み要求あり。コマンド終了
  public static int ioiSignal;  //割り込み信号

  //割り込みベクタ
  //  下位2ビットは常に0
  public static int ioiVector;  //割り込みベクタ

  //ioiInit ()
  public static void ioiInit () {
  }

  //ioiReset ()
  //  I/O割り込みをリセットする
  public static void ioiReset () {
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiReset()\n", XEiJ.regPC0);
    }
    ioiEnable = IOI_SPC_REQUEST;  //SPC割り込みは常に許可
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiEnable=0x%04x\n", ioiEnable);
    }
    ioiInService = 0;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiInService=0x%04x\n", ioiInService);
    }
    ioiRequest = 0;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
    }
    ioiSignal = PrinterPort.prnOnlineOn ? IOI_PRN_SIGNAL : 0;  //オンラインのときはレディ、オフラインのときはビジー
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
    }
    ioiVector = 0;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiVector=0x%04x\n", ioiVector);
    }
  }  //ioiReset()

  //  割り込み信号が変化したとき
  //    デバイスがfallとriseで割り込み信号を操作する
  //    割り込み信号が0→1のとき割り込み許可が1ならば割り込み要求を1にする
  //    割り込み要求が0→1のデバイスがあるときMPUに割り込みを要求する
  //
  //  MPUが割り込みを受け付けたとき
  //    SPC→FDC→FDD→HDC→PRNの優先順位で割り込み要求が1のデバイスを選択する
  //    選択したデバイスの割り込み要求を0にする
  //    選択したデバイスの割り込み処理中を1にする
  //    選択したデバイスの割り込みベクタを返す
  //
  //  MPUの割り込み処理が終了したとき
  //    割り込み処理中のデバイスを選択する
  //    選択したデバイスの割り込み処理中を0にする
  //    割り込み要求が1のデバイスが残っていたら再度MPUに割り込みを要求する
  //
  //  割り込み許可が変化したとき
  //    割り込み許可が1→0のデバイスの割り込み要求を0にする
  //    割り込み許可が0→1のデバイスの割り込み信号が1のとき割り込み要求を1にする
  //    割り込み要求が0→1のデバイスがあるときMPUに割り込みを要求する

  //ioiSpcFall ()
  //  SPCの割り込み信号を0にする
  public static void ioiSpcFall () {
    if ((ioiSignal & IOI_SPC_SIGNAL) != 0) {  //割り込み信号が1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiSpcFall()\n", XEiJ.regPC0);
      }
      ioiSignal &= ~IOI_SPC_SIGNAL;  //割り込み信号を1→0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
    }
  }  //ioiSpcFall()

  //ioiFdcFall ()
  //  FDCの割り込み信号を0にする
  public static void ioiFdcFall () {
    if ((ioiSignal & IOI_FDC_SIGNAL) != 0) {  //割り込み信号が1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiFdcFall()\n", XEiJ.regPC0);
      }
      ioiSignal &= ~IOI_FDC_SIGNAL;  //割り込み信号を1→0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
    }
  }  //ioiFdcFall()

  //ioiFddFall ()
  //  FDDの割り込み信号を0にする
  public static void ioiFddFall () {
    if ((ioiSignal & IOI_FDD_SIGNAL) != 0) {  //割り込み信号が1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiFddFall()\n", XEiJ.regPC0);
      }
      ioiSignal &= ~IOI_FDD_SIGNAL;  //割り込み信号を1→0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
    }
  }  //ioiFddFall()

  //ioiHdcFall ()
  //  HDCの割り込み信号を0にする
  public static void ioiHdcFall () {
    if ((ioiSignal & IOI_HDC_SIGNAL) != 0) {  //割り込み信号が1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiHdcFall()\n", XEiJ.regPC0);
      }
      ioiSignal &= ~IOI_HDC_SIGNAL;  //割り込み信号を1→0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
    }
  }  //ioiHdcFall()

  //ioiPrnFall ()
  //  PRNの割り込み信号を0にする
  public static void ioiPrnFall () {
    if ((ioiSignal & IOI_PRN_SIGNAL) != 0) {  //割り込み信号が1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiPrnFall()\n", XEiJ.regPC0);
      }
      ioiSignal &= ~IOI_PRN_SIGNAL;  //割り込み信号を1→0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
    }
  }  //ioiPrnFall()

  //ioiSpcRise ()
  //  SPCの割り込み信号を1にする
  public static void ioiSpcRise () {
    if ((ioiSignal & IOI_SPC_SIGNAL) == 0) {  //割り込み信号が0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiSpcRise()\n", XEiJ.regPC0);
      }
      ioiSignal |= IOI_SPC_SIGNAL;  //割り込み信号を0→1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
      if ((ioiEnable & IOI_SPC_REQUEST) != 0 &&  //割り込み許可が1
          (ioiRequest & IOI_SPC_REQUEST) == 0) {  //割り込み要求が0
        ioiRequest |= IOI_SPC_REQUEST;  //割り込み要求を0→1
        if (IOI_DEBUG_TRACE) {
          System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
        }
        XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;  //MPUに割り込みを要求する
      }
    }
  }  //ioiSpcRise()

  //ioiFdcRise ()
  //  FDCの割り込み信号を1にする
  public static void ioiFdcRise () {
    if ((ioiSignal & IOI_FDC_SIGNAL) == 0) {  //割り込み信号が0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiFdcRise()\n", XEiJ.regPC0);
      }
      ioiSignal |= IOI_FDC_SIGNAL;  //割り込み信号を0→1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
      if ((ioiEnable & IOI_FDC_REQUEST) != 0 &&  //割り込み許可が1
          (ioiRequest & IOI_FDC_REQUEST) == 0) {  //割り込み要求が0
        ioiRequest |= IOI_FDC_REQUEST;  //割り込み要求を0→1
        if (IOI_DEBUG_TRACE) {
          System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
        }
        XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;  //MPUに割り込みを要求する
      }
    }
  }  //ioiFdcRise()

  //ioiFddRise ()
  //  FDDの割り込み信号を1にする
  public static void ioiFddRise () {
    if ((ioiSignal & IOI_FDD_SIGNAL) == 0) {  //割り込み信号が0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiFddRise()\n", XEiJ.regPC0);
      }
      ioiSignal |= IOI_FDD_SIGNAL;  //割り込み信号を0→1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
      if ((ioiEnable & IOI_FDD_REQUEST) != 0 &&  //割り込み許可が1
          (ioiRequest & IOI_FDD_REQUEST) == 0) {  //割り込み要求が0
        ioiRequest |= IOI_FDD_REQUEST;  //割り込み要求を0→1
        if (IOI_DEBUG_TRACE) {
          System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
        }
        XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;  //MPUに割り込みを要求する
      }
    }
  }  //ioiFddRise()

  //ioiHdcRise ()
  //  HDCの割り込み信号を1にする
  public static void ioiHdcRise () {
    if ((ioiSignal & IOI_HDC_SIGNAL) == 0) {  //割り込み信号が0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiHdcRise()\n", XEiJ.regPC0);
      }
      ioiSignal |= IOI_HDC_SIGNAL;  //割り込み信号を0→1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
      if ((ioiEnable & IOI_HDC_REQUEST) != 0 &&  //割り込み許可が1
          (ioiRequest & IOI_HDC_REQUEST) == 0) {  //割り込み要求が0
        ioiRequest |= IOI_HDC_REQUEST;  //割り込み要求を0→1
        if (IOI_DEBUG_TRACE) {
          System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
        }
        XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;  //MPUに割り込みを要求する
      }
    }
  }  //ioiHdcRise()

  //ioiPrnRise ()
  //  PRNの割り込み信号を1にする
  public static void ioiPrnRise () {
    if ((ioiSignal & IOI_PRN_SIGNAL) == 0) {  //割り込み信号が0
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("%08x ioiPrnRise()\n", XEiJ.regPC0);
      }
      ioiSignal |= IOI_PRN_SIGNAL;  //割り込み信号を0→1
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiSignal=0x%04x\n", ioiSignal);
      }
      if ((ioiEnable & IOI_PRN_REQUEST) != 0 &&  //割り込み許可が1
          (ioiRequest & IOI_PRN_REQUEST) == 0) {  //割り込み要求が0
        ioiRequest |= IOI_PRN_REQUEST;  //割り込み要求を0→1
        if (IOI_DEBUG_TRACE) {
          System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
        }
        XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;  //MPUに割り込みを要求する
      }
    }
  }  //ioiPrnRise()

  //vector = ioiAcknowledge ()
  //  MPUが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返す
  public static int ioiAcknowledge () {
    int vector = 0;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiAcknowledge()\n", XEiJ.regPC0);
    }
    if ((ioiRequest & IOI_SPC_REQUEST) != 0) {  //SPCの割り込み要求が1のとき
      ioiInService = IOI_SPC_REQUEST;  //SPCの割り込み処理中を1にする
      vector = IOI_SPC_VECTOR;  //SPCの割り込みベクタを返す
    } else if ((ioiRequest & IOI_FDC_REQUEST) != 0) {  //FDCの割り込み要求が1のとき
      ioiInService = IOI_FDC_REQUEST;  //FDCの割り込み処理中を1にする
      vector = ioiVector | IOI_FDC_VECTOR;  //FDCの割り込みベクタを返す
    } else if ((ioiRequest & IOI_FDD_REQUEST) != 0) {  //FDDの割り込み要求が1のとき
      ioiInService = IOI_FDD_REQUEST;  //FDDの割り込み処理中を1にする
      vector = ioiVector | IOI_FDD_VECTOR;  //FDDの割り込みベクタを返す
    } else if ((ioiRequest & IOI_HDC_REQUEST) != 0) {  //HDCの割り込み要求が1のとき
      ioiInService = IOI_HDC_REQUEST;  //HDCの割り込み処理中を1にする
      vector = ioiVector | IOI_HDC_VECTOR;  //HDCの割り込みベクタを返す
    } else if ((ioiRequest & IOI_PRN_REQUEST) != 0) {  //PRNの割り込み要求が1のとき
      ioiInService = IOI_PRN_REQUEST;  //PRNの割り込み処理中を1にする
      vector = ioiVector | IOI_PRN_VECTOR;  //PRNの割り込みベクタを返す
    }
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiInService=0x%04x\n", ioiInService);
    }
    if (true) {
      ioiRequest &= ~ioiInService;  //割り込み要求を0にする
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
      }
    }
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tvector=0x%02x\n", vector);
    }
    return vector;
  }  //ioiAcknowledge()

  //ioiDone ()
  //  MPUが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void ioiDone () {
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiDone()\n", XEiJ.regPC0);
    }
    if (false) {
      ioiRequest &= ~ioiInService;  //割り込み要求を0にする
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
      }
    }
    ioiInService = 0;  //割り込み処理中を0にする
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiInService=0x%04x\n", ioiInService);
    }
    if (ioiRequest != 0) {  //割り込み要求が1のデバイスが残っている
      XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;  //再度MPUに割り込みを要求する
    }
  }  //ioiDone()

  //d = ioiReadStatus ()
  //  リードステータスレジスタ
  //  0x00e9c001
  public static int ioiReadStatus () {
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiReadStatus()\n", XEiJ.regPC0);
    }
    int d = (ioiSignal | ioiEnable) & 255;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tstatus=0x%02x\n", d);
    }
    return d;
  }  //ioiReadStatus()

  //d = ioiReadVector ()
  //  リードベクタレジスタ
  //  0x00e9c003
  public static int ioiReadVector () {
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiReadVector()\n", XEiJ.regPC0);
    }
    int d = ioiVector;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tvector=0x%02x\n", d);
    }
    return d;
  }  //ioiReadVector()

  //ioiWriteEnable (d)
  //  ライトイネーブルレジスタ
  //  0x00e9c001
  public static void ioiWriteEnable (int d) {
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiWriteEnable(0x%02x)\n", XEiJ.regPC0, d & 255);
    }
    int enable = ioiEnable & ~0x0f | d & 0x0f;
    int falled = ioiEnable & ~enable;  //1=1→0
    int raised = ~ioiEnable & enable;  //1=0→1
    ioiEnable = enable;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiEnable=0x%04x\n", ioiEnable);
    }
    int request = ioiRequest;
    if ((falled & IOI_FDC_REQUEST) != 0) {
      request &= ~IOI_FDC_REQUEST;
    } else if ((raised & IOI_FDC_REQUEST) != 0 && (ioiSignal & IOI_FDC_SIGNAL) != 0) {
      request |= IOI_FDC_REQUEST;
    }
    if ((falled & IOI_FDD_REQUEST) != 0) {
      request &= ~IOI_FDD_REQUEST;
    } else if ((raised & IOI_FDD_REQUEST) != 0 && (ioiSignal & IOI_FDD_SIGNAL) != 0) {
      request |= IOI_FDD_REQUEST;
    }
    if ((falled & IOI_HDC_REQUEST) != 0) {
      request &= ~IOI_HDC_REQUEST;
    } else if ((raised & IOI_HDC_REQUEST) != 0 && (ioiSignal & IOI_HDC_SIGNAL) != 0) {
      request |= IOI_HDC_REQUEST;
    }
    if ((falled & IOI_PRN_REQUEST) != 0) {
      request &= ~IOI_PRN_REQUEST;
    } else if ((raised & IOI_PRN_REQUEST) != 0 && (ioiSignal & IOI_PRN_SIGNAL) != 0) {
      request |= IOI_PRN_REQUEST;
    }
    if (ioiRequest != request) {
      ioiRequest = request;  //ここで割り込み要求が増えないことはない
      if (IOI_DEBUG_TRACE) {
        System.out.printf ("\tioiRequest=0x%04x\n", ioiRequest);
      }
      XEiJ.mpuIRR |= XEiJ.MPU_IOI_INTERRUPT_MASK;
    }
  }  //ioiWriteEnable(int)

  //ioiWriteVector (d)
  //  ライトベクタレジスタ
  //  0x00e9c003
  //  割り込みベクタを設定する
  public static void ioiWriteVector (int d) {
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("%08x ioiWriteVector(0x%02x)\n", XEiJ.regPC0, d & 255);
    }
    ioiVector = d & 0xfc;
    if (IOI_DEBUG_TRACE) {
      System.out.printf ("\tioiVector=0x%02x\n", ioiVector);
    }
  }  //ioiWriteVector(int)



  public static int ioiReadByte (int a) {
    a &= XEiJ.BUS_MOTHER_MASK;
    if (a == IOI_STATUS) {
      return ioiReadStatus ();
    } else if (a == IOI_VECTOR) {  //ライトオンリー。リードすると$FFが返る
      //return ioiReadVector ();
      return 0xff;
    } else {
      return 0xff;
    }
  }  //ioiReadByte

  public static void ioiWriteByte (int a, int d) {
    a &= XEiJ.BUS_MOTHER_MASK;
    if (a == IOI_STATUS) {
      ioiWriteEnable (d);
    } else if (a == IOI_VECTOR) {
      ioiWriteVector (d);
    }
  }  //ioiWriteByte



}  //class IOInterrupt
