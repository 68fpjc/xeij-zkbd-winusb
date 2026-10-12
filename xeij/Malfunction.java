//========================================================================================
//  Malfunction.java
//    en:Malfunction screen
//    ja:故障画面
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //FlowLayout
import java.awt.event.*;  //ActionListener
import javax.swing.*;

class Malfunction {

  public static final boolean MLF_ON = true;

/*
  //警告
  private static final String[] CAUTION_EN = {
    "Due to insufficient information, the noise may not be displayed in the",
    "correct position. If you notice any screen errors, please report them.",
  };
  private static final String[] CAUTION_JA = {
    "情報不足のためノイズが正しい位置に表示されない可能性があります。",
    "表示が間違っていることに気づいた方はご報告をお願いいたします。",
  };
*/

  //機種
  private static final String[] MODEL_NAME = {
    "First gen",
    "EXPERT",
    "XVI",
    "Compact",
    "X68030",
    "030 Compact",
  };
  private static int modelNumber;

  //チップ
  public static int mlfChipMask;
  private static int[] nthChip;  //mask==1のチップに0～3の番号を振る

  //アドレス
  private static int addressZero;
  private static int addressOne;
  private static int addressMask;  //zero|one

  //データ
  private static int dataLow;  //15-12:1st,11-8:2nd,7-4:3rd,3-0:4th
  private static int dataHigh;
  private static int dataFrozen;
  private static int dataDynamic;
  private static int dataMask;  //low|high|frozen|dynamic
  private static byte[] noiseArray;

  //IC
  private static final String[][] IC_NAME = {
    //1st gen
    {
      "IC1", "IC2", "IC3", "IC4", "IC5", "IC6", "IC7", "IC8",
      "IC9", "IC10", "IC11", "IC12", "IC13", "IC14", "IC15", "IC16",
      "IC17", "IC19", "IC18", "IC20", "IC21", "IC23", "IC22", "IC24",
      "IC25", "IC27", "IC26", "IC28", "IC29", "IC31", "IC30", "IC32",
    },
    //EXPERT
    {
      "IC49", "IC48", "IC47", "IC46", "IC45", "IC44", "IC43", "IC42",
      "IC41", "IC40", "IC39", "IC38", "IC37", "IC36", "IC35", "IC34",
      "IC33", "IC32", "IC31", "IC30", "IC29", "IC28", "IC27", "IC26",
      "IC25", "IC24", "IC23", "IC22", "IC21", "IC20", "IC19", "IC18",
    },
    //XVI
    {
      "IC53", "IC54", "IC55", "IC56", "IC57", "IC58", "IC59", "IC60",
      "IC61", "IC62", "IC63", "IC64", "IC65", "IC66", "IC67", "IC68",
      "IC69", "IC70", "IC71", "IC72", "IC73", "IC74", "IC75", "IC76",
      "IC77", "IC78", "IC79", "IC80", "IC81", "IC82", "IC83", "IC84",
    },
    //Compact
    {
      "IC20", "IC24", "IC53", "IC57", "IC21", "IC25", "IC54", "IC58",
      "IC22", "IC26", "IC55", "IC59", "IC23", "IC27", "IC56", "IC60",
      "IC12", "IC14", "IC13", "IC15", "IC16", "IC18", "IC17", "IC19",
      "IC45", "IC47", "IC46", "IC48", "IC49", "IC51", "IC50", "IC52",
    },
    //X68030
    {
      "IC59", "IC61", "IC63", "IC65", "IC60", "IC62", "IC64", "IC66",
      "IC67", "IC69", "IC71", "IC73", "IC68", "IC70", "IC72", "IC74",
      "IC87", "IC89", "IC88", "IC90", "IC91", "IC93", "IC92", "IC94",
      "IC95", "IC97", "IC96", "IC98", "IC99", "IC101", "IC100", "IC102",
    },
    //030Compact
    {
      "IC19", "IC23", "IC47", "IC51", "IC20", "IC24", "IC48", "IC52",
      "IC21", "IC25", "IC49", "IC53", "IC22", "IC26", "IC50", "IC54",
      "IC11", "IC13", "IC12", "IC14", "IC15", "IC17", "IC16", "IC18",
      "IC39", "IC41", "IC40", "IC42", "IC43", "IC45", "IC44", "IC46",
    },
  };

  //コンポーネント
  private static JFrame frame;  //ウインドウ
  private static JRadioButton[] modelButton;  //モデルラジオボタン
  private static JCheckBox[] chipCheckBox;  //チップチェックボックス
  //  button[0]が右上でmaskのbit0
  //  button[15]が左上でmaskのbit15
  private static JRadioButton[] addressButton;  //アドレスラジオボタン
  private static JRadioButton[] dataButton;  //データラジオボタン

