//========================================================================================
//  Model.java
//    en:Model
//    ja:機種
//  Copyright (C) 2003-2022 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

public class Model {

  //コード
  public static final int CODE_X68000      = 0b00000000;
  public static final int CODE_X68030      = 0b10000000;
  public static final int CODE_SHODAI      = 0b00000000;
  public static final int CODE_ACE         = 0b00010000;
  public static final int CODE_EXPERT      = 0b00100000;
  public static final int CODE_PRO         = 0b00110000;
  public static final int CODE_SUPER       = 0b01000000;
  public static final int CODE_XVI         = 0b01010000;
  public static final int CODE_COMPACT     = 0b01100000;
  public static final int CODE_II          = 0b00001000;
  public static final int CODE_HD          = 0b00000100;
  public static final int CODE_OFFICE_GRAY = 0b00000000;
  public static final int CODE_GRAY        = 0b00000001;
  public static final int CODE_TITAN_BLACK = 0b00000010;
  public static final int CODE_BLACK       = 0b00000011;
  public static final String[] COLOR_NAMES = {
    "Office Gray",  //0
    "Gray",  //1
    "Titan Black",  //2
    "Black",  //3
  };

  //MPU
  public static final int MPU_MC68000   = 1;
  public static final int MPU_MC68010   = 2;
  public static final int MPU_MC68020   = 3;
  public static final int MPU_MC68EC030 = 4;
  public static final int MPU_MC68030   = 5;
  public static final int MPU_MC68LC040 = 6;
  public static final int MPU_MC68040   = 7;
  public static final int MPU_MC68LC060 = 8;
  public static final int MPU_MC68060   = 9;
  public static final String[] MPU_NAMES = {
    "",  //0
    "MC68000",  //1
    "MC68010",  //2
    "MC68020",  //3
    "MC68EC030",  //4
    "MC68030",  //5
    "MC68LC040",  //6
    "MC68040",  //7
    "MC68LC060",  //8
    "MC68060",  //9
  };

  //FPU
  public static final int FPU_MC68881 = 1;
  public static final int FPU_MC68882 = 2;
  public static final int FPU_MC68040 = 3;
  public static final int FPU_MC68060 = 4;
  public static final String[] FPU_NAMES = {
    "",  //0
    "MC68881",  //1
    "MC68882",  //2
    "MC68040",  //3
    "MC68060",  //4
  };