  //mlfInit ()
  public static void mlfInit () {
    //機種
    modelNumber = 0;
    {
      String s = Settings.sgsGetString ("mlfmodel", "").replace ("%20", " ");
      for (int m = 0; m < MODEL_NAME.length; m++) {
        if (s.equals (MODEL_NAME[m])) {
          modelNumber = m;
          break;
        }
      }
    }
    //チップ
    mlfChipMask = Settings.sgsGetInt ("mlfchip", 0);
    nthChip = new int[32];
    updateNthChip ();
    //アドレス
    addressZero = Settings.sgsGetInt ("mlfzero", 0, 0, 65535);
    addressOne = Settings.sgsGetInt ("mlfone", 0, 0, 65535);
    addressMask = addressZero | addressOne;
    //データ
    dataLow = Settings.sgsGetInt ("mlflow", 0, 0, 65535);
    dataHigh = Settings.sgsGetInt ("mlfhigh", 0, 0, 65535);
    dataFrozen = Settings.sgsGetInt ("mlffrozen", 0, 0, 65535);
    dataDynamic = Settings.sgsGetInt ("mlfdynamic", 0, 0, 65535);
    dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
    noiseArray = new byte[65536 * 32];
    for (int i = 0; i < 65536 * 32; i++) {
      noiseArray[i] = (byte) (random () >>> 28);
    }
    //コンポーネント
    frame = null;
    chipCheckBox = null;
  }  //mlfInit

  //mlfTini ()
  public static void mlfTini () {
    Settings.sgsPutString ("mlfmodel",
                           MODEL_NAME[modelNumber].replace (" ", "%20"));
    Settings.sgsPutInt ("mlfchip", mlfChipMask);
    Settings.sgsPutInt ("mlfzero", addressZero);
    Settings.sgsPutInt ("mlfone", addressOne);
    Settings.sgsPutInt ("mlflow", dataLow);
    Settings.sgsPutInt ("mlfhigh", dataHigh);
    Settings.sgsPutInt ("mlffrozen", dataFrozen);
    Settings.sgsPutInt ("mlfdynamic", dataDynamic);
  }  //mlfTini

  //mlfMake ()
  public static void mlfMake () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        if (command.equals ("Reset to initial settings")) {
          //チップ
          modelNumber = 0;
          mlfChipMask = 0;
          for (int m = 0; m < MODEL_NAME.length; m++) {
            modelButton[m].setSelected (m == 0);
          }
          for (int i = 0; i < 32; i++) {
            chipCheckBox[i].setText (IC_NAME[0][i]);
            chipCheckBox[i].setSelected (false);
          }
          //アドレス
          addressZero = 0;
          addressOne = 0;
          addressMask = addressZero | addressOne;
          for (int i = 0; i < 16 * 3; i++) {
            addressButton[i].setSelected (i < 16);
          }
          //データ
          dataLow = 0;
          dataHigh = 0;
          dataFrozen = 0;
          dataDynamic = 0;
          dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
          for (int i = 0; i < 16 * 5; i++) {
            dataButton[i].setSelected (i < 16);
          }
          updateNthChip ();
          return;
        }
        for (int m = 0; m < MODEL_NAME.length; m++) {
          if (command.equals (MODEL_NAME[m])) {
            modelNumber = m;
            for (int i = 0; i < 32; i++) {
              chipCheckBox[i].setText (IC_NAME[m][i]);
            }
            updateNthChip ();
            return;
          }
        }
        int c = command.charAt (0);
        if (c == 'i') {  //チップ
          int m = 1 << Integer.parseInt (command.substring (1));
          if (((JCheckBox) source).isSelected ()) {
            mlfChipMask |= m;
          } else {
            mlfChipMask &= ~m;
          }
          updateNthChip ();
        } else if (c == 'e') {  //アドレス Either 0 or 1
          int m = 1 << Integer.parseInt (command.substring (1));
          addressZero &= ~m;
          addressOne &= ~m;
          addressMask = addressZero | addressOne;
        } else if (c == 'z') {  //アドレス 0
          int m = 1 << Integer.parseInt (command.substring (1));
          addressZero |= m;
          addressOne &= ~m;
          addressMask = addressZero | addressOne;
        } else if (c == 'o') {  //アドレス 1
          int m = 1 << Integer.parseInt (command.substring (1));
          addressZero &= ~m;
          addressOne |= m;
          addressMask = addressZero | addressOne;
        } else if (c == 'n') {  //データ Normal
          int m = 1 << Integer.parseInt (command.substring (1));
          dataLow &= ~m;
          dataHigh &= ~m;
          dataFrozen &= ~m;
          dataDynamic &= ~m;
          dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
        } else if (c == 'l') {  //データ Stuck at low
          int m = 1 << Integer.parseInt (command.substring (1));
          dataLow |= m;
          dataHigh &= ~m;
          dataFrozen &= ~m;
          dataDynamic &= ~m;
          dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
        } else if (c == 'h') {  //データ Stuck at high
          int m = 1 << Integer.parseInt (command.substring (1));
          dataLow &= ~m;
          dataHigh |= m;
          dataFrozen &= ~m;
          dataDynamic &= ~m;
          dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
        } else if (c == 'f') {  //データ Frozen noise
          int m = 1 << Integer.parseInt (command.substring (1));
          dataLow &= ~m;
          dataHigh &= ~m;
          dataFrozen |= m;
          dataDynamic &= ~m;
          dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
        } else if (c == 'd') {  //データ Dynamic noise
          int m = 1 << Integer.parseInt (command.substring (1));
          dataLow &= ~m;
          dataHigh &= ~m;
          dataFrozen &= ~m;
          dataDynamic |= m;
          dataMask = dataLow | dataHigh | dataFrozen | dataDynamic;
        } else {
          System.out.println ("unknown action command " + command);
        }  //switch command
      }  //actionPerformed
    };  //listener