  //機種
  //  初代 オフィスグレー
  public static final Model CZ_600CE = new Model ("CZ-600CE",  //type
                                                  CODE_X68000 + CODE_OFFICE_GRAY,  //code
                                                  false,  //scsi
                                                  1,  //memory
                                                  MPU_MC68000,  //mpu
                                                  10.0,  //clock
                                                  100,  //iplrom
                                                  "Shodai"  //synonym
                                                  );  //1987-03
  //  初代 ブラック
  public static final Model CZ_600CB = new Model ("CZ-600CB",  //type
                                                  CODE_X68000 + CODE_BLACK,  //code
                                                  false,  //scsi
                                                  1,  //memory
                                                  MPU_MC68000,  //mpu
                                                  10.0,  //clock
                                                  101,  //iplrom
                                                  null  //synonym
                                                  );  //1987-11
  //  ACE ブラック
  public static final Model CZ_601C_BK = new Model ("CZ-601C-BK",  //type
                                                    CODE_X68000 + CODE_ACE + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1988-03
  //  ACE グレー
  public static final Model CZ_601C_GY = new Model ("CZ-601C-GY",  //type
                                                    CODE_X68000 + CODE_ACE + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    "ACE"  //synonym
                                                    );  //1988-03
  //  ACE HD ブラック
  public static final Model CZ_611C_BK = new Model ("CZ-611C-BK",  //type
                                                    CODE_X68000 + CODE_ACE + CODE_HD + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1988-03
  //  ACE HD グレー
  public static final Model CZ_611C_GY = new Model ("CZ-611C-GY",  //type
                                                    CODE_X68000 + CODE_ACE + CODE_HD + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1988-03
  //  EXPERT ブラック
  public static final Model CZ_602C_BK = new Model ("CZ-602C-BK",  //type
                                                    CODE_X68000 + CODE_EXPERT + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    "EXPERT"  //synonym
                                                    );  //1989-03
  //  EXPERT グレー
  public static final Model CZ_602C_GY = new Model ("CZ-602C-GY",  //type
                                                    CODE_X68000 + CODE_EXPERT + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1989-03
  //  EXPERT HD ブラック
  public static final Model CZ_612C_BK = new Model ("CZ-612C-BK",  //type
                                                    CODE_X68000 + CODE_EXPERT + CODE_HD + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1989-03
  //  PRO ブラック
  public static final Model CZ_652C_BK = new Model ("CZ-652C-BK",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1989-03
  //  PRO グレー
  public static final Model CZ_652C_GY = new Model ("CZ-652C-GY",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    "PRO"  //synonym
                                                    );  //1989-03
  //  PRO HD ブラック
  public static final Model CZ_662C_BK = new Model ("CZ-662C-BK",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_HD + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1989-03
  //  PRO HD グレー
  public static final Model CZ_662C_GY = new Model ("CZ-662C-GY",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_HD + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1989-03
  //  EXPERTII ブラック
  public static final Model CZ_603C_BK = new Model ("CZ-603C-BK",  //type
                                                    CODE_X68000 + CODE_EXPERT + CODE_II + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    "EXPERTII"  //synonym
                                                    );  //1990-03
  //  EXPERTII グレー
  public static final Model CZ_603C_GY = new Model ("CZ-603C-GY",  //type
                                                    CODE_X68000 + CODE_EXPERT + CODE_II + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1990-03
  //  EXPERTII HD ブラック
  public static final Model CZ_613C_BK = new Model ("CZ-613C-BK",  //type
                                                    CODE_X68000 + CODE_EXPERT + CODE_II + CODE_HD + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1990-03
  //  PROII ブラック
  public static final Model CZ_653C_BK = new Model ("CZ-653C-BK",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_II + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1990-04
  //  PROII グレー
  public static final Model CZ_653C_GY = new Model ("CZ-653C-GY",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_II + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    "PROII"  //synonym
                                                    );  //1990-04
  //  PROII HD ブラック
  public static final Model CZ_663C_BK = new Model ("CZ-663C-BK",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_II + CODE_HD + CODE_BLACK,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1990-04
  //  PROII HD グレー
  public static final Model CZ_663C_GY = new Model ("CZ-663C-GY",  //type
                                                    CODE_X68000 + CODE_PRO + CODE_II + CODE_HD + CODE_GRAY,  //code
                                                    false,  //scsi
                                                    1,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    102,  //iplrom
                                                    null  //synonym
                                                    );  //1990-04
  //  SUPER HD ブラック
  public static final Model CZ_623C_TN = new Model ("CZ-623C-TN",  //type
                                                    CODE_X68000 + CODE_SUPER + CODE_HD + CODE_TITAN_BLACK,  //code
                                                    true,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    103,  //iplrom
                                                    null  //synonym
                                                    );  //1990-06
  //  SUPER チタンブラック
  public static final Model CZ_604C_TN = new Model ("CZ-604C-TN",  //type
                                                    CODE_X68000 + CODE_SUPER + CODE_TITAN_BLACK,  //code
                                                    true,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    10.0,  //clock
                                                    103,  //iplrom
                                                    "SUPER"  //synonym
                                                    );  //1991-01
  //  XVI チタンブラック
  public static final Model CZ_634C_TN = new Model ("CZ-634C-TN",  //type
                                                    CODE_X68000 + CODE_XVI + CODE_TITAN_BLACK, //code
                                                    true,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    50.0 / 3.0,  //clock
                                                    110,  //iplrom
                                                    "XVI"  //synonym
                                                    );  //1991-05
  //  XVI HD チタンブラック
  public static final Model CZ_644C_TN = new Model ("CZ-644C-TN",  //type
                                                    CODE_X68000 + CODE_XVI + CODE_HD + CODE_TITAN_BLACK,  //code
                                                    true,  //scsi
                                                    2,  //memory
                                                    MPU_MC68000,  //mpu
                                                    50.0 / 3.0,  //clock
                                                    110,  //iplrom
                                                    null  //synonym
                                                    );  //1991-05
  //  Compact グレー
  public static final Model CZ_674C_H = new Model ("CZ-674C-H",  //type
                                                   CODE_X68000 + CODE_COMPACT + CODE_GRAY,  //code
                                                   true,  //scsi
                                                   2,  //memory
                                                   MPU_MC68000,  //mpu
                                                   50.0 / 3.0,  //clock
                                                   120,  //iplrom
                                                   "Compact"  //synonym
                                                   );  //1992-02
  //  X68030 チタンブラック
  public static final Model CZ_500C_B = new Model ("CZ-500C-B",  //type
                                                   CODE_X68030 + CODE_TITAN_BLACK,  //code
                                                   true,  //scsi
                                                   4,  //memory
                                                   MPU_MC68EC030,  //mpu
                                                   25.0,  //clock
                                                   130,  //iplrom
                                                   "X68030"  //synonym
                                                   );  //1993-03
  //  X68030 HD チタンブラック
  public static final Model CZ_510C_B = new Model ("CZ-510C-B",  //type
                                                   CODE_X68030 + CODE_HD + CODE_TITAN_BLACK,  //code
                                                   true,  //scsi
                                                   4,  //memory
                                                   MPU_MC68EC030,  //mpu
                                                   25.0,  //clock
                                                   130,  //iplrom
                                                   null  //synonym
                                                   );  //1993-03
  //  X68030 Compact チタンブラック
  public static final Model CZ_300C_B = new Model ("CZ-300C-B",  //type
                                                   CODE_X68030 + CODE_COMPACT + CODE_TITAN_BLACK,  //code
                                                   true,  //scsi
                                                   4,  //memory
                                                   MPU_MC68EC030,  //mpu
                                                   25.0,  //clock
                                                   130,  //iplrom
                                                   "030Compact"  //synonym
                                                   );  //1993-05
  //  X68030 Compact HD チタンブラック
  public static final Model CZ_310C_B = new Model ("CZ-310C-B",  //type
                                                   CODE_X68030 + CODE_COMPACT + CODE_HD + CODE_TITAN_BLACK,  //code
                                                   true,  //scsi
                                                   4,  //memory
                                                   MPU_MC68EC030,  //mpu
                                                   25.0,  //clock
                                                   130,  //iplrom
                                                   null  //synonym
                                                   );  //1993-05

  //機種の別名
  public static final Model SHODAI        = CZ_600CE;    //  初代 オフィスグレー
  public static final Model ACE           = CZ_601C_GY;  //  ACE オフィスグレー
  public static final Model EXPERT        = CZ_602C_BK;  //  EXPERT ブラック
  public static final Model PRO           = CZ_652C_GY;  //  PRO グレー
  public static final Model EXPERTII      = CZ_603C_BK;  //  EXPERTII ブラック
  public static final Model PROII         = CZ_653C_GY;  //  PROII オフィスグレー
  public static final Model SUPER         = CZ_604C_TN;  //  SUPER チタンブラック
  public static final Model XVI           = CZ_634C_TN;  //  XVI チタンブラック
  public static final Model COMPACT       = CZ_674C_H;   //  Compact グレー
  public static final Model X68030        = CZ_500C_B;   //  X68030 チタンブラック
  public static final Model X68030COMPACT = CZ_300C_B;   //  X68030 Compact チタンブラック

  //機種の配列
  public static final Model[] MODELS = {
    CZ_600CE,    //  初代 オフィスグレー
    CZ_600CB,    //  初代 ブラック
    CZ_601C_BK,  //  ACE ブラック
    CZ_601C_GY,  //  ACE オフィスグレー
    CZ_611C_BK,  //  ACE HD ブラック
    CZ_611C_GY,  //  ACE HD オフィスグレー
    CZ_602C_BK,  //  EXPERT ブラック
    CZ_602C_GY,  //  EXPERT オフィスグレー
    CZ_612C_BK,  //  EXPERT HD ブラック
    CZ_652C_BK,  //  PRO ブラック
    CZ_652C_GY,  //  PRO グレー
    CZ_662C_BK,  //  PRO HD ブラック
    CZ_662C_GY,  //  PRO HD オフィスグレー
    CZ_603C_BK,  //  EXPERTII ブラック
    CZ_603C_GY,  //  EXPERTII オフィスグレー
    CZ_613C_BK,  //  EXPERTII HD ブラック
    CZ_653C_BK,  //  PROII ブラック
    CZ_653C_GY,  //  PROII オフィスグレー
    CZ_663C_BK,  //  PROII HD ブラック
    CZ_663C_GY,  //  PROII HD オフィスグレー
    CZ_623C_TN,  //  SUPER HD ブラック
    CZ_604C_TN,  //  SUPER チタンブラック
    CZ_634C_TN,  //  XVI チタンブラック
    CZ_644C_TN,  //  XVI HD チタンブラック
    CZ_674C_H,   //  Compact グレー
    CZ_500C_B,   //  X68030 チタンブラック
    CZ_510C_B,   //  X68030 HD チタンブラック
    CZ_300C_B,   //  X68030 Compact チタンブラック
    CZ_310C_B,   //  X68030 Compact HD チタンブラック
  };

  //型名または別名から機種を求める
  public static Model fromTypeOrSynonym (String typeOrSynonym) {
    for (Model model : MODELS) {
      if (model.type.equalsIgnoreCase (typeOrSynonym) ||
          (model.synonym != null && model.synonym.equalsIgnoreCase (typeOrSynonym))) {
        return model;
      }
    }
    return null;
  }

  //MPUの名前
  public static String mpuNameOf (int mpu) {
    return MPU_NAMES[mpu];
  }

  //FPUの名前
  public static String fpuNameOf (int fpu) {
    return FPU_NAMES[fpu];
  }

  //インスタンスフィールド
  private String type;  //型名
  private int code;  //コード
  private boolean scsi;  //内蔵ハードディスクインターフェイスはSCSIか
  private int memory;  //標準のメモリ容量(MB)
  private int mpu;  //MPU
  private double clock;  //クロック(MHz)。10.0,50.0/3.0,25.0
  private int iplrom;  //IPLROMのバージョン。100,101,102,103,110,120,130
  private String synonym;  //別名

  //コンストラクタ
  private Model (String type, int code, boolean scsi, int memory, int mpu, double clock, int iplrom, String synonym) {
    this.type = type;
    this.code = code;
    this.scsi = scsi;
    this.memory = memory;
    this.mpu = mpu;
    this.clock = clock;
    this.iplrom = iplrom;
    this.synonym = synonym;
  }

  //型名
  public String getType () {
    return type;
  }

  //コード
  public int getCode () {
    return code;
  }

  //内蔵ハードディスクインターフェイスはSCSIか
  public boolean isSCSI () {
    return scsi;
  }

  //標準のメモリ容量(MB)
  public int getMemory () {
    return memory;
  }

  //MPU
  public int getMPU () {
    return mpu;
  }

  //クロック(MHz)
  public double getClock () {
    return clock;
  }

  //IPLROMのバージョン
  public int getIPLROM () {
    return iplrom;
  }

  //別名
  public String getSynonym () {
    return synonym;
  }

  //機種名
  public String getName () {
    StringBuilder sb = new StringBuilder ();
    switch (code & 0b10000000) {
    case CODE_X68000:
      sb.append ("X68000");
      break;
    case CODE_X68030:
      sb.append ("X68030");
      break;
    }
    switch (code & 0b01110000) {
    case CODE_ACE:
      sb.append (" ACE");
      break;
    case CODE_EXPERT:
      sb.append (" EXPERT");
      break;
    case CODE_PRO:
      sb.append (" PRO");
      break;
    case CODE_SUPER:
      sb.append (" SUPER");
      break;
    case CODE_XVI:
      sb.append (" XVI");
      break;
    case CODE_COMPACT:
      sb.append (" Compact");
      break;
    }
    if ((code & CODE_II) != 0) {
      sb.append ("II");
    }
    if ((code & CODE_HD) != 0) {
      sb.append (" HD");
    }
    return sb.toString ();
  }

  //X68030/X68030 Compactか
  public boolean isX68030 () {
    return (code & CODE_X68030) != 0;
  }

  //X68000初代か
  public boolean isShodai () {
    return (code & 0b11110000) == CODE_X68000 + CODE_SHODAI;
  }

  //ACEか
  public boolean isACE () {
    return (code & 0b01110000) == CODE_ACE;
  }

  //EXPERT/EXPERTIIか
  public boolean isEXPERT () {
    return (code & 0b01110000) == CODE_EXPERT;
  }

  //PRO/PROIIか
  public boolean isPRO () {
    return (code & 0b01110000) == CODE_PRO;
  }

  //SUPERか
  public boolean isSUPER () {
    return (code & 0b01110000) == CODE_SUPER;
  }

  //XVIか
  public boolean isXVI () {
    return (code & 0b01110000) == CODE_XVI;
  }

  //Compact/X68030 Compactか
  public boolean isCompact () {
    return (code & 0b01110000) == CODE_COMPACT;
  }

  //色コード
  public int getColorCode () {
    return code & 0b00000011;
  }

  //色名
  public String getColorName () {
    return COLOR_NAMES[code & 0b00000011];
  }

}