/*
    //警告パネル
    //     0  1          2
    //  0  0:--------------
    //  1  1: 2:caution0 3:
    //  2  |: 4:caution1 |
    //  3  5:--------------
    //オブジェクト
    Object[] cautionObject = new Object[6];
    for (int i = 0; i < 6; i++) {
      cautionObject[i] = (
        //横線
        i == 0 || i == 5 ?
        ComponentFactory.createHorizontalSeparator () :
        //縦線(隙間)
        i == 1 || i == 3 ?
        Box.createHorizontalStrut (10) :
        //ラベル
        i == 2 ? Multilingual.mlnText (
          ComponentFactory.createLabel (CAUTION_EN[0]),
          "ja", CAUTION_JA[0]) :
        Multilingual.mlnText (
          ComponentFactory.createLabel (CAUTION_EN[1]),
          "ja", CAUTION_JA[1]));
    }
    //パネル
    JPanel cautionPanel =
      ComponentFactory.createGridPanel (
        //colCount
        3,
        //rowCount
        4,
        //gridStyles
        "paddingLeft=3,paddingRight=3,center",
        //colStyles
        "",
        //rowStyles
        "colSpan=3,widen;;;" +  //row=0 横線
        "colSpan=3,widen",  //row=3 横線
        //cellStyles
        ";" +
        "rowSpan=2,lengthen;;" +  //1: 縦線(隙間)
        "rowSpan=2,lengthen",  //3: 縦線(隙間)
        cautionObject);
*/

    //チップパネル
    //     0   1        2   3-10       11
    //  0  0:-----------------------------
    //  1  1:  2:Model  3:     4:model  5:
    //  2  6:-----------------------------
    //  3  7:  8:TVRAM  9: 10-17:[]... 18:
    //  4  |   |        |     19:-----  |
    //  5  |   |        |  20-27:[]...  |
    //  6 28:-----------------------------
    //  7 29: 30:GVRAM 31: 32-39:[]... 40:
    //  8  |   |        |     41:-----  |
    //  9  |   |        |  42-49:[]...  |
    // 10 50:-----------------------------
    //modelパネル
    ButtonGroup modelGroup = new ButtonGroup ();
    modelButton = new JRadioButton[MODEL_NAME.length];
    JPanel modelPanel1 = ComponentFactory.createFlowPanel (
      FlowLayout.CENTER, 10, 0);
    JPanel modelPanel2 = ComponentFactory.createFlowPanel (
      FlowLayout.CENTER, 10, 0);
    for (int m = 0; m < MODEL_NAME.length; m++) {
      modelButton[m] = ComponentFactory.createRadioButton (
        modelGroup,
        m == modelNumber,
        MODEL_NAME[m],
        listener);
      if (m == 0) {
        Multilingual.mlnText (modelButton[m],
                              "ja", "初代");
      }
      (m < 4 ? modelPanel1 : modelPanel2).add (modelButton[m]);
    }
    //チェックボックス
    chipCheckBox = new JCheckBox[32];
    for (int i = 0; i < 32; i++) {
      chipCheckBox[i] = 
        ComponentFactory.setText (
          ComponentFactory.createCheckBox (
            (mlfChipMask & (1 << i)) != 0,
            "i" + i,
            listener),
          IC_NAME[modelNumber][i]);
    }  //for i
    //オブジェクト
    Object[] chipObject = new Object[51];
    for (int i = 0; i < 51; i++) {
      chipObject[i] = (
        //横線
        i == 0 || i == 6 || i == 19 || i == 28 || i == 41 || i == 50 ?
        ComponentFactory.createHorizontalSeparator () :
        //縦線(隙間)
        i == 1 || i == 3 || i == 5 ||
        i == 7 || i == 9 || i == 18 ||
        i == 29 || i == 31 || i == 40 ?
        Box.createHorizontalStrut (10) :
        //ラベル
        i == 2 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Model"),
          "ja", "機種") :
        i == 8 ? ComponentFactory.createLabel ("TVRAM") :
        i == 30 ? ComponentFactory.createLabel ("GVRAM") :
        //機種パネル
        i == 4 ? ComponentFactory.createVerticalBox (modelPanel1, modelPanel2) :
        //チェックボックス
        i <= 17 ? chipCheckBox[i - 10] :
        i <= 27 ? chipCheckBox[i - 20 + 8] :
        i <= 39 ? chipCheckBox[i - 32 + 16] :
        chipCheckBox[i - 42 + 24]);
    }  //for i
    //パネル
    JPanel chipPanel =
      ComponentFactory.createGridPanel (
        //colCount
        12,
        //rowCount
        11,
        //gridStyles
        "paddingLeft=3,paddingRight=3,center",
        //colStyles
        ";" +
        "italic",  //col=1 ラベル
        //rowStyles
        "colSpan=12,widen;;" +  //row=0 横線
        "colSpan=12,widen;;" +  //row=2 横線
        "colSpan=8,widen;;" +  //row=4 横線
        "colSpan=12,widen;;" +  //row=6 横線
        "colSpan=8,widen;;" +  //row=8 横線
        "colSpan=12,widen",  //row=10 横線
        //cellStyles
        ";" +
        "lengthen;;" +  //1: 縦線(隙間)
        "lengthen;" +  //3: 縦線(隙間)
        "colSpan=8;" +  //4: model
        "lengthen;;" +  //5: 縦線(隙間)
        "rowSpan=3,lengthen;" +  //7: 縦線(隙間)
        "rowSpan=3;" +  //8: TVRAM
        "rowSpan=3,lengthen;;;;;;;;;" +  //9: 縦線(隙間)
        "rowSpan=3,lengthen;;;;;;;;;;;" +  //18: 縦線(隙間)
        "rowSpan=3,lengthen;" +  //29: 縦線(隙間)
        "rowSpan=3;" +  //30: GVRAM
        "rowSpan=3,lengthen;;;;;;;;;" +  //31: 縦線(隙間)
        "rowSpan=3,lengthen",  //40: 縦線(隙間)
        chipObject);

    //アドレスパネル
    //     0   1                2    3-10       11  12-19        20
    //  0  0:-------------------------------------------------------
    //  1  1:  2:               3:   4-11:15-8  12: 13-20:7-0    21:
    //  2 22:-------------------------------------------------------
    //  3 23: 24:Either 0 or 1 25:  26-33:[]... 34: 35-42:[]...  43:
    //              mask bit/button  15-8             7-0
    //  4  |  44:0              |   45-52:[]...  |  53-60:[]...   |
    //                       button 31-24           23-16
    //  5  |  61:1              |   62-69:[]...  |  70-77:[]...   |
    //                       button 47-40           39-32
    //  6 78:-------------------------------------------------------
    //ボタン
    ButtonGroup[] addressGroup = new ButtonGroup[16];
    addressButton = new JRadioButton[16 * 3];
    //Either 0 or 1
    for (int i = 0; i < 16; i++) {
      addressGroup[i] = new ButtonGroup ();
      addressButton[16 * 0 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            addressGroup[i],
            (addressMask & (1 << i)) == 0,
            "e" + i,
            listener),
          "");
    }  //for i
    //0
    for (int i = 0; i < 16; i++) {
      addressButton[16 * 1 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            addressGroup[i],
            (addressZero & (1 << i)) != 0,
            "z" + i,
            listener),
          "");
    }  //for i
    //1
    for (int i = 0; i < 16; i++) {
      addressButton[16 * 2 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            addressGroup[i],
            (addressOne & (1 << i)) != 0,
            "o" + i,
            listener),
          "");
    }  //for i
    //オブジェクト
    Object[] addressObject = new Object[79];
    for (int i = 0; i < 79; i++) {
      addressObject[i] = (
        //横線
        i == 0 || i == 22 || i == 78 ?
        ComponentFactory.createHorizontalSeparator () :
        //縦線(隙間)
        i == 1 || i == 3 || i == 12 || i == 21 ||
        i == 23 || i == 25 || i == 34 || i == 43 ?
        Box.createHorizontalStrut (10) :
        //ラベル
        i == 2 ? ComponentFactory.createLabel ("") :
        i == 24 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Either 0 or 1"),
          "ja", "0または1") :
        i == 44 ? ComponentFactory.createLabel ("0") :
        i == 61 ? ComponentFactory.createLabel ("1") :
        i <= 11 ? ComponentFactory.createLabel (String.valueOf (8 + 11 - i)) :
        i <= 20 ? ComponentFactory.createLabel (String.valueOf (20 - i)) :
        //ボタン
        addressButton[i <= 33 ? 8 + 33 - i :  //15-8
                      i <= 42 ? 42 - i :  //7-0
                      i <= 60 ? 16 + 60 - i :  //31-16
                      32 + 77 - i]);  //47-32
    }  //for i
    //パネル
    JPanel addressPanel =
      ComponentFactory.createGridPanel (
        //colCount
        21,
        //rowCount
        7,
        //gridStyles
        "paddingLeft=3,paddingRight=3,center",
        //colStyles
        ";" +
        "italic;",  //col=1 ラベル
        //rowStyles
        "colSpan=21,widen;" +  //row=0 横線
        "italic;" +  //row=1 ラベル
        "colSpan=21,widen;;;;" +  //row=2 横線
        "colSpan=21,widen",  //row=6 横線
        //cellStyles
        ";" +
        "lengthen;;" +  //1: 縦線(隙間)
        "lengthen;;;;;;;;;" +  //3: 縦線(隙間)
        "lengthen;;;;;;;;;" +  //12: 縦線(隙間)
        "lengthen;;" +  //21: 縦線(隙間)
        "rowSpan=3,lengthen;;" +  //23: 縦線(隙間)
        "rowSpan=3,lengthen;;;;;;;;;" +  //25: 縦線(隙間)
        "rowSpan=3,lengthen;;;;;;;;;" +  //34: 縦線(隙間)
        "rowSpan=3,lengthen",  //43: 縦線(隙間)
        addressObject);

    //データパネル
    //      0    1               2    3-6        7   8-11       12  13-16       17  18-21       22
    //  0   0:-------------------------------------------------------------------------------------
    //  1   1:   2:              3:     4:1st    5:     6:2nd    7:     8:3rd    9:    10:4th   11:
    //  2   |    |               |  12-15:3-0    |  16-19:3-0     | 20-23:3-0     | 24-27:3-0    | 
    //  3  28:-------------------------------------------------------------------------------------
    //  4  29:  30:Normal       31: 32-35:[]... 36: 37-40:[]... 41: 42-45:[]... 46: 47-50:[]... 51:
    //              mask bit/button 15-12            11-8             7-4             3-0
    //  5   |:  52:Stuck at low  |  53-56:[]...  |  57-60:[]...  |  61-64:[]...  |  65-68:[]...  |
    //                       button 31-28           27-24           23-20           19-16
    //  6   |:  69:Stuck at high |  70-73:[]...  |  74-77:[]...  |  78-81:[]...  |  82-85:[]...  |
    //                       button 47-44           43-40           39-36           35-32
    //  7   |:  86:Frozen noise  |  87-90:[]...  |  91-94:[]...  |  95-98:[]...  | 99-102:[]...  |
    //                       button 63-60           59-56           55-52           51-48
    //  8   |: 103:Dynamic noise |104-107:[]...  |108-111:[]...  |112-115:[]...  |116-119:[]...  |
    //                       button 79-76           75-72           71-68           67-64
    //  9 120:-------------------------------------------------------------------------------------
    //                 dataGroup     0-3             4-7            8-11           12-15
    //ボタン
    ButtonGroup[] dataGroup = new ButtonGroup[16];
    dataButton = new JRadioButton[16 * 5];
    //Normal
    for (int i = 0; i < 16; i++) {
      dataGroup[i] = new ButtonGroup ();
      dataButton[16 * 0 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            dataGroup[i],
            (dataMask & (1 << i)) == 0,
            "n" + i,
            listener),
          "");
    }  //for i
    //Stuck at low
    for (int i = 0; i < 16; i++) {
      dataButton[16 * 1 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            dataGroup[i],
            (dataLow & (1 << i)) != 0,
            "l" + i,
            listener),
          "");
    }  //for i
    //Stuck at high
    for (int i = 0; i < 16; i++) {
      dataButton[16 * 2 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            dataGroup[i],
            (dataHigh & (1 << i)) != 0,
            "h" + i,
            listener),
          "");
    }  //for i
    //Frozen noise
    for (int i = 0; i < 16; i++) {
      dataButton[16 * 3 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            dataGroup[i],
            (dataFrozen & (1 << i)) != 0,
            "f" + i,
            listener),
          "");
    }  //for i
    //Dynamic noise
    for (int i = 0; i < 16; i++) {
      dataButton[16 * 4 + i] =
        ComponentFactory.setText (
          ComponentFactory.createRadioButton (
            dataGroup[i],
            (dataDynamic & (1 << i)) != 0,
            "d" + i,
            listener),
          "");
    }  //for i
    //オブジェクト
    Object[] dataObject = new Object[121];
    for (int i = 0; i < 121; i++) {
      dataObject[i] = (
        //横線
        i == 0 || i == 28 || i == 120 ?
        ComponentFactory.createHorizontalSeparator () :
        //縦線(隙間)
        i == 1 || i == 3 || i == 5 || i == 7 || i == 9 || i == 11 ||
        i == 29 || i == 31 | i == 36 || i == 41 || i == 46 || i == 51 ?
        Box.createHorizontalStrut (10) :
        //ラベル
        i == 2 ? ComponentFactory.createLabel ("") :
        i == 4 ? ComponentFactory.createLabel ("1st") :
        i == 6 ? ComponentFactory.createLabel ("2nd") :
        i == 8 ? ComponentFactory.createLabel ("3rd") :
        i == 10 ? ComponentFactory.createLabel ("4th") :
        i == 30 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Normal"),
          "ja", "正常") :
        i == 52 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Stuck at low"),
          "ja", "常に0") :
        i == 69 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Stuck at high"),
          "ja", "常に1") :
        i == 86 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Frozen noise"),
          "ja", "静的ノイズ") :
        i == 103 ? Multilingual.mlnText (
          ComponentFactory.createLabel ("Dynamic noise"),
          "ja", "動的ノイズ") :
        i <= 15 ? ComponentFactory.createLabel (String.valueOf (15 - i)) :  //3-0
        i <= 19 ? ComponentFactory.createLabel (String.valueOf (19 - i)) :  //3-0
        i <= 23 ? ComponentFactory.createLabel (String.valueOf (23 - i)) :  //3-0
        i <= 27 ? ComponentFactory.createLabel (String.valueOf (27 - i)) :  //3-0
        //ボタン
        dataButton[i <= 35 ? 16 * 0 + 12 + 35 - i :  //15-12
                   i <= 40 ? 16 * 0 + 8 + 40 - i :  //11-8
                   i <= 45 ? 16 * 0 + 4 + 45 - i :  //7-4
                   i <= 50 ? 16 * 0 + 0 + 50 - i :  //3-0
                   i <= 68 ? 16 * 1 + 68 - i :  //16*1+(15-0)
                   i <= 85 ? 16 * 2 + 85 - i :  //16*2+(15-0)
                   i <= 102 ? 16 * 3 + 102 - i :  //16*3+(15-0)
                   16 * 4 + 119 - i]);  //16*4+(15-0)
    }  //for i
    //パネル
    JPanel dataPanel =
      ComponentFactory.createGridPanel (
        //colCount
        23,
        //rowCount
        10,
        //gridStyles
        "paddingLeft=3,paddingRight=3,center",
        //colStyles
        ";" +
        "italic;",  //col=1 ラベル
        //rowStyles
        "colSpan=23,widen;" +  //row=0 横線
        "italic;" +  //row=1 ラベル
        "italic;" +  //row=2 ラベル
        "colSpan=23,widen;;;;;;" +  //row=3 横線
        "colSpan=23,widen",  //row=9 横線
        //cellStyles
        ";" +
        "rowSpan=2,lengthen;" +  //1: 縦線(隙間)
        "rowSpan=2;" +  //2: -
        "rowSpan=2,lengthen;" +  //3: 縦線(隙間)
        "colSpan=4;" +  //4: 1st
        "rowSpan=2,lengthen;" +  //5: 縦線(隙間)
        "colSpan=4;" +  //6: 2nd
        "rowSpan=2,lengthen;" +  //7: 縦線(隙間)
        "colSpan=4;" +  //8: 3rd
        "rowSpan=2,lengthen;" +  //9: 縦線(隙間)
        "colSpan=4;" +  //10: 4th
        "rowSpan=2,lengthen;;;;;;;;;;;;;;;;;;" +  //11: 縦線(隙間)
        "rowSpan=5,lengthen;;" +  //29: 縦線(隙間)
        "rowSpan=5,lengthen;;;;;" +  //31: 縦線(隙間)
        "rowSpan=5,lengthen;;;;;" +  //36: 縦線(隙間)
        "rowSpan=5,lengthen;;;;;" +  //41: 縦線(隙間)
        "rowSpan=5,lengthen;;;;;" +  //46: 縦線(隙間)
        "rowSpan=5,lengthen",  //51: 縦線(隙間)
        dataObject);

    updateNthChip ();

    //ウインドウ
    frame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_MLF_FRAME_KEY,
        "Malfunction screen (experimental)",
        null,
        ComponentFactory.createVerticalBox (
          ComponentFactory.setPreferredSize (
            ComponentFactory.setEmptyBorder (
              ComponentFactory.createScrollPane (
                ComponentFactory.setEmptyBorder (
                  ComponentFactory.createVerticalBox (
/*
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      Multilingual.mlnText (
                        ComponentFactory.createLabel ("Caution"),
                        "ja", "警告")),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      cautionPanel),
                    Box.createVerticalStrut (10),
*/
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      Multilingual.mlnText (
                        ComponentFactory.createLabel ("VRAM chip"),
                        "ja", "VRAMチップ")),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      chipPanel),
                    Box.createVerticalStrut (10),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      Multilingual.mlnText (
                        ComponentFactory.createLabel ("Address within chip"),
                        "ja", "チップ内アドレス")),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      addressPanel),
                    Box.createVerticalStrut (10),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      Multilingual.mlnText (
                        ComponentFactory.createLabel ("Data corruption"),
                        "ja", "データ破壊")),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      dataPanel),
                    Box.createVerticalStrut (10),
                    ComponentFactory.createFlowPanel (
                      FlowLayout.CENTER, 0, 0,
                      Multilingual.mlnText (
                        ComponentFactory.createButton (
                          "Reset to initial settings",
                          listener),
                        "ja", "初期設定に戻す")
                      )
                    ),  //createVerticalBox
                  10, 10, 10, 10)  //setEmptyBorder
                ),  //createScrollPane
              0, 0, 0, 0),  //setEmptyBorder
            600, 520),  //setPreferredSize
          Box.createVerticalGlue ()
          )  //createVerticalBox
        ),  //createRestorableSubFrame
      "ja", "故障画面 (実験中)");  //mlnTitle

  }  //mlfMake

  //mlfStart ()
  public static void mlfStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_MLF_FRAME_KEY)) {
      mlfOpen ();
    }
  }  //mlfStart

  //mlfOpen ()
  public static void mlfOpen () {
    if (frame == null) {
      mlfMake ();
    }
    XEiJ.pnlExitFullScreen (false);
    frame.setVisible (true);
  }  //mlfOpen

  private static void updateNthChip () {
    int n = 0;  //ONのチップの数
    for (int i = 0; i < 32; i++) {
      if ((mlfChipMask & (1 << i)) != 0) {
        nthChip[i] = (n & 3);
        n++;
      } else {
        nthChip[i] = -1;
      }
    }
    if (addressButton != null) {
      for (int i = 0; i < 16 * 3; i++) {
        addressButton[i].setEnabled (0 < n);
      }
    }
    if (dataButton != null) {
      for (int i = 0; i < 16 * 5; i++) {
        //  (i&15)>>2  左から3,2,1,0
        //  3-((i&15)>>2)  左から0,1,2,3
        dataButton[i].setEnabled ((3 - ((i & 15) >> 2)) < n);
      }
    }
  }  //updateNthChip

  //!!! 以下は推測のため誤りが含まれる可能性がある
/*
(SPECULATION)

CHIP ORDER IN SCHEMATICS
          <-------P0--------> <-------P1-------->
        A [ 0] [ 1] [ 2] [ 3] [ 4] [ 5] [ 6] [ 7]
  TVRAM |  LL   LH   HL   HH   LL   LH   HL   HH
        V [ 8] [ 9] [10] [11] [12] [13] [14] [15]
          <-------P2--------> <-------P3-------->
          <-------P0--------> <-------P1-------->
        A [16] [17] [18] [19] [20] [21] [22] [23]
  GVRAM |  NW   NE   SW   SE   NW   NE   SW   SE
        V [24] [25] [26] [27] [28] [29] [30] [31]
          <-------P2--------> <-------P3-------->

TVRAM
             +0        +1            +7E       +7F
  E00000  [ 3] [ 2] [ 1] [ 0] ... [ 3] [ 2] [ 1] [ 0]
          0000 0000 0000 0000 ... 003F 003F 003F 003F
  E00080  [ 3] [ 2] [ 1] [ 0] ... [ 3] [ 2] [ 1] [ 0]
          0040 0040 0040 0040 ... 007F 007F 007F 007F
                               :
  E1FF80  [ 3] [ 2] [ 1] [ 0] ... [ 3] [ 2] [ 1] [ 0]
          FFC0 FFC0 FFC0 FFC0 ... FFFF FFFF FFFF FFFF
  E20000  [ 7] [ 6] [ 5] [ 4] ... [ 7] [ 6] [ 5] [ 4]
          0000 0000 0000 0000 ... 003F 003F 003F 003F
                               :
  E7FF80  [15] [14] [13] [12] ... [15] [14] [13] [12]
          FFC0 FFC0 FFC0 FFC0 ... FFFF FFFF FFFF FFFF

GVRAM (512 DOTS 16 COLORS)
           +0   +2   +4   +6      +3F8 +3FA +3FC +3FE
  C00000  [16] [17] [16] [17] ... [16] [17] [16] [17]
          0000 0000 0001 0001 ... 00FE 00FE 00FF 00FF
  C00400  [16] [17] [16] [17] ... [16] [17] [16] [17]
          0100 0100 0101 0101 ... 01FE 01FE 01FF 01FF
                               :
  C3FC00  [16] [17] [16] [17] ... [16] [17] [16] [17]
          FF00 FF00 FF01 FF01 ... FFFE FFFE FFFF FFFF
  C40000  [18] [19] [18] [19] ... [18] [19] [18] [19]
          0000 0000 0001 0001 ... 00FE 00FE 00FF 00FF
                               :
  DFFC00  [30] [31] [30] [31] ... [30] [31] [30] [31]
          FF00 FF00 FF01 FF01 ... FFFE FFFE FFFF FFFF
*/

  //VRAMのデータを一時的に退避させる場所
  //  1ラスタのハーフワード数だけ必要
  private static final int[] tempTArray = new int[2 * 1024];
  private static final int[] tempGArray = new int[2 * 1024];
  private static int tempTIndex;
  private static int tempGIndex;

  //mlfRaster (dataY)
  //  一時的にVRAMを書き換えて描画する
  public static void mlfRaster (int src, int dst, boolean rh) {
    //TVRAMを書き換える
    tempTIndex = 0;
    if ((mlfChipMask & 0x0000ffff) != 0 &&
        (VideoController.vcnReg3Curr & 0x0060) != 0) {
      int r8c2 = (CRTC.crtR11TxYZero + src) & 1023;  //ドットY座標
      for (int c6 = 0; c6 < 64; c6++) {  //ワードX座標
        int r8c8 = r8c2 << 6 | c6;  //チップ内アドレス
        if ((r8c8 & addressZero) != 0 ||
            (~r8c8 & addressOne) != 0) {
          continue;
        }
        for (int i5 = 0; i5 < 16; i5++) {  //チップ番号
          if ((mlfChipMask & (1 << i5)) == 0) {
            continue;
          }
          int p2 = i5 >> 2;  //プレーン
          int a = (0x00e00000 |  //メモリアドレス
                   p2 << 17 |  //プレーン
                   r8c8 << 1 |  //ワード
                   ((~i5 >> 1) & 1));  //バイト。チップ番号が若い方が下位
          int d = MainMemory.mmrM8[a];
          tempTArray[tempTIndex] = a;
          tempTArray[tempTIndex + 1] = d;
          tempTIndex += 2;
          //  nthChip[i5]  左から0,1,2,3
          //  3-nthChip[i5]  左から3,2,1,0
          int b = ((3 - nthChip[i5]) << 2);
          if ((i5 & 1) == 0) {  //ハーフバイト下位
            d &= ~((dataMask >> b) & 15);
            d |= ((dataHigh >> b) & 15);
            if (((dataFrozen >> b) & 15) != 0) {
              d |= (((dataFrozen >> b) & 15) & noiseArray[0 << 18 | p2 << 16 | r8c8]);
            }
            if (((dataDynamic >> b) & 15) != 0) {
              d |= (((dataDynamic >> b) & 15) & (random () >>> 28));
            }
            MainMemory.mmrM8[a] = (byte) d;
          } else {  //ハーフバイト上位
            d &= ~(((dataMask >> b) & 15) << 4);
            d |= ((dataHigh >> b) & 15) << 4;
            if (((dataFrozen >> b) & 15) != 0) {
              d |= (((dataFrozen >> b) & 15) & noiseArray[0 << 18 | p2 << 16 | r8c8]) << 4;
            }
            if (((dataDynamic >> b) & 15) != 0) {
              d |= (((dataDynamic >> b) & 15) & (random () >>> 28)) << 4;
            }
            MainMemory.mmrM8[a] = (byte) d;
          }
        }
      }
    }
    //GVRAMを書き換える
    tempGIndex = 0;
    if ((mlfChipMask & 0xffff0000) != 0 &&
        (VideoController.vcnReg3Curr & 0x001f) != 0) {
      for (int p2 = 0; p2 < 4; p2++) {  //ページ
        int y9;  //ドットY座標
        if ((VideoController.vcnReg1Curr & 7) < 4) {  //512x512
          y9 = (CRTC.crtR13GrYZero[p2] + src) & 511;
        } else {  //1024x1024
          int y10 = (CRTC.crtR13GrYZero[0] + src) & 1023;
          if ((y10 >> 9) != (p2 >> 1)) {  //上半分は0,1、下半分は2,3
            continue;
          }
          y9 = y10 & 511;
        }
        //int r8 = y9 >> 1;  //NSはLSB
        //int ns = y9 & 1;
        int r8 = y9 & 255;  //NSはMSB
        int ns = y9 >> 8;
        for (int x9 = 0; x9 < 512; x9++) {  //ドットX座標
          int c8 = x9 >> 1;
          int we = x9 & 1;
          int r8c8 = r8 << 8 | c8;  //チップ内アドレス
          if ((r8c8 & addressZero) != 0 ||
              (~r8c8 & addressOne) != 0) {
            continue;
          }
          int i5 = 16 | p2 << 2 | ns << 1 | we;  //チップ番号
          if ((mlfChipMask & (1 << i5)) == 0) {
            continue;
          }
          int a = p2 << 18 | y9 << 9 | x9;  //アドレス
          int d = GraphicScreen.graM4[a];
          tempGArray[tempGIndex] = a;
          tempGArray[tempGIndex + 1] = d;
          tempGIndex += 2;
          //  nthChip[i5]  左から0,1,2,3
          //  3-nthChip[i5]  左から3,2,1,0
          int b = ((3 - nthChip[i5]) << 2);
          d &= ~((dataMask >> b) & 15);
          d |= ((dataHigh >> b) & 15);
          if (((dataFrozen >> b) & 15) != 0) {
            d |= (((dataFrozen >> b) & 15) & noiseArray[1 << 18 | p2 << 16 | r8c8]);
          }
          if (((dataDynamic >> b) & 15) != 0) {
            d |= (((dataDynamic >> b) & 15) & (random () >>> 28));
          }
          GraphicScreen.graM4[a] = (byte) d;
        }
      }
    }
    //描画する
    VideoController.vcnMode.drawRaster (src, dst, rh);
    //TVRAMを元に戻す
    while (0 < tempTIndex) {
      tempTIndex -= 2;
      MainMemory.mmrM8[tempTArray[tempTIndex]] = (byte) tempTArray[tempTIndex + 1];
    }
    //GVRAMを元に戻す
    while (0 < tempGIndex) {
      tempGIndex -= 2;
      GraphicScreen.graM4[tempGArray[tempGIndex]] = (byte) tempGArray[tempGIndex + 1];
    }
  }  //mlfRaster

  private static int x = 123456789;
  private static int y = 362436069;
  private static int z = 521288629;
  private static int w = 88675123;

  //w = random ()
  //  疑似乱数
  private static int random () {
    int t = x ^ (x << 11);
    x = y;
    y = z;
    z = w;
    return w = (w ^ (w >>> 19)) ^ (t ^ (t >>> 8));
  }  //random

}  //class Malfunction
