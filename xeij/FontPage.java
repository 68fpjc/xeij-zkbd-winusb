//========================================================================================
//  FontPage.java
//    en:Font page
//    ja:フォントページ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.font.*;  //FontRenderContext,LineMetrics,TextLayout
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,ByteArrayOutputStream,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile,UnsupportedEncodingException
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.imageio.*;  //ImageIO
import javax.imageio.stream.*;  //ImageOutputStream
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException




public class FontPage {



  //クラスフィールド

  //色
  //  1dotあたり2bit使用する
  //  bit0は背景の市松模様
  //  bit1は文字の有無
  public static final int FNP_COLOR_0 = 0x00;  //背景=黒,文字=なし
  public static final int FNP_COLOR_1 = 0x33;  //背景=灰,文字=なし
  public static final int FNP_COLOR_2 = 0xff;  //背景=黒,文字=あり
  public static final int FNP_COLOR_3 = 0xff;  //背景=灰,文字=あり
  public static final byte[] FNP_COLOR_BASE = new byte[] {
    (byte) FNP_COLOR_0,
    (byte) FNP_COLOR_1,
    (byte) FNP_COLOR_2,
    (byte) FNP_COLOR_3,
  };
  public static final Color[] FNP_COLOR_ARRAY = new Color[] {
    new Color (FNP_COLOR_0, FNP_COLOR_0, FNP_COLOR_0),
    new Color (FNP_COLOR_1, FNP_COLOR_1, FNP_COLOR_1),
    new Color (FNP_COLOR_2, FNP_COLOR_2, FNP_COLOR_2),
    new Color (FNP_COLOR_3, FNP_COLOR_3, FNP_COLOR_3),
  };

  //4bitのフォントデータ→4dotのパレットコード
  public static final byte[][] FNP_PALET = {
    //背景=黒黒黒黒
    {
      0b00_00_00_00,
      0b00_00_00_10,
      0b00_00_10_00,
      0b00_00_10_10,
      0b00_10_00_00,
      0b00_10_00_10,
      0b00_10_10_00,
      0b00_10_10_10,
      (byte) 0b10_00_00_00,
      (byte) 0b10_00_00_10,
      (byte) 0b10_00_10_00,
      (byte) 0b10_00_10_10,
      (byte) 0b10_10_00_00,
      (byte) 0b10_10_00_10,
      (byte) 0b10_10_10_00,
      (byte) 0b10_10_10_10,
    },
    //背景=灰灰灰灰
    {
      0b01_01_01_01,
      0b01_01_01_11,
      0b01_01_11_01,
      0b01_01_11_11,
      0b01_11_01_01,
      0b01_11_01_11,
      0b01_11_11_01,
      0b01_11_11_11,
      (byte) 0b11_01_01_01,
      (byte) 0b11_01_01_11,
      (byte) 0b11_01_11_01,
      (byte) 0b11_01_11_11,
      (byte) 0b11_11_01_01,
      (byte) 0b11_11_01_11,
      (byte) 0b11_11_11_01,
      (byte) 0b11_11_11_11,
    },
    //背景=黒黒灰灰
    {
      0b00_00_01_01,
      0b00_00_01_11,
      0b00_00_11_01,
      0b00_00_11_11,
      0b00_10_01_01,
      0b00_10_01_11,
      0b00_10_11_01,
      0b00_10_11_11,
      (byte) 0b10_00_01_01,
      (byte) 0b10_00_01_11,
      (byte) 0b10_00_11_01,
      (byte) 0b10_00_11_11,
      (byte) 0b10_10_01_01,
      (byte) 0b10_10_01_11,
      (byte) 0b10_10_11_01,
      (byte) 0b10_10_11_11,
    },
    //背景=灰灰黒黒
    {
      0b01_01_00_00,
      0b01_01_00_10,
      0b01_01_10_00,
      0b01_01_10_10,
      0b01_11_00_00,
      0b01_11_00_10,
      0b01_11_10_00,
      0b01_11_10_10,
      (byte) 0b11_01_00_00,
      (byte) 0b11_01_00_10,
      (byte) 0b11_01_10_00,
      (byte) 0b11_01_10_10,
      (byte) 0b11_11_00_00,
      (byte) 0b11_11_00_10,
      (byte) 0b11_11_10_00,
      (byte) 0b11_11_10_10,
    },
  };

  //4dotのパレットコード→4bitのフォントデータ
  public static final byte[] FNP_INV_PALET = new byte[256];
  static {
    for (int i = 0; i < 256; i++) {
      FNP_INV_PALET[i] = (byte) (i >> 4 & 8 | i >> 3 & 4 | i >> 2 & 2 | i >> 1 & 1);  //0bP.Q.R.S. → 0b0000PQRS
    }
  }



  //各フォントページはフォントデータを2つまたは3つの状態で保持する
  //  イメージ
  //    全角は94点×94区
  //    イメージファイルの内容
  //    フォントエディタに表示するイメージ。ビットマップ
  //  バイナリ
  //    全角は94点×94区
  //    フォントデータファイルの内容
  //    半角のバイナリと全角のバイナリが連結された状態のフォントデータファイルを読み書きできる
  //  メモリ
  //    ROMの内容。CGROMにないフォントは含まない
  //    全角は94点×77区(1区～8区,16区～84区)
  //    バイナリをメモリに変換するとき77区(1区～8区,16区～84区)をコピーして17区(9区～15区,85区～94区)を省く
  //    メモリをバイナリに変換するとき77区(1区～8区,16区～84区)にコピーして17区(9区～15区,85区～94区)を空白にする
  //    ROM.DAT(1MB)またはCGROM.DAT(768KB)から読み込む
  //
  //  IPLROMにあるHan6x12の扱い
  //    Han6x12のメモリのアドレスはCGROMを指す
  //    Han6x12がCGROMになくてIPLROMにあるときIPLROMからCGROMにコピーする
  //    CGROMにあるHan6x12を更新したらIPLROMにコピーする。これはピクセルの編集を含む



  //インスタンスフィールド
  public int fnpCharacterWidth;  //1文字の幅(ドット)
  public int fnpCharacterHeight;  //1文字の高さ(ドット)
  public String fnpNameEn;  //ページの名前(英語)
  public String fnpNameJa;  //ページの名前(日本語)
  public String fnpFontDataFileName;  //フォントデータファイル名。バイナリまたはバイナリを連結したもの
  public String fnpFontImageFileName;  //フォント画像ファイル名
  public int fnpImageCols;  //イメージとバイナリとメモリの桁数
  public int fnpImageRows;  //イメージとバイナリの行数
  public int fnpMemoryRows;  //メモリの行数。メモリがないときは0
  public byte[] fnpMemoryArray;  //メモリの配列。メモリがないときはnull
  public int fnpMemoryAddress;  //メモリのアドレス。メモリがないときは0
  public char[][] fnpTableArray;  //自動生成に使う文字テーブルの配列

  public int fnpImageWidth;  //イメージとバイナリとメモリの幅(ドット)
  public int fnpImageHeight;  //イメージとバイナリの高さ(ドット)
  public BufferedImage fnpImageObject;  //2ビットイメージのオブジェクト
  public byte[] fnpBitmapArray;  //2ビットイメージのビットマップ
  public int fnpBitmapRasterBytes;  //2ビットイメージのビットマップの1ラスタのバイト数。幅を4の倍数に切り上げて4で割る

  public int fnpCharacterHorizontalBytes;  //バイナリとメモリの1文字の水平方向のバイト数。幅を8の倍数に切り上げて8で割る
  public int fnpCharacterBytes;  //バイナリとメモリの1文字のバイト数
  public int fnpBinaryBytes;  //バイナリのバイト数
  public byte[] fnpBinaryArray;  //バイナリの配列
  public int fnpMinimumFontDataFileLength;  //フォントデータファイルの最小の長さ(バイト)
  public int fnpMaximumFontDataFileLength;  //フォントデータファイルの最大の長さ(バイト)

  public int fnpMemoryBytes;  //メモリのバイト数。メモリがないときは0

  public boolean fnpReady;  //true=フォントが有効。CGROM、フォントデータファイル、フォント画像ファイルのいずれかを読み込んだか、自動生成した
  public String fnpHostFontName;  //自動生成に使ったホストのフォント名

  public JFileChooser2 fnpFontDataFileChooser;  //フォントデータファイルチューザー
  public String fnpExtension;  //フォントデータファイルの拡張子
  public String fnpDescription;  //フォントデータファイルの説明

  public JFileChooser2 fnpFontImageFileChooser;  //フォント画像ファイルチューザー

  public boolean fnpEditted;  //true=編集あり。編集したらセット、フォントデータファイルまたはフォント画像ファイルに保存したらクリア



  //コンストラクタ
  public FontPage (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName,
                   int imageCols, int imageRows, int memoryRows,
                   byte[] memoryArray, int memoryAddress,
                   char[][] tableArray) {
    fnpCharacterWidth = characterWidth;
    fnpCharacterHeight = characterHeight;
    fnpNameEn = nameEn;
    fnpNameJa = nameJa;
    fnpFontDataFileName = dataName;
    fnpFontImageFileName = imageName;
    fnpImageCols = imageCols;
    fnpImageRows = imageRows;
    fnpMemoryRows = memoryArray == null ? 0 : memoryRows;
    fnpMemoryArray = memoryArray;
    fnpMemoryAddress = memoryArray == null ? 0 : memoryAddress;
    fnpTableArray = tableArray;

    fnpImageWidth = characterWidth * fnpImageCols;
    fnpImageHeight = characterHeight * fnpImageRows;
    fnpImageObject = new BufferedImage ((fnpImageWidth + 3) & -4, fnpImageHeight, BufferedImage.TYPE_BYTE_BINARY,
                                        new IndexColorModel (2, 4, FNP_COLOR_BASE, FNP_COLOR_BASE, FNP_COLOR_BASE));
    fnpBitmapArray = ((DataBufferByte) fnpImageObject.getRaster ().getDataBuffer ()).getData ();
    fnpBitmapRasterBytes = (fnpImageWidth + 3) >> 2;

    fnpCharacterHorizontalBytes = (characterWidth + 7) >> 3;
    fnpCharacterBytes = fnpCharacterHorizontalBytes * fnpCharacterHeight;
    fnpBinaryBytes = fnpCharacterBytes * fnpImageCols * fnpImageRows;
    fnpBinaryArray = new byte[fnpBinaryBytes];
    fnpMinimumFontDataFileLength = fnpBinaryBytes;
    fnpMaximumFontDataFileLength = fnpBinaryBytes;

    fnpMemoryBytes = memoryArray == null ? 0 : fnpCharacterBytes * fnpImageCols * fnpMemoryRows;

    fnpReady = false;
    fnpHostFontName = null;

    fnpFontDataFileChooser = null;
    fnpExtension = null;
    fnpDescription = null;

    fnpFontImageFileChooser = null;

    fnpBinaryToImage ();  //市松模様にする
  }



  //success = fnpInputMemory ()
  //  ロード済みのCGROMでフォントを構築する
  //  半角は'A'(0x41)、全角は'あ'(4区2点)が空白のとき失敗する
  public boolean fnpInputMemory () {
    if (fnpMemoryArray == null) {  //メモリがない
      return false;  //失敗
    }
    if (fnpImageCols == 16) {  //半角
      if (fnpIsBlankMemory (1, 4)) {  //'A'(0x41)が空白
        return false;  //失敗
      }
    } else if (fnpImageCols == 94) {  //全角
      if (fnpIsBlankMemory (1, 3)) {  //'あ'(4区2点)が空白
        return false;  //失敗
      }
    }
    fnpMemoryToBinary ();  //メモリをバイナリに変換する
    fnpBinaryToImage ();  //バイナリをイメージに変換する
    if (!fnpReady) {
      System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                          fnpNameEn + " font is ready");
      fnpReady = true;  //フォントデータが有効
    }
    return true;
  }



  //yes = fnpIsBlankMemory (col, memoryRow)
  //  メモリの指定された文字は空白か
  //  メモリがないときはtrueを返す
  public boolean fnpIsBlankMemory (int col, int memoryRow) {
    if (fnpMemoryArray != null) {  //メモリがある
      int start = fnpMemoryAddress + fnpCharacterBytes * (fnpImageCols * memoryRow + col);
      for (int i = 0; i < fnpCharacterBytes; i++) {
        if (fnpMemoryArray[start + i] != 0) {
          return false;
        }
      }
    }
    return true;
  }



  //memoryRow = fnpImageRowToMemoryRow (imageRow)
  //  イメージとバイナリの行番号をメモリの行番号に変換する
  //  メモリがないか対応する行がないときは-1を返す
  //  全角
  //    イメージ  1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 ... 83 84 85 86 87 88 89 90 91 92 93 94 区
  //              0 1 2 3 4 5 6 7 8  9 10 11 12 13 14 15 16 ... 82 83 84 85 86 87 88 89 90 91 92 93 行
  //              ---------------                     ---------------
  //      メモリ  0 1 2 3 4 5 6 7                      8  9     75 76 行
  //              1 2 3 4 5 6 7 8                     16 17 ... 83 84 区
  public int fnpImageRowToMemoryRow (int imageRow) {
    return (fnpMemoryArray == null ? -1 :
            fnpImageCols != 94 ? imageRow :
            imageRow <= 7 ? imageRow :
            imageRow <= 14 ? -1 :
            imageRow <= 83 ? imageRow - 7 :
            -1);
  }

  //imageRow = fnpMemoryRowToImageRow (memoryRow)
  //  メモリの行番号をイメージとバイナリの行番号に変換する
  //  メモリがないときは-1を返す
  public int fnpMemoryRowToImageRow (int memoryRow) {
    return (fnpMemoryArray == null ? -1 :
            fnpImageCols != 94 ? memoryRow :
            memoryRow <= 7 ? memoryRow :
            memoryRow + 7);
  }



  //fnpMemoryToBinary ()
  //  メモリをバイナリに変換する
  public void fnpMemoryToBinary () {
    if (fnpMemoryArray != null) {  //メモリがある
      for (int imageRow = 0; imageRow < fnpImageRows; imageRow++) {
        int memoryRow = fnpImageRowToMemoryRow (imageRow);
        if (0 <= memoryRow) {
          System.arraycopy (fnpMemoryArray, fnpMemoryAddress + fnpCharacterBytes * fnpImageCols * memoryRow,
                            fnpBinaryArray, fnpCharacterBytes * fnpImageCols * imageRow,
                            fnpCharacterBytes * fnpImageCols);
        } else {
          Arrays.fill (fnpBinaryArray,
                       fnpCharacterBytes * fnpImageCols * imageRow,
                       fnpCharacterBytes * fnpImageCols * (imageRow + 1),
                       (byte) 0);
        }
      }
    }
  }

  //fnpBinaryToMemory ()
  //  バイナリをメモリに変換する
  public void fnpBinaryToMemory () {
    if (fnpMemoryArray != null) {  //メモリがある
      for (int imageRow = 0; imageRow < fnpImageRows; imageRow++) {
        int memoryRow = fnpImageRowToMemoryRow (imageRow);
        if (0 <= memoryRow) {
          System.arraycopy (fnpBinaryArray, fnpCharacterBytes * fnpImageCols * imageRow,
                            fnpMemoryArray, fnpMemoryAddress + fnpCharacterBytes * fnpImageCols * memoryRow,
                            fnpCharacterBytes * fnpImageCols);
        }
      }
    }
  }



  //fnpBinaryToImage ()
  //  バイナリをイメージに変換する
  public final void fnpBinaryToImage () {
    byte[] bitmap = fnpBitmapArray;
    byte[] m = fnpBinaryArray;
    int a = 0;
    int o = fnpBitmapRasterBytes;
    int h = fnpCharacterHeight;
    switch (fnpCharacterWidth) {
    case 4:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (4 >> 2) * col;
            //    m[a]
            //  ABCD0000
            //     t
            //  0000ABCD
            //    b[i]
            //  A.B.C.D.
            bitmap[i] = palet[(m[a] & 255) >> 4];
            a++;
          }
        }
      }
      break;
    case 6:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col += 2) {  //2文字ずつ変換する
          //                                          偶数行   奇数行
          byte[] palet0 = FNP_PALET[    row & 1];  //黒黒黒黒 灰灰灰灰
          byte[] palet1 = FNP_PALET[2 | row & 1];  //黒黒灰灰 灰灰黒黒
          byte[] palet2 = FNP_PALET[   ~row & 1];  //灰灰灰灰 黒黒黒黒
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (6 * col >> 2);  //colは偶数
            //    m[a]    m[a+h]
            //  ABCDEF00 GHIJKL00
            //          t
            //  0000ABCD EFGHIJKL
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            int t = (m[a] & 253) << 4 | (m[a + h] & 253) >> 2;
            bitmap[i    ] = palet0[t >>  8     ];  //0000ABCD → A.B.C.D.
            bitmap[i + 1] = palet1[t >>  4 & 15];  //0000EFGH → E.F.G.H.
            bitmap[i + 2] = palet2[t       & 15];  //0000IJKL → I.J.K.L.
            a++;
          }
          a += h;
        }
      }
      break;
    case 8:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (8 >> 2) * col;
            //    m[a]
            //  ABCDEFGH
            //     t
            //  ABCDEFGH
            //    b[i]    b[i+1]
            //  A.B.C.D. E.F.G.H.
            int t = m[a] & 255;
            bitmap[i    ] = palet[t >>  4     ];
            bitmap[i + 1] = palet[t       & 15];
            a++;
          }
        }
      }
      break;
    case 12:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (12 >> 2) * col;
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKL0000
            //          t
            //  ABCDEFGH IJKL0000
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            int t = (char) (m[a] << 8 | m[a + 1] & 255);
            bitmap[i    ] = palet[t >> 12     ];
            bitmap[i + 1] = palet[t >>  8 & 15];
            bitmap[i + 2] = palet[t >>  4 & 15];
            a += 2;
          }
        }
      }
      break;
    case 16:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (16 >> 2) * col;
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKLMNOP
            //          t
            //  ABCDEFGH IJKLMNOP
            //    b[i]    b[i+1]   b[i+2]   b[i+3]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P.
            int t = (char) (m[a] << 8 | m[a + 1] & 255);
            bitmap[i    ] = palet[t >> 12     ];
            bitmap[i + 1] = palet[t >>  8 & 15];
            bitmap[i + 2] = palet[t >>  4 & 15];
            bitmap[i + 3] = palet[t       & 15];
            a += 2;
          }
        }
      }
      break;
    case 24:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (24 >> 2) * col;
            //    m[a]    m[a+1]   m[a+2]
            //  ABCDEFGH IJKLMNOP RSTUVWX
            //              t
            //  ABCDEFGH IJKLMNOP RSTUVWX
            //    b[i]    b[i+1]   b[i+2]   b[i+3]   b[i+4]   b[i+5]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P. Q.R.S.T. U.V.W.X.
            int t = (char) (m[a] << 8 | m[a + 1] & 255) << 8 | m[a + 2] & 255;
            bitmap[i    ] = palet[t >> 20     ];
            bitmap[i + 1] = palet[t >> 16 & 15];
            bitmap[i + 2] = palet[t >> 12 & 15];
            bitmap[i + 3] = palet[t >>  8 & 15];
            bitmap[i + 4] = palet[t >>  4 & 15];
            bitmap[i + 5] = palet[t       & 15];
            a += 3;
          }
        }
      }
      break;
    case 32:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (32 >> 2) * col;
            int t = m[a] << 24 | (m[a + 1] & 255) << 16 | (char) (m[a + 2] << 8 | m[a + 3] & 255);
            bitmap[i    ] = palet[t >>> 28     ];
            bitmap[i + 1] = palet[t >>> 24 & 15];
            bitmap[i + 2] = palet[t >>> 20 & 15];
            bitmap[i + 3] = palet[t >>> 16 & 15];
            bitmap[i + 4] = palet[t >>> 12 & 15];
            bitmap[i + 5] = palet[t >>>  8 & 15];
            bitmap[i + 6] = palet[t >>>  4 & 15];
            bitmap[i + 7] = palet[t        & 15];
            a += 4;
          }
        }
      }
      break;
    case 48:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                            偶数マス 奇数マス
          byte[] palet = FNP_PALET[(col ^ row) & 1];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (48 >> 2) * col;
            int t = m[a] << 24 | (m[a + 1] & 255) << 16 | (char) (m[a + 2] << 8 | m[a + 3] & 255);
            bitmap[i     ] = palet[t >>> 28     ];
            bitmap[i +  1] = palet[t >>> 24 & 15];
            bitmap[i +  2] = palet[t >>> 20 & 15];
            bitmap[i +  3] = palet[t >>> 16 & 15];
            bitmap[i +  4] = palet[t >>> 12 & 15];
            bitmap[i +  5] = palet[t >>>  8 & 15];
            bitmap[i +  6] = palet[t >>>  4 & 15];
            bitmap[i +  7] = palet[t        & 15];
            t = (char) (m[a + 4] << 8 | m[a + 5] & 255);
            bitmap[i +  8] = palet[t >>> 12 & 15];
            bitmap[i +  9] = palet[t >>>  8 & 15];
            bitmap[i + 10] = palet[t >>>  4 & 15];
            bitmap[i + 11] = palet[t        & 15];
            a += 6;
          }
        }
      }
      break;
/*
    default:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < fnpCharacterHeight; y++) {
            for (int x = 0; x < fnpCharacterWidth; x++) {
              fnpSetImagePixel (col, row, x, y, fnpGetBinaryPixel (col, row, x, y));
            }
          }
        }
      }
*/
    }
  }



  //fnpCreateImage (hostFontName)
  //  イメージを自動生成する
  public void fnpCreateImage (String hostFontName) {
    //開始
    System.out.println (Multilingual.mlnJapanese ?
                        hostFontName + " を使って " + fnpNameJa + " を作ります" :
                        "Creating " + fnpNameEn + " by using " + hostFontName);
    long startTime = System.currentTimeMillis ();
    //フォント
    fnpHostFontName = hostFontName;
    Font font = new Font (hostFontName, Font.PLAIN, fnpCharacterHeight);
    double fw = (double) fnpCharacterWidth;
    double fh = (double) fnpCharacterHeight;
    //文字テーブル
    char[][] tableArray = fnpTableArray;
    int numberOfTable = tableArray.length;  //文字のテーブルの数
    int lastTableIndex = -1;  //最後に描いた文字の文字テーブルの番号
    double px = 0.0;
    double py = 0.0;
    double pw = 0.0;
    double ph = 0.0;
    //イメージ
    Graphics2D g2 = (Graphics2D) fnpImageObject.getGraphics ();
    FontRenderContext frc = g2.getFontRenderContext ();
    //背景を黒で塗り潰す
    byte[] bitmap = fnpBitmapArray;
    Arrays.fill (bitmap, 0, fnpBitmapRasterBytes * fnpImageHeight, (byte) 0);
    //白い文字を描く
    g2.setColor (FNP_COLOR_ARRAY[2]);
    g2.setFont (font);
    //アフィン変換を保存する
    AffineTransform savedTransform = g2.getTransform ();
    //クリッピング領域を保存する
    Shape savedClip = g2.getClip ();
    //行ループ
    for (int row = 0; row < fnpImageRows; row++) {
      int gy = fnpCharacterHeight * row;  //イメージ内y座標
      //列ループ
    col:
      for (int col = 0; col < fnpImageCols; col++) {
        int gx = fnpCharacterWidth * col;  //イメージ内x座標
        //クリッピング領域を設定する
        //  クリッピングしないと1ドットはみ出すことがある
        g2.setClip (null);
        g2.clipRect (gx, gy, fnpCharacterWidth, fnpCharacterHeight);
        //描く文字を決める
        //  最初の文字テーブルでは空白でも次の文字テーブルでは空白でない場合があるので、
        //  空白が出てきても最後の文字テーブルまでループさせること
        char[] table;  //選択したテーブル
        String s;  //文字
        char c;  //文字の先頭
        int tableIndex = -1;  //文字テーブルの番号
        do {
          if (++tableIndex == numberOfTable) {  //どの文字テーブルの文字も描けない
            continue col;
          }
          table = tableArray[tableIndex];
          int i = col + fnpImageCols * row;
          if (table[1] != '\0') {  //1文字構成
            s = new String (table, i, 1);
          } else {  //2文字構成
            i <<= 1;
            if (table[i + 1] == '\0') {  //1文字
              s = new String (table, i, 1);
            } else {  //2文字
              s = new String (table, i, 2);
            }
          }
          c = s.charAt (0);
          //特殊記号を描く
          int t;
          switch (c) {
          case '\u2571':  //U+2571  BOX DRAWINGS LIGHT DIAGONAL UPPER RIGHT TO LOWER LEFT
            g2.setStroke (new BasicStroke ());
            g2.drawLine (gx + fnpCharacterWidth - 1, gy, gx, gy + fnpCharacterHeight - 1);
            continue col;
          case '\u2572':  //U+2572  BOX DRAWINGS LIGHT DIAGONAL UPPER LEFT TO LOWER RIGHT
            g2.setStroke (new BasicStroke ());
            g2.drawLine (gx, gy, gx + fnpCharacterWidth - 1, gy + fnpCharacterHeight - 1);
            continue col;
          case '\u2573':  //U+2573  BOX DRAWINGS LIGHT DIAGONAL CROSS
            g2.setStroke (new BasicStroke ());
            g2.drawLine (gx, gy, gx + fnpCharacterWidth - 1, gy + fnpCharacterHeight - 1);
            g2.drawLine (gx + fnpCharacterWidth - 1, gy, gx, gy + fnpCharacterHeight - 1);
            continue col;
          case '\u2581':  //U+2581  LOWER ONE EIGHTH BLOCK
            t = fnpCharacterHeight + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2582':  //U+2582  LOWER ONE QUARTER BLOCK
            t = fnpCharacterHeight * 2 + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2583':  //U+2583  LOWER THREE EIGHTHS BLOCK
            t = fnpCharacterHeight * 3 + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2584':  //U+2584  LOWER HALF BLOCK
            t = fnpCharacterHeight * 4 + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2585':  //U+2585  LOWER FIVE EIGHTHS BLOCK
            t = fnpCharacterHeight * 5 + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2586':  //U+2586  LOWER THREE QUARTERS BLOCK
            t = fnpCharacterHeight * 6 + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2587':  //U+2587  LOWER SEVEN EIGHTHS BLOCK
            t = fnpCharacterHeight * 7 + 4 >> 3;
            g2.fillRect (gx, gy + fnpCharacterHeight - t, fnpCharacterWidth, t);
            continue col;
          case '\u2588':  //U+2588  FULL BLOCK
            g2.fillRect (gx, gy, fnpCharacterWidth, fnpCharacterHeight);
            continue col;
          case '\u2589':  //U+2589  LEFT SEVEN EIGHTHS BLOCK
            t = fnpCharacterWidth * 7 + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u258a':  //U+258A  LEFT THREE QUARTERS BLOCK
            t = fnpCharacterWidth * 6 + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u258b':  //U+258B  LEFT FIVE EIGHTHS BLOCK
            t = fnpCharacterWidth * 5 + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u258c':  //U+258C  LEFT HALF BLOCK
            t = fnpCharacterWidth * 4 + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u258d':  //U+258D  LEFT THREE EIGHTHS BLOCK
            t = fnpCharacterWidth * 3 + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u258e':  //U+258E  LEFT ONE QUARTER BLOCK
            t = fnpCharacterWidth * 2 + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u258f':  //U+258F  LEFT ONE EIGHTH BLOCK
            t = fnpCharacterWidth + 4 >> 3;
            g2.fillRect (gx, gy, t, fnpCharacterHeight);
            continue col;
          case '\u2593':  //U+2593  DARK SHADE
            for (int v = 0; v < fnpCharacterHeight; v++) {
              for (int u = 0; u < fnpCharacterWidth; u++) {
                if (((u ^ v) & 1) == 0) {
                  g2.fillRect (gx + u, gy + v, 1, 1);
                }
              }
            }
            continue col;
          case '\u2596':  //U+2596  QUADRANT LOWER LEFT
            g2.fillRect (gx, gy + (fnpCharacterHeight >> 1), fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            continue col;
          case '\u2597':  //U+2597  QUADRANT LOWER RIGHT
            g2.fillRect (gx + (fnpCharacterWidth >> 1), gy + (fnpCharacterHeight >> 1), fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            continue col;
          case '\u2598':  //U+2598  QUADRANT UPPER LEFT
            g2.fillRect (gx, gy, fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            continue col;
          case '\u259a':  //U+259A  QUADRANT UPPER LEFT AND LOWER RIGHT
            g2.fillRect (gx, gy, fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            g2.fillRect (gx + (fnpCharacterWidth >> 1), gy + (fnpCharacterHeight >> 1), fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            continue col;
          case '\u259d':  //U+259D  QUADRANT UPPER RIGHT
            g2.fillRect (gx + (fnpCharacterWidth >> 1), gy, fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            continue col;
          case '\u259e':  //U+259E  QUADRANT UPPER RIGHT AND LOWER LEFT
            g2.fillRect (gx + (fnpCharacterWidth >> 1), gy, fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            g2.fillRect (gx, gy + (fnpCharacterHeight >> 1), fnpCharacterWidth >> 1, fnpCharacterHeight >> 1);
            continue col;
            //case '\u25a1':  //U+25A1  WHITE SQUARE
            //  g2.drawRect (gx, gy, fnpCharacterWidth - 1, fnpCharacterHeight - 1);
            //  continue col;
          case '\u25e2':  //U+25E2  BLACK LOWER RIGHT TRIANGLE
            g2.setStroke (new BasicStroke ());
            if (fnpCharacterWidth <= fnpCharacterHeight) {  //縦長
              for (int v = 0; v <= fnpCharacterHeight - 1; v++) {
                int u = v * (fnpCharacterWidth - 1) / (fnpCharacterHeight - 1);
                g2.drawLine (gx + fnpCharacterWidth - 1 - u, gy + v, gx + fnpCharacterWidth - 1, gy + v);  //横線
              }
            } else {  //横長
              for (int u = 0; u <= fnpCharacterWidth - 1; u++) {
                int v = u * (fnpCharacterHeight - 1) / (fnpCharacterWidth - 1);
                g2.drawLine (gx + u, gy + fnpCharacterHeight - 1 - v, gx + u, gy + fnpCharacterHeight - 1);  //縦線
              }
            }
            continue col;
          case '\u25e3':  //U+25E3  BLACK LOWER LEFT TRIANGLE
            g2.setStroke (new BasicStroke ());
            if (fnpCharacterWidth <= fnpCharacterHeight) {  //縦長
              for (int v = 0; v <= fnpCharacterHeight - 1; v++) {
                int u = v * (fnpCharacterWidth - 1) / (fnpCharacterHeight - 1);
                g2.drawLine (gx, gy + v, gx + u, gy + v);  //横線
              }
            } else {  //横長
              for (int u = 0; u <= fnpCharacterWidth - 1; u++) {
                int v = u * (fnpCharacterHeight - 1) / (fnpCharacterWidth - 1);
                g2.drawLine (gx + u, gy + v, gx + u, gy + fnpCharacterHeight - 1);  //縦線
              }
            }
            continue col;
          }
        } while (c == ' ' || c == '　' || font.canDisplayUpTo (s) != -1);  //空白または描けないときは次の文字テーブルへ
        //フォントの枠を決める
        //  半角はFULL BLOCK、全角は罫線の十字のレクタングルを基準にする
        if (lastTableIndex != tableIndex) {
          lastTableIndex = tableIndex;
          Rectangle2D p = new TextLayout (table[0] < 0x80 ? "\u2588" : "┼", font, g2.getFontRenderContext ()).getBounds ();
          px = p.getX ();
          py = p.getY ();
          pw = p.getWidth ();
          ph = p.getHeight ();
        }
        //今回の文字のレクタングルを求める
        Rectangle2D q = new TextLayout (s, font, g2.getFontRenderContext ()).getBounds ();
        double qx = q.getX ();
        double qy = q.getY ();
        double qw = q.getWidth ();
        double qh = q.getHeight ();
        //  x=sx*x+tx
        //  y=sy*y+ty
        double sx = 1.0;
        double sy = 1.0;
        double tx = 0.0;
        double ty = 0.0;
        //{px,py,pw,ph}から{0,0,pw,ph}へ移動する
        //  x=(sx*x+tx)-px
        //   =sx*x+(tx-px)
        //  y=(sy*y+ty)-py
        //   =sy*y+(ty-py)
        qx -= px;
        qy -= py;
        tx -= px;
        ty -= py;
        //{qx,qy,qw,qh}が{0,0,pw,ph}に収まるようにする
        //  既に収まっているときは中央になくても動かしてはならない
        if (pw < qw) {  //幅が大きすぎるので水平方向に縮小してずらす
          //  x=((sx*x+tx)-qx)*pw/qw
          //   =sx*pw/qw*x+(tx-qx)*pw/qw
          double r = pw / qw;
          sx *= r;
          tx = (tx - qx) * r;
        } else if (qx < 0) {  //左にはみ出しているので右にずらす
          //  x=(sx*x+tx)-qx
          //   =sx*x+(tx-qx)
          tx -= qx;
        } else if (pw < qx + qw) {  //右にはみ出しているので左にずらす
          //  x=(sx*x+tx)-(qx+qw-pw)
          //   =sx*x+(tx-(qx+qw-pw))
          tx -= qx + qw - pw;
        }
        if (ph < qh) {  //高さが大きすぎるので垂直方向に縮小してずらす
          //  y=((sy*y+ty)-qy)*ph/qh
          //   =sy*ph/qh*y+(ty-qy)*ph/qh
          double r = ph / qh;
          sy *= r;
          ty = (ty - qy) * r;
        } else if (qy < 0) {  //上にはみ出しているので下にずらす
          //  y=(sy*y+ty)-qy
          //   =sy*y+(ty-qy)
          ty -= qy;
        } else if (ph < qy + qh) {  //下にはみ出しているので上にずらす
          //  y=(sy*y+ty)-(qy+qh-ph)
          //   =sy*y+(ty-(qy+qh-ph))
          ty -= qy + qh - ph;
        }
        //{0,0,pw,ph}を{fw*col,fh*row,fw,fh}に拡大してずらす
        //  x=(sx*x+tx)*fw/pw+fw*col
        //   =(sx*fw/pw)*x+(tx*fw/pw+fw*col)
        //  y=(sy*y+ty)*fh/ph+fh*row
        //   =(sy*fh/ph)*y+(ty*fh/ph+fh*row)
        {
          double r = fw / pw;
          sx *= r;
          tx = tx * r + fw * (double) col;
        }
        {
          double r = fh / ph;
          sy *= r;
          ty = ty * r + fh * (double) row;
        }
        //アフィン変換を設定する
        g2.translate (tx, ty);
        g2.scale (sx, sy);
        //フォントを描く
        g2.drawString (s, 0, 0);
        //アフィン変換を復元する
        g2.setTransform (savedTransform);
      }  //for col
    }  //for row
    //クリッピング領域を復元する
    g2.setClip (savedClip);
    //背景を市松模様に塗る
    //  フォントが枠からはみ出していないか確認できる
    int o = fnpBitmapRasterBytes;
    int w = fnpCharacterWidth;
    int h = fnpCharacterHeight;
    switch (fnpCharacterWidth) {
    case 4:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (4 >> 2) * col;
            bitmap[i] |= p;
          }
        }
      }
      break;
    case 6:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col += 2) {  //2文字ずつ変換する
          //                                       偶数行   奇数行
          byte p0 = FNP_PALET[    row & 1][0];  //黒黒黒黒 灰灰灰灰
          byte p1 = FNP_PALET[2 | row & 1][0];  //黒黒灰灰 灰灰黒黒
          byte p2 = FNP_PALET[   ~row & 1][0];  //灰灰灰灰 黒黒黒黒
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (6 * col >> 2);
            bitmap[i    ] |= p0;
            bitmap[i + 1] |= p1;
            bitmap[i + 2] |= p2;
          }
        }
      }
      break;
    case 8:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (8 >> 2) * col;
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
          }
        }
      }
      break;
    case 12:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (12 >> 2) * col;
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
          }
        }
      }
      break;
    case 16:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (16 >> 2) * col;
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
          }
        }
      }
      break;
    case 24:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (24 >> 2) * col;
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
            bitmap[i + 4] |= p;
            bitmap[i + 5] |= p;
          }
        }
      }
      break;
    case 32:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (32 >> 2) * col;
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
            bitmap[i + 4] |= p;
            bitmap[i + 5] |= p;
            bitmap[i + 6] |= p;
            bitmap[i + 7] |= p;
          }
        }
      }
      break;
    case 36:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (36 >> 2) * col;
            bitmap[i    ] |= p;
            bitmap[i + 1] |= p;
            bitmap[i + 2] |= p;
            bitmap[i + 3] |= p;
            bitmap[i + 4] |= p;
            bitmap[i + 5] |= p;
            bitmap[i + 6] |= p;
            bitmap[i + 7] |= p;
            bitmap[i + 8] |= p;
          }
        }
      }
      break;
    case 48:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          //                                         偶数マス 奇数マス
          byte p = FNP_PALET[(col ^ row) & 1][0];  //黒黒黒黒 灰灰灰灰
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (48 >> 2) * col;
            bitmap[i     ] |= p;
            bitmap[i +  1] |= p;
            bitmap[i +  2] |= p;
            bitmap[i +  3] |= p;
            bitmap[i +  4] |= p;
            bitmap[i +  5] |= p;
            bitmap[i +  6] |= p;
            bitmap[i +  7] |= p;
            bitmap[i +  8] |= p;
            bitmap[i +  9] |= p;
            bitmap[i + 10] |= p;
            bitmap[i + 11] |= p;
          }
        }
      }
      break;
/*
    default:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < fnpCharacterHeight; y++) {
            for (int x = 0; x < fnpCharacterWidth; x++) {
              fnpSetImagePixel (col, row, x, y, fnpGetImagePixel (col, row, x, y));
            }
          }
        }
      }
*/
    }
    fnpImageToBinary ();  //イメージをバイナリに変換する
    fnpBinaryToMemory ();  //バイナリをメモリに変換する
    //終了
    long elapsedTime = System.currentTimeMillis () - startTime;
    //if (1000L <= elapsedTime) {
    System.out.println (elapsedTime + "ms");
    //}
    if (!fnpReady) {
      System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                          fnpNameEn + " font is ready");
      fnpReady = true;  //フォントデータが有効
    }
  }  //fnpCreateImage

  //fnpImageToBinary ()
  //  イメージをバイナリに変換する
  public void fnpImageToBinary () {
    byte[] bitmap = fnpBitmapArray;
    byte[] m = fnpBinaryArray;
    int a = 0;
    int o = fnpBitmapRasterBytes;
    int h = fnpCharacterHeight;
    switch (fnpCharacterWidth) {
    case 4:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (4 >> 2) * col;
            //    b[i]
            //  A.B.C.D.
            //    m[a]
            //  ABCD0000
            m[a] = (byte) (FNP_INV_PALET[bitmap[i] & 255] << 4);
            a++;
          }
        }
      }
      break;
    case 6:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col += 2) {  //2文字ずつ変換する
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (6 * col >> 2);
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            //              t
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            //    m[a]    m[a+h]
            //  ABCDEF00 GHIJKL00
            int t = (char) (bitmap[i] << 8 | bitmap[i + 1] & 255) << 8 | bitmap[i + 2] & 255;
            m[a    ] = (byte) (FNP_INV_PALET[t >> 16      ] << 4 |  //0000ABCD → ABCD0000
                               FNP_INV_PALET[t >>  8 & 240]);       //E.F.0000 → 0000EF00
            m[a + h] = (byte) (FNP_INV_PALET[t >>  4 & 255] << 4 |  //G.H.I.J. → GHIJ0000
                               FNP_INV_PALET[t <<  4 & 240]);       //K.L.0000 → 0000KL00
            a++;
          }
          a += h;
        }
      }
      break;
    case 8:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (8 >> 2) * col;
            //    b[i]    b[i+1]
            //  A.B.C.D. E.F.G.H.
            //    m[a]
            //  ABCDEFGH
            m[a] = (byte) (FNP_INV_PALET[bitmap[i] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            a++;
          }
        }
      }
      break;
    case 12:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (12 >> 2) * col;
            //    b[i]    b[i+1]   b[i+2]
            //  A.B.C.D. E.F.G.H. I.J.K.L.
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKL0000
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4);
            a += 2;
          }
        }
      }
      break;
    case 16:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (16 >> 2) * col;
            //    b[i]    b[i+1]   b[i+2]   b[i+3]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P.
            //    m[a]    m[a+1]
            //  ABCDEFGH IJKLMNOP
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            a += 2;
          }
        }
      }
      break;
    case 18:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col += 2) {  //2文字ずつ変換する
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (18 * col >> 2);
            long t = ((long) FNP_INV_PALET[bitmap[i    ] & 255] << 32 |
                      (long) FNP_INV_PALET[bitmap[i + 1] & 255] << 28 |
                      (long) FNP_INV_PALET[bitmap[i + 2] & 255] << 24 |
                      (long) FNP_INV_PALET[bitmap[i + 3] & 255] << 20 |
                      (long) FNP_INV_PALET[bitmap[i + 4] & 255] << 16 |
                      (long) FNP_INV_PALET[bitmap[i + 5] & 255] << 12 |
                      (long) FNP_INV_PALET[bitmap[i + 6] & 255] <<  8 |
                      (long) FNP_INV_PALET[bitmap[i + 7] & 255] <<  4 |
                      (long) FNP_INV_PALET[bitmap[i + 8] & 255]);
            //  333333222222222211111111110000000000
            //  543210987654321098765432109876543210
            //  000000001111111122......
            //                    000000001111111122......
            m[a            ] = (byte) (t >> 28);
            m[a         + 1] = (byte) (t >> 20);
            m[a         + 2] = (byte) ((t >> 12) & 0xc0);
            m[a + 3 * h    ] = (byte) (t >> 10);
            m[a + 3 * h + 1] = (byte) (t >>  2);
            m[a + 3 * h + 2] = (byte) (t <<  6);
            a += 3;
          }
          a += 3 * h;
        }
      }
      break;
    case 24:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (24 >> 2) * col;
            //    b[i]    b[i+1]   b[i+2]   b[i+3]   b[i+4]   b[i+5]
            //  A.B.C.D. E.F.G.H. I.J.K.L. M.N.O.P. Q.R.S.T. U.V.W.X.
            //    m[a]    m[a+1]   m[a+2]
            //  ABCDEFGH IJKLMNOP RSTUVWX
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i + 4] & 255] << 4 | FNP_INV_PALET[bitmap[i + 5] & 255]);
            a += 3;
          }
        }
      }
      break;
    case 28:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (28 >> 2) * col;
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i + 4] & 255] << 4 | FNP_INV_PALET[bitmap[i + 5] & 255]);
            m[a + 3] = (byte) (FNP_INV_PALET[bitmap[i + 6] & 255] << 4);
            a += 4;
          }
        }
      }
      break;
    case 30:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col += 2) {  //2文字ずつ変換する
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (30 * col >> 2);
            long t = ((long) FNP_INV_PALET[bitmap[i     ] & 255] << 56 |
                      (long) FNP_INV_PALET[bitmap[i +  1] & 255] << 52 |
                      (long) FNP_INV_PALET[bitmap[i +  2] & 255] << 48 |
                      (long) FNP_INV_PALET[bitmap[i +  3] & 255] << 44 |
                      (long) FNP_INV_PALET[bitmap[i +  4] & 255] << 40 |
                      (long) FNP_INV_PALET[bitmap[i +  5] & 255] << 36 |
                      (long) FNP_INV_PALET[bitmap[i +  6] & 255] << 32 |
                      (long) FNP_INV_PALET[bitmap[i +  7] & 255] << 28 |
                      (long) FNP_INV_PALET[bitmap[i +  8] & 255] << 24 |
                      (long) FNP_INV_PALET[bitmap[i +  9] & 255] << 20 |
                      (long) FNP_INV_PALET[bitmap[i + 10] & 255] << 16 |
                      (long) FNP_INV_PALET[bitmap[i + 11] & 255] << 12 |
                      (long) FNP_INV_PALET[bitmap[i + 12] & 255] <<  8 |
                      (long) FNP_INV_PALET[bitmap[i + 13] & 255] <<  4 |
                      (long) FNP_INV_PALET[bitmap[i + 14] & 255]);
            //  555555555544444444443333333333222222222211111111110000000000
            //  987654321098765432109876543210987654321098765432109876543210
            //  000000001111111122222222333333..
            //                                000000001111111122222222333333..
            m[a            ] = (byte) (t >> 52);
            m[a         + 1] = (byte) (t >> 44);
            m[a         + 2] = (byte) (t >> 36);
            m[a         + 3] = (byte) ((t >> 28) & 0xfc);
            m[a + 4 * h    ] = (byte) (t >> 22);
            m[a + 4 * h + 1] = (byte) (t >> 14);
            m[a + 4 * h + 2] = (byte) (t >>  6);
            m[a + 4 * h + 3] = (byte) (t <<  2);
            a += 4;
          }
          a += 4 * h;
        }
      }
      break;
    case 32:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (32 >> 2) * col;
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i + 4] & 255] << 4 | FNP_INV_PALET[bitmap[i + 5] & 255]);
            m[a + 3] = (byte) (FNP_INV_PALET[bitmap[i + 6] & 255] << 4 | FNP_INV_PALET[bitmap[i + 7] & 255]);
            a += 4;
          }
        }
      }
      break;
    case 36:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (36 >> 2) * col;
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i    ] & 255] << 4 | FNP_INV_PALET[bitmap[i + 1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i + 2] & 255] << 4 | FNP_INV_PALET[bitmap[i + 3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i + 4] & 255] << 4 | FNP_INV_PALET[bitmap[i + 5] & 255]);
            m[a + 3] = (byte) (FNP_INV_PALET[bitmap[i + 6] & 255] << 4 | FNP_INV_PALET[bitmap[i + 7] & 255]);
            m[a + 4] = (byte) (FNP_INV_PALET[bitmap[i + 8] & 255] << 4);
            a += 5;
          }
        }
      }
      break;
    case 48:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < h; y++) {
            int i = o * (h * row + y) + (48 >> 2) * col;
            m[a    ] = (byte) (FNP_INV_PALET[bitmap[i     ] & 255] << 4 | FNP_INV_PALET[bitmap[i +  1] & 255]);
            m[a + 1] = (byte) (FNP_INV_PALET[bitmap[i +  2] & 255] << 4 | FNP_INV_PALET[bitmap[i +  3] & 255]);
            m[a + 2] = (byte) (FNP_INV_PALET[bitmap[i +  4] & 255] << 4 | FNP_INV_PALET[bitmap[i +  5] & 255]);
            m[a + 3] = (byte) (FNP_INV_PALET[bitmap[i +  6] & 255] << 4 | FNP_INV_PALET[bitmap[i +  7] & 255]);
            m[a + 4] = (byte) (FNP_INV_PALET[bitmap[i +  8] & 255] << 4 | FNP_INV_PALET[bitmap[i +  9] & 255]);
            m[a + 5] = (byte) (FNP_INV_PALET[bitmap[i + 10] & 255] << 4 | FNP_INV_PALET[bitmap[i + 11] & 255]);
            a += 6;
          }
        }
      }
      break;
/*
    default:
      for (int row = 0; row < fnpImageRows; row++) {
        for (int col = 0; col < fnpImageCols; col++) {
          for (int y = 0; y < fnpCharacterHeight; y++) {
            for (int x = 0; x < fnpCharacterWidth; x++) {
              fnpSetBinaryPixel (col, row, x, y, fnpGetImagePixel (col, row, x, y));
            }
          }
        }
      }
*/
    }
  }



/*
  //d = fnpGetImagePixel (imageCol, imageRow, characterX, characterY)
  //  イメージから1ピクセルゲットする
  public int fnpGetImagePixel (int imageCol, int imageRow, int characterX, int characterY) {
    int imageX = fnpCharacterWidth * imageCol + characterX;
    int imageY = fnpCharacterHeight * imageRow + characterY;
    int bitmapIndex = fnpBitmapRasterBytes * imageY + (imageX >> 2);  //ビットマップのインデックス
    int bitmapBit = 1 + ((~imageX & 3) << 1);  //ビットマップのビット番号
    return (fnpBitmapArray[bitmapIndex] >> bitmapBit) & 1;  //ビットマップからゲット
  }  //fnpGetImagePixel

  //fnpSetImagePixel (imageCol, imageRow, characterX, characterY, d)
  //  イメージに1ピクセルセットする
  public void fnpSetImagePixel (int imageCol, int imageRow, int characterX, int characterY, int d) {
    int imageX = fnpCharacterWidth * imageCol + characterX;
    int imageY = fnpCharacterHeight * imageRow + characterY;
    int bitmapIndex = fnpBitmapRasterBytes * imageY + (imageX >> 2);  //ビットマップのインデックス
    int bitmapBit = 1 + ((~imageX & 3) << 1);  //ビットマップのビット番号
    fnpBitmapArray[bitmapIndex] = (byte) ((fnpBitmapArray[bitmapIndex] & ~(1 << bitmapBit)) | ((d & 1) << bitmapBit));  //ビットマップにセット
  }  //fnpSetImagePixel
*/



  //fnpGetExtension ()
  //  フォントデータファイルの拡張子を返す
  //  全角と半角は.fonまたは.f<1文字の高さ>、それ以外は.dat
  public String fnpGetExtension () {
    if (fnpExtension == null) {
      fnpExtension = ".dat";
    }
    return fnpExtension;
  }

  //fnpGetDescription ()
  //  フォントデータファイルの説明を返す
  public String fnpGetDescription () {
    if (fnpDescription == null) {
      fnpDescription = (Multilingual.mlnJapanese ?
                        fnpNameJa + " フォントデータファイル (*" + fnpGetExtension () + ")" :
                        fnpNameEn + " font data files (*" + fnpGetExtension () + ")");
    }
    return fnpDescription;
  }

  //fnpMakeFontDataFileChooser ()
  //  フォントデータファイルチューザーを作る
  public void fnpMakeFontDataFileChooser () {
    if (fnpFontDataFileChooser == null) {
      fnpFontDataFileChooser = new JFileChooser2 (new File (fnpFontDataFileName));
      fnpFontDataFileChooser.setMultiSelectionEnabled (false);  //複数選択不可
      fnpFontDataFileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
        @Override public boolean accept (File file) {
          return (file.isDirectory () ||  //ディレクトリまたは
                  (file.isFile () &&  //ファイルで
                   fnpIsFontDataFileLength (file.length ()) &&  //フォントデータファイルの長さで
                   file.getName ().toLowerCase ().endsWith (fnpGetExtension ())));  //拡張子が合っている
        }
        @Override public String getDescription () {
          return fnpGetDescription ();
        }
      });
    }
  }



  //fnpOpenLoadFontDataFileChooser ()
  //  フォントデータファイルチューザーを開いて選択されたフォントデータファイルを読み込む
  public void fnpOpenLoadFontDataFileChooser () {
    fnpMakeFontDataFileChooser ();
    if (fnpFontDataFileChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fnpFontDataFileChooser.getSelectedFile ();
      if (fnpLoadFontDataFile (file)) {  //ロードできたら
        fnpFontDataFileName = file.getPath ();  //最後に使ったファイル
        fnpFontDataFileChooser.addHistory (file);  //ヒストリに加える
      }
    }
  }

  //success = fnpLoadFontDataFiles (names)
  //  列挙されたフォントデータファイルを読み込む
  //  1個読み込めた時点で終了する
  public boolean fnpLoadFontDataFiles (String names) {
    boolean previousReady = fnpReady;
    fnpReady = false;  //一旦not readyにする
    for (String name : names.split (",")) {  //ファイル名を","で区切って先頭から順に
      name = name.trim ();  //前後の空白を削除して
      if (!(name.length () == 0 || name.equalsIgnoreCase ("none"))) {  //""または"none"でなければ
        fnpLoadFontDataFile (new File (name));  //読み込む
        if (fnpReady) {  //読み込めたら
          if (!previousReady) {
            System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                                fnpNameEn + " font is ready");
            previousReady = true;  //フォントデータが有効
          }
          return true;  //終わり
        }
        System.out.println (Multilingual.mlnJapanese ?
                            name + " を読み込めません" :
                            "Cannot read " + name);
      }
    }
    fnpReady = previousReady;
    return false;  //1個も読み込めなかった
  }

  //success = fnpLoadFontDataFile (file)
  //  フォントデータファイルを読み込む
  public boolean fnpLoadFontDataFile (File file) {
    if (file.isFile () &&  //ファイルがある
        (long) fnpMinimumFontDataFileLength <= file.length () &&  //短すぎない
        file.length () <= (long) fnpMaximumFontDataFileLength) {  //長すぎない
      try (BufferedInputStream bis = new BufferedInputStream (new FileInputStream (file))) {
        byte[] array = new byte[fnpMaximumFontDataFileLength];
        int length = 0;  //読み込んだ長さ
        while (length < fnpMaximumFontDataFileLength) {
          int t = bis.read (array, length, fnpMaximumFontDataFileLength - length);
          if (t < 0) {
            break;
          }
          length += t;
        }
        if (fnpIsFontDataFileLength ((long) length) &&  //長さが合っている
            fnpLoadFontDataArray (array, 0, length)) {  //読み込めた
          return true;
        }
      } catch (IOException ioe) {
      }
    }
    return false;
  }

  //yes = fnpIsFontDataFileLength (longLength)
  //  フォントデータファイルの長さか
  //  半角と全角は半角+全角を受け付ける
  public boolean fnpIsFontDataFileLength (long longLength) {
    return longLength == (long) fnpBinaryBytes;
  }

  //success = fnpLoadFontDataArray (array)
  public boolean fnpLoadFontDataArray (byte[] array) {
    return fnpLoadFontDataArray (array, 0, array.length);
  }
  //success = fnpLoadFontDataArray (array, start, length)
  //  フォントデータファイルを配列から読み込む
  //  半角と全角は半角+全角を受け付ける
  public boolean fnpLoadFontDataArray (byte[] array, int start, int length) {
    if (length == fnpBinaryBytes) {
      System.arraycopy (array, start, fnpBinaryArray, 0, length);  //配列からバイナリへコピーする
      fnpBinaryToImage ();  //バイナリをイメージに変換する
      fnpBinaryToMemory ();  //バイナリをメモリに変換する
      if (!fnpReady) {
        System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                            fnpNameEn + " font is ready");
        fnpReady = true;  //フォントデータが有効
      }
      return true;  //成功
    }
    return false;  //失敗
  }



  //fnpOpenSaveFontDataFileChooser ()
  //  フォントデータファイルチューザーを開いて選択されたフォントデータファイルに書き出す
  public void fnpOpenSaveFontDataFileChooser () {
    fnpMakeFontDataFileChooser ();
    if (fnpFontDataFileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fnpFontDataFileChooser.getSelectedFile ();
      if (fnpSaveFontDataFile (file)) {  //書き出せた
        fnpFontDataFileName = file.getPath ();  //最後に使ったファイル
        fnpFontDataFileChooser.addHistory (file);  //ヒストリに加える
        fnpEditted = false;  //編集なし
      }
    }
  }

  //success = fnpSaveFontDataFile (file)
  //  フォントデータファイルに書き出す
  public boolean fnpSaveFontDataFile (File file) {
    String path = file.getAbsolutePath ();  //ファイル名
    file = new File (path);
    String pathBak = path + ".bak";  //バックアップファイル名
    String pathTmp = path + ".tmp";  //テンポラリファイル名
    File fileBak = new File (pathBak);  //バックアップファイル
    File fileTmp = new File (pathTmp);  //テンポラリファイル
    if (file.exists ()) {  //ファイルがある
      //上書きの確認
      if (JOptionPane.showConfirmDialog (
        null,
        Multilingual.mlnJapanese ? path + " に上書きしますか？" : "Do you want to overwrite " + path + " ?",
        Multilingual.mlnJapanese ? "上書きの確認" : "Overwrite confirmation",
        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {  //上書きが許可されなかった
        return false;
      }
    }
    if (fileTmp.isFile ()) {  //テンポラリファイルがある
      //テンポラリファイルを削除する。ゴミが残っていた場合
      if (!fileTmp.delete ()) {  //テンポラリファイルを削除できない
        JOptionPane.showMessageDialog (
          null, Multilingual.mlnJapanese ? pathTmp + " を削除できません" : "Cannot delete " + pathTmp);
        return false;
      }
    }
    //テンポラリファイルに書き出す
    byte[] array = fnpSaveFontDataArray ();
    try (BufferedOutputStream bos = new BufferedOutputStream (new FileOutputStream (fileTmp))) {
      bos.write (array, 0, array.length);
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog (
        null, Multilingual.mlnJapanese ? pathTmp + " に書き出せません" : "Cannot write " + pathTmp);
      return false;
    }
    if (file.exists ()) {  //ファイルがある
      if (fileBak.isFile ()) {  //バックアップファイルがある
        //バックアップファイルを削除する
        //  リネームは上書きしないので明示的に削除しなければならない
        if (!fileBak.delete ()) {  //バックアップファイルを削除できない
          JOptionPane.showMessageDialog (
            null, Multilingual.mlnJapanese ? pathBak + " を削除できません" : "Cannot delete " + pathBak);
          return false;
        }
      }
      //ファイルをバックアップファイルにリネームする
      if (!file.renameTo (fileBak)) {  //ファイルをバックアップファイルにリネームできない
        JOptionPane.showMessageDialog (
          null, Multilingual.mlnJapanese ? path + " を " + pathBak + " にリネームできません" : "Cannot rename " + path + " to " + pathBak);
        return false;
      }
    }
    //テンポラリファイルをファイルにリネームする
    if (!fileTmp.renameTo (file)) {  //テンポラリファイルをファイルにリネームできない
      JOptionPane.showMessageDialog (
        null, Multilingual.mlnJapanese ? pathTmp + " を " + path + " にリネームできません" : "Cannot rename " + pathTmp + " to " + path);
      return false;
    }
    return true;
  }

  //array = fnpSaveFontDataArray ()
  //  フォントデータファイルの内容の配列を返す
  //  全角は半角+全角を出力する
  public byte[] fnpSaveFontDataArray () {
    return fnpBinaryArray;
  }



  //fnpZeroClear ()
  //  ゼロクリアする
  //  設定ファイルに保存する必要のないフォントをゼロクリアすると設定ファイルが軽くなる
  public void fnpZeroClear () {
    if (JOptionPane.showConfirmDialog (
      null,
      Multilingual.mlnJapanese ? fnpNameJa + " をゼロクリアします" : "Zero clear " + fnpNameEn + " ?",
      Multilingual.mlnJapanese ? "ゼロクリアの確認" : "Zero clear confirmation",
      JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {  //ゼロクリアが許可されなかった
      return;
    }
    Arrays.fill (fnpBinaryArray, 0, fnpBinaryBytes, (byte) 0);  //バイナリをゼロクリアする
    fnpBinaryToImage ();  //バイナリをイメージに変換する
    fnpBinaryToMemory ();  //バイナリをメモリに変換する
    fnpReady = false;  //フォントデータが無効
  }



  //d = fnpGetBinaryPixel (imageCol, imageRow, characterX, characterY)
  //  バイナリから1ピクセルゲットする
  public int fnpGetBinaryPixel (int imageCol, int imageRow, int characterX, int characterY) {
    int binaryIndex = (fnpCharacterBytes * (fnpImageCols * imageRow + imageCol) +
                       fnpCharacterHorizontalBytes * characterY + (characterX >> 3));  //バイナリのインデックス
    int binaryBit = ~characterX & 7;  //バイナリとメモリのビット番号
    return (fnpBinaryArray[binaryIndex] >> binaryBit) & 1;  //メモリからゲット
  }  //fnpGetBinaryPixel

  //fnpSetBinaryPixel (imageCol, imageRow, characterX, characterY, d)
  //  バイナリに1ピクセルセットする
  public void fnpSetBinaryPixel (int imageCol, int imageRow, int characterX, int characterY, int d) {
    int binaryIndex = (fnpCharacterBytes * (fnpImageCols * imageRow + imageCol) +
                       fnpCharacterHorizontalBytes * characterY + (characterX >> 3));  //バイナリのインデックス
    int binaryBit = ~characterX & 7;  //バイナリとメモリのビット番号
    fnpBinaryArray[binaryIndex] = (byte) ((fnpBinaryArray[binaryIndex] & ~(1 << binaryBit)) | ((d & 1) << binaryBit));  //バイナリにセット
  }  //fnpSetBinaryPixel



  //string = fnpGetStatusText (imageX, imageY)
  //  ピクセルの情報を文字列で返す。範囲外のときは""を返す
  public String fnpGetStatusText (int imageX, int imageY) {
    if (!(0 <= imageX && imageX < fnpImageWidth &&
          0 <= imageY && imageY < fnpImageHeight)) {  //範囲外
      return "";
    }
    StringBuilder sb = new StringBuilder ();
    int imageCol = imageX / fnpCharacterWidth;  //イメージとバイナリとメモリの桁
    int imageRow = imageY / fnpCharacterHeight;  //イメージとバイナリの行
    int characterX = imageX - fnpCharacterWidth * imageCol;  //1文字の中のx座標
    int characterY = imageY - fnpCharacterHeight * imageRow;  //1文字の中のy座標
    int memoryRow = fnpImageRowToMemoryRow (imageRow);  //メモリの行
    if (memoryRow < 0) {  //メモリがないか対応する行がない
      sb.append ('(').append (imageCol).append (',').append (imageRow).append (')');
      sb.append (' ');
      sb.append ('(').append (characterX).append (',').append (characterY).append (')');
    } else {  //メモリがある
      int memoryTopLeftIndex = (fnpMemoryAddress +
                                fnpCharacterBytes * (fnpImageCols * memoryRow + imageCol));  //文字の左上のメモリのインデックス
      int memoryIndex = (memoryTopLeftIndex + fnpCharacterHorizontalBytes * characterY + (characterX >> 3));  //メモリのインデックス
      int binaryBit = ~characterX & 7;  //バイナリとメモリのビット番号
      XEiJ.fmtHex8 (sb.append ('(').append (imageCol).append (',').append (imageRow).append (")=$"), memoryTopLeftIndex);
      XEiJ.fmtHex8 (sb.append ("-$"), memoryTopLeftIndex + fnpCharacterBytes - 1);
      XEiJ.fmtHex8 (sb.append (" (").append (characterX).append (',').append (characterY).append (")=$"), memoryIndex);
      sb.append (':').append (binaryBit);
    }
    return sb.toString ();
  }



  //fnpMakeFontImageFileChooser ()
  //  フォント画像ファイルチューザーを作る
  public void fnpMakeFontImageFileChooser () {
    if (fnpFontImageFileChooser == null) {
      fnpFontImageFileChooser = new JFileChooser2 (new File (fnpFontImageFileName));
      fnpFontImageFileChooser.setMultiSelectionEnabled (false);  //複数選択不可
      fnpFontImageFileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
        @Override public boolean accept (File file) {
          if (file.isFile ()) {  //ファイル
            String name = file.getName ();  //ファイル名
            int flag = 0;
            int index = name.lastIndexOf (".");  //ファイルの拡張子の区切りの"."の位置
            if (0 <= index) {  //拡張子がある
              String fileSuffix = name.substring (index + 1);  //ファイルの拡張子
              for (String readerSuffix : ImageIO.getReaderFileSuffixes ()) {  //imageReaderの拡張子
                if (fileSuffix.equalsIgnoreCase (readerSuffix)) {
                  flag |= 1;  //ファイルの拡張子に対応するimageReaderがある
                  break;
                }
              }
              for (String writerSuffix : ImageIO.getWriterFileSuffixes ()) {  //imageWriterの拡張子
                if (fileSuffix.equalsIgnoreCase (writerSuffix)) {
                  flag |= 2;  //ファイルの拡張子に対応するimageWriterがある
                  break;
                }
              }
            }
            return flag == 3;  //imageReaderとimageWriterが揃っている拡張子だけ有効。書けても読めないと困るので
          }
          if (file.isDirectory ()) {  //ディレクトリ
            return true;
          }
          return false;
        }
        @Override public String getDescription () {
          return (Multilingual.mlnJapanese ?
                  "フォント画像ファイル" :
                  "Font image files");
        }
      });
    }
  }

  //fnpOpenReadFontImageFileChooser ()
  //  フォント画像ファイルチューザーを開いて選択されたフォント画像ファイルを読み込む
  public void fnpOpenReadFontImageFileChooser () {
    fnpMakeFontImageFileChooser ();
    if (fnpFontImageFileChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fnpFontImageFileChooser.getSelectedFile ();
      if (fnpReadFontImageFile (file)) {  //ロードできたら
        fnpFontImageFileName = file.getPath ();  //最後に使ったファイル
        fnpFontImageFileChooser.addHistory (file);  //ヒストリに加える
      } else {
        JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? "失敗しました" : "failed");
      }
    }
  }

  //success = fnpReadFontImageFile (file)
  //  フォント画像ファイルを読み込む
  public boolean fnpReadFontImageFile (File file) {
    BufferedImage image;
    try {
      image = ImageIO.read (file);
    } catch (Exception e) {
      return false;
    }
    return fnpInputImage (image);
  }

  //success = fnpInputImage (image)
  //  イメージを入力する
  //  イメージの幅と高さが合っていないとき失敗する
  //  全角の高さは94区だが、77区のときも1区～8区,16区～84区として受け付ける
  public boolean fnpInputImage (BufferedImage image) {
    if (!(image.getWidth () == fnpImageWidth &&
          image.getHeight () == fnpImageHeight)) {  //イメージの幅と高さが合っていない
      return false;  //失敗
    }
    //画像をビットマップに変換する
    //  r,g,bがすべて0xc0以上のとき1、さもなくば0とみなす
    int o = fnpBitmapRasterBytes;
    byte[] m = fnpBitmapArray;
    for (int y = 0; y < fnpImageHeight; y++) {
      int x = 0;
      for (; x + 3 < fnpImageWidth; x += 4) {
        m[o * y + (x >> 2)] = (byte) (((image.getRGB (x    , y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b10_00_00_00 : 0) |
                                      ((image.getRGB (x + 1, y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b00_10_00_00 : 0) |
                                      ((image.getRGB (x + 2, y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b00_00_10_00 : 0) |
                                      ((image.getRGB (x + 3, y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b00_00_00_10 : 0));  //ここでは市松模様にしない
      }
      for (; x < fnpImageWidth; x++) {
        m[o * y + (x >> 2)] = (byte) (((image.getRGB (x    , y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b10_00_00_00 : 0) |
                                      (x + 1 < fnpImageWidth && (image.getRGB (x + 1, y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b00_10_00_00 : 0) |
                                      (x + 2 < fnpImageWidth && (image.getRGB (x + 2, y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b00_00_10_00 : 0) |
                                      (x + 3 < fnpImageWidth && (image.getRGB (x + 3, y) & 0x00c0c0c0) == 0x00c0c0c0 ? 0b00_00_00_10 : 0));
      }
    }
    fnpImageToBinary ();  //イメージをバイナリに変換する
    fnpBinaryToImage ();  //バイナリをイメージに変換する。ここで市松模様になる
    fnpBinaryToMemory ();  //バイナリをメモリに変換する
    if (!fnpReady) {
      System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                          fnpNameEn + " font is ready");
      fnpReady = true;  //フォントデータが有効
    }
    return true;  //成功
  }

  //fnpOpenWriteFontImageFileChooser ()
  //  フォント画像ファイルチューザーを開いて選択されたフォント画像ファイルに書き出す
  public void fnpOpenWriteFontImageFileChooser () {
    fnpMakeFontImageFileChooser ();
    if (fnpFontImageFileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fnpFontImageFileChooser.getSelectedFile ();
      if (fnpWriteFontImageFile (file)) {  //書き出せた
        fnpFontImageFileName = file.getPath ();  //最後に使ったファイル
        fnpFontImageFileChooser.addHistory (file);  //ヒストリに加える
        fnpEditted = false;  //編集なし
      }
    }
  }

  //success = fnpWriteFontImageFile (file)
  //  フォント画像ファイルに書き出す
  public boolean fnpWriteFontImageFile (File file) {
    String path = file.getAbsolutePath ();  //ファイル名
    file = new File (path);
    String pathBak = path + ".bak";  //バックアップファイル名
    String pathTmp = path + ".tmp";  //テンポラリファイル名
    File fileBak = new File (pathBak);  //バックアップファイル
    File fileTmp = new File (pathTmp);  //テンポラリファイル
    //形式を決める
    ImageWriter imageWriter = null;
    {
      int index = path.lastIndexOf (".");
      if (0 <= index) {  //拡張子がある
        Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix (path.substring (index + 1));  //拡張子に対応するImageWriterのIterator
        if (iterator.hasNext ()) {  //拡張子に対応するImageWriterがある
          imageWriter = iterator.next ();  //拡張子に対応するImageWriter
        }
      }
    }
    if (imageWriter == null) {  //拡張子がないか、拡張子に対応するImageWriterがない
      JOptionPane.showMessageDialog (
        null, Multilingual.mlnJapanese ? path + " の形式が不明です" : "Unknown format of " + path);
      return false;
    }
    if (file.exists ()) {  //ファイルがある
      //上書きの確認
      if (JOptionPane.showConfirmDialog (
        null,
        Multilingual.mlnJapanese ? path + " に上書きします" : "Overwrite " + path + " ?",
        Multilingual.mlnJapanese ? "上書きの確認" : "Overwrite confirmation",
        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {  //上書きが許可されなかった
        return false;
      }
    }
    if (fileTmp.isFile ()) {  //テンポラリファイルがある
      //テンポラリファイルを削除する。ゴミが残っていた場合
      if (!fileTmp.delete ()) {  //テンポラリファイルを削除できない
        JOptionPane.showMessageDialog (
          null, Multilingual.mlnJapanese ? pathTmp + " を削除できません" : "Cannot delete " + pathTmp);
        return false;
      }
    }
    //テンポラリファイルに書き出す
    ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam ();
    if (imageWriteParam.canWriteCompressed ()) {
      imageWriteParam.setCompressionMode (ImageWriteParam.MODE_EXPLICIT);
      imageWriteParam.setCompressionQuality (1.0F);
    }
    try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream (fileTmp)) {
      imageWriter.setOutput (imageOutputStream);
      imageWriter.write (null, new IIOImage (fnpImageObject, null, null), imageWriteParam);
    } catch (Exception e) {
      JOptionPane.showMessageDialog (
        null, Multilingual.mlnJapanese ? pathTmp + " に書き出せません" : "Cannot write " + pathTmp);
      return false;
    }
    if (file.exists ()) {  //ファイルがある
      if (fileBak.isFile ()) {  //バックアップファイルがある
        //バックアップファイルを削除する
        //  リネームは上書きしないので明示的に削除しなければならない
        if (!fileBak.delete ()) {  //バックアップファイルを削除できない
          JOptionPane.showMessageDialog (
            null, Multilingual.mlnJapanese ? pathBak + " を削除できません" : "Cannot delete " + pathBak);
          return false;
        }
      }
      //ファイルをバックアップファイルにリネームする
      if (!file.renameTo (fileBak)) {  //ファイルをバックアップファイルにリネームできない
        JOptionPane.showMessageDialog (
          null, Multilingual.mlnJapanese ? path + " を " + pathBak + " にリネームできません" : "Cannot rename " + path + " to " + pathBak);
        return false;
      }
    }
    //テンポラリファイルをファイルにリネームする
    if (!fileTmp.renameTo (file)) {  //テンポラリファイルをファイルにリネームできない
      JOptionPane.showMessageDialog (
        null, Multilingual.mlnJapanese ? pathTmp + " を " + path + " にリネームできません" : "Cannot rename " + pathTmp + " to " + path);
      return false;
    }
    return true;
  }



/*

  //d = fnpGetMemoryPixel (imageCol, memoryRow, characterX, characterY)
  //  メモリから1ピクセルゲットする。メモリがないときは0を返す
  public int fnpGetMemoryPixel (int imageCol, int memoryRow, int characterX, int characterY) {
    if (fnpMemoryArray == null) {
      return 0;
    }
    int memoryIndex = (fnpMemoryAddress +
                       fnpCharacterBytes * (fnpImageCols * memoryRow + imageCol) +
                       fnpCharacterHorizontalBytes * characterY + (characterX >> 3));  //メモリのインデックス
    int binaryBit = ~characterX & 7;  //バイナリとメモリのビット番号
    return (fnpMemoryArray[memoryIndex] >> binaryBit) & 1;  //メモリからゲット
  }  //fnpGetMemoryPixel

  //fnpSetMemoryPixel (imageCol, memoryRow, characterX, characterY, d)
  //  メモリに1ピクセルセットする。メモリがないときは何もしない
  public void fnpSetMemoryPixel (int imageCol, int memoryRow, int characterX, int characterY, int d) {
    if (fnpMemoryArray == null) {
      return;
    }
    int memoryIndex = (fnpMemoryAddress +
                       fnpCharacterBytes * (fnpImageCols * memoryRow + imageCol) +
                       fnpCharacterHorizontalBytes * characterY + (characterX >> 3));  //メモリのインデックス
    int binaryBit = ~characterX & 7;  //バイナリとメモリのビット番号
    fnpMemoryArray[memoryIndex] = (byte) ((fnpMemoryArray[memoryIndex] & ~(1 << binaryBit)) | ((d & 1) << binaryBit));  //メモリにセット
  }  //fnpSetMemoryPixel

*/




  //class FontPage.Yon
  //  1/4角ANK。16桁×16行
  public static final class Yon extends FontPage {

    //コントロールコード
    //public static final char[] CONTROL_BASE = (
    //  //0 1 2 3 4 5 6 7 8 9 a b c d e f
    //  "  SHSXEXETEQAKBLBSHTLFVTFFCRSOSI" +  //0
    //  "DED1D2D3D4NKSNEBCNEMSBEC        "    //1
    //  ).toCharArray ();

    //全角フォントから作る文字
    //  0x82のU+00A6 BROKEN BARはJIS X 0213:2000以降で9区の0x8544に割り当てられているが、X68000のCGROMには9区が存在しない
    //  CGROM由来の漢字フォントからANKフォントを作るときはBROKEN BARを描かなければならない
    public static final char[] FULL_BASE = (
      //0 1 2 3 4 5 6 7 8 9 a b c d e f
      "　　　　　　　　　　　　　　　　" +  //0
      "　　　　　　　　　　　　→←↑↓" +  //1
      "　！”＃＄％＆’（）＊＋，－．／" +  //2
      "０１２３４５６７８９：；＜＝＞？" +  //3
      "＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ" +  //4
      "ＰＱＲＳＴＵＶＷＸＹＺ［￥］＾＿" +  //5
      "｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏ" +  //6
      "ｐｑｒｓｔｕｖｗｘｙｚ｛｜｝￣　" +  //7
      "＼～￤　　　をぁぃぅぇぉゃゅょっ" +  //8
      "　あいうえおかきくけこさしすせそ" +  //9
      "　。「」、・ヲァィゥェォャュョッ" +  //a
      "ーアイウエオカキクケコサシスセソ" +  //b
      "タチツテトナニヌネノハヒフヘホマ" +  //c
      "ミムメモヤユヨラリルレロワン゛゜" +  //d
      "たちつてとなにぬねのはひふへほま" +  //e
      "みむめもやゆよらりるれろわん　　"    //f
      ).toCharArray ();

    public Yon (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName) {
      this (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName, null, 0);
    }
    public Yon (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName,
                byte[] memoryArray, int memoryAddress) {
      super (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName,
             16, 16, 16,
             memoryArray, memoryAddress,
             new char[][] { FULL_BASE });
    }

  }  //class FontPage.Yon



  //class FontPage.Han
  //  半角ANK。16桁×16行
  public static final class Han extends FontPage {

    //半角フォントから作る文字
    //  0x5cは￥マークなので\u00a5
    public static final char[] HALF_BASE = (
      //123456789abcdef
      "                " +  //0
      "                " +  //1
      " !\"#$%&'()*+,-./" +  //2
      "0123456789:;<=>?" +  //3
      "@ABCDEFGHIJKLMNO" +  //4
      "PQRSTUVWXYZ[\u00a5]^_" +  //5
      "`abcdefghijklmno" +  //6
      "pqrstuvwxyz{|}  " +  //7
      " \u007e\u00a6             " +  //8
      "                " +  //9
      " ｡｢｣､･ｦｧｨｩｪｫｬｭｮｯ" +  //a
      "ｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ" +  //b
      "ﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ" +  //c
      "ﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ" +  //d
      "                " +  //e
      "                "    //f
      ).toCharArray ();

    //全角フォントから作る文字
    //  全角ひらがなを潰して半角ひらがなを作る
    //  矢印は半角フォントが潰れてしまうことがあるので全角フォントを使う
    //  OVERLINEは半角フォントが極端に短いことがあるので全角フォントを使う
    //  REVERSE SOLIDUSは日本語環境だと半角フォントが￥マークになってしまうので全角フォントを使う
    //    でもあまり綺麗に表示できないので斜線を描いたほうが良いかも
    public static final char[] FULL_BASE = (
      //0 1 2 3 4 5 6 7 8 9 a b c d e f
      "　　　　　　　　　　　　　　　　" +  //0
      "　　　　　　　　　　　　→←↑↓" +  //1
      "　　　　　　　　　　　　　　　　" +  //2
      "　　　　　　　　　　　　　　　　" +  //3
      "　　　　　　　　　　　　　　　　" +  //4
      "　　　　　　　　　　　　　　　　" +  //5
      "　　　　　　　　　　　　　　　　" +  //6
      "　　　　　　　　　　　　　　￣　" +  //7
      "＼　　　　　をぁぃぅぇぉゃゅょっ" +  //8
      "　あいうえおかきくけこさしすせそ" +  //9
      "　　　　　　　　　　　　　　　　" +  //a
      "　　　　　　　　　　　　　　　　" +  //b
      "　　　　　　　　　　　　　　　　" +  //c
      "　　　　　　　　　　　　　　　　" +  //d
      "たちつてとなにぬねのはひふへほま" +  //e
      "みむめもやゆよらりるれろわん　　"    //f
      ).toCharArray ();

    public FontPage.Zen fnpZenPage;  //対応する全角フォントのページ

    public Han (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName) {
      this (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName, null, 0);
    }
    public Han (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName,
                 byte[] memoryArray, int memoryAddress) {
      super (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName,
             16, 16, 16,
             memoryArray, memoryAddress,
             new char[][] { HALF_BASE, FULL_BASE });
    }

    //setZenPage (zenPage)
    //  対応する全角フォントのページを設定する
    public void setZenPage (FontPage.Zen zenPage) {
      fnpZenPage = zenPage;
      fnpMaximumFontDataFileLength = fnpBinaryBytes + zenPage.fnpBinaryBytes;  //半角+全角
    }

    //yes = fnpIsFontDataFileLength (longLength)
    //  フォントデータファイルの長さか
    //  半角と全角は半角+全角を受け付ける
    @Override public boolean fnpIsFontDataFileLength (long longLength) {
      return (longLength == (long) fnpBinaryBytes ||  //半角のみ
              longLength == (long) (fnpBinaryBytes + fnpZenPage.fnpBinaryBytes));  //半角+全角
    }

    //success = fnpLoadFontDataArray (array, start, length)
    //  フォントデータファイルを配列から読み込む
    //  半角と全角は半角+全角を受け付ける
    @Override public boolean fnpLoadFontDataArray (byte[] array, int start, int length) {
      if (length == fnpBinaryBytes ||  //半角のみ
          length == fnpBinaryBytes + fnpZenPage.fnpBinaryBytes) {  //半角+全角
        System.arraycopy (array, start, fnpBinaryArray, 0, fnpBinaryBytes);  //半角をコピーする
        fnpBinaryToMemory ();
        fnpBinaryToImage ();
        if (!fnpReady) {
          System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                              fnpNameEn + " font is ready");
          fnpReady = true;  //フォントデータが有効
        }
        return true;
      }
      return false;
    }

    //fnpGetExtension ()
    //  フォントデータファイルの拡張子を返す
    //  全角と半角は.fonまたは.f<1文字の高さ>、それ以外は.dat
    @Override public String fnpGetExtension () {
      if (fnpExtension == null) {
        fnpExtension = fnpCharacterHeight == 16 ? ".fon" : ".f" + fnpCharacterHeight;
      }
      return fnpExtension;
    }

  }  //class FontPage.Han



  //class FontPage.Zen
  //  全角漢字。イメージとバイナリは94点×94区。メモリは94点×77区(1区～8区,16区～84区)
  public static final class Zen extends FontPage {

    //全角フォントから作る文字
    public static final char[] FULL_BASE = (
      "　\0、\0。\0，\0．\0・\0：\0；\0？\0！\0゛\0゜\0´\0｀\0¨\0＾\0￣\0＿\0ヽ\0ヾ\0ゝ\0ゞ\0〃\0仝\0々\0〆\0〇\0ー\0―\0－\0／\0＼\0～\0‖\0｜\0…\0‥\0‘\0’\0“\0”\0（\0）\0〔\0〕\0［\0］\0｛\0｝\0〈\0〉\0《\0》\0「\0」\0『\0』\0【\0】\0＋\0－\0±\0×\0÷\0＝\0≠\0＜\0＞\0≦\0≧\0∞\0∴\0♂\0♀\0°\0′\0″\0℃\0￥\0＄\0￠\0￡\0％\0＃\0＆\0＊\0＠\0§\0☆\0★\0○\0●\0◎\0◇\0" +  //1区
      "◆\0□\0■\0△\0▲\0▽\0▼\0※\0〒\0→\0←\0↑\0↓\0〓\0\uff07\0\uff02\0\uff0d\0\uff5e\0\u3033\0\u3034\0\u3035\0\u303b\0\u303c\0\u30ff\0\u309f\0∈\0∋\0⊆\0⊇\0⊂\0⊃\0∪\0∩\0\u2284\0\u2285\0\u228a\0\u228b\0\u2209\0\u2205\0\u2305\0\u2306\0∧\0∨\0￢\0⇒\0⇔\0∀\0∃\0\u2295\0\u2296\0\u2297\0\u2225\0\u2226\0\uff5f\0\uff60\0\u3018\0\u3019\0\u3016\0\u3017\0∠\0⊥\0⌒\0∂\0∇\0≡\0≒\0≪\0≫\0√\0∽\0∝\0∵\0∫\0∬\0\u2262\0\u2243\0\u2245\0\u2248\0\u2276\0\u2277\0\u2194\0Å\0‰\0♯\0♭\0♪\0†\0‡\0¶\0\u266e\0\u266b\0\u266c\0\u2669\0◯\0" +  //2区
      "\u25b7\0\u25b6\0\u25c1\0\u25c0\0\u2197\0\u2198\0\u2196\0\u2199\0\u21c4\0\u21e8\0\u21e6\0\u21e7\0\u21e9\0\u2934\0\u2935\0０\0１\0２\0３\0４\0５\0６\0７\0８\0９\0\u29bf\0\u25c9\0\u303d\0\ufe46\0\ufe45\0\u25e6\0\u2022\0Ａ\0Ｂ\0Ｃ\0Ｄ\0Ｅ\0Ｆ\0Ｇ\0Ｈ\0Ｉ\0Ｊ\0Ｋ\0Ｌ\0Ｍ\0Ｎ\0Ｏ\0Ｐ\0Ｑ\0Ｒ\0Ｓ\0Ｔ\0Ｕ\0Ｖ\0Ｗ\0Ｘ\0Ｙ\0Ｚ\0\u2213\0\u2135\0\u210f\0\u33cb\0\u2113\0\u2127\0ａ\0ｂ\0ｃ\0ｄ\0ｅ\0ｆ\0ｇ\0ｈ\0ｉ\0ｊ\0ｋ\0ｌ\0ｍ\0ｎ\0ｏ\0ｐ\0ｑ\0ｒ\0ｓ\0ｔ\0ｕ\0ｖ\0ｗ\0ｘ\0ｙ\0ｚ\0\u30a0\0\u2013\0\u29fa\0\u29fb\0" +  //3区
      "ぁ\0あ\0ぃ\0い\0ぅ\0う\0ぇ\0え\0ぉ\0お\0か\0が\0き\0ぎ\0く\0ぐ\0け\0げ\0こ\0ご\0さ\0ざ\0し\0じ\0す\0ず\0せ\0ぜ\0そ\0ぞ\0た\0だ\0ち\0ぢ\0っ\0つ\0づ\0て\0で\0と\0ど\0な\0に\0ぬ\0ね\0の\0は\0ば\0ぱ\0ひ\0び\0ぴ\0ふ\0ぶ\0ぷ\0へ\0べ\0ぺ\0ほ\0ぼ\0ぽ\0ま\0み\0む\0め\0も\0ゃ\0や\0ゅ\0ゆ\0ょ\0よ\0ら\0り\0る\0れ\0ろ\0ゎ\0わ\0ゐ\0ゑ\0を\0ん\0\u3094\0\u3095\0\u3096\0\u304b\u309a\u304d\u309a\u304f\u309a\u3051\u309a\u3053\u309a\u3000\0\u3000\0\u3000\0" +  //4区
      "ァ\0ア\0ィ\0イ\0ゥ\0ウ\0ェ\0エ\0ォ\0オ\0カ\0ガ\0キ\0ギ\0ク\0グ\0ケ\0ゲ\0コ\0ゴ\0サ\0ザ\0シ\0ジ\0ス\0ズ\0セ\0ゼ\0ソ\0ゾ\0タ\0ダ\0チ\0ヂ\0ッ\0ツ\0ヅ\0テ\0デ\0ト\0ド\0ナ\0ニ\0ヌ\0ネ\0ノ\0ハ\0バ\0パ\0ヒ\0ビ\0ピ\0フ\0ブ\0プ\0ヘ\0ベ\0ペ\0ホ\0ボ\0ポ\0マ\0ミ\0ム\0メ\0モ\0ャ\0ヤ\0ュ\0ユ\0ョ\0ヨ\0ラ\0リ\0ル\0レ\0ロ\0ヮ\0ワ\0ヰ\0ヱ\0ヲ\0ン\0ヴ\0ヵ\0ヶ\0\u30ab\u309a\u30ad\u309a\u30af\u309a\u30b1\u309a\u30b3\u309a\u30bb\u309a\u30c4\u309a\u30c8\u309a" +  //5区
      "Α\0Β\0Γ\0Δ\0Ε\0Ζ\0Η\0Θ\0Ι\0Κ\0Λ\0Μ\0Ν\0Ξ\0Ο\0Π\0Ρ\0Σ\0Τ\0Υ\0Φ\0Χ\0Ψ\0Ω\0\u2664\0\u2660\0\u2662\0\u2666\0\u2661\0\u2665\0\u2667\0\u2663\0α\0β\0γ\0δ\0ε\0ζ\0η\0θ\0ι\0κ\0λ\0μ\0ν\0ξ\0ο\0π\0ρ\0σ\0τ\0υ\0φ\0χ\0ψ\0ω\0\u03c2\0\u24f5\0\u24f6\0\u24f7\0\u24f8\0\u24f9\0\u24fa\0\u24fb\0\u24fc\0\u24fd\0\u24fe\0\u2616\0\u2617\0\u3020\0\u260e\0\u2600\0\u2601\0\u2602\0\u2603\0\u2668\0\u25b1\0\u31f0\0\u31f1\0\u31f2\0\u31f3\0\u31f4\0\u31f5\0\u31f6\0\u31f7\0\u31f8\0\u31f9\0\u31f7\u309a\u31fa\0\u31fb\0\u31fc\0\u31fd\0\u31fe\0\u31ff\0" +  //6区
      "А\0Б\0В\0Г\0Д\0Е\0Ё\0Ж\0З\0И\0Й\0К\0Л\0М\0Н\0О\0П\0Р\0С\0Т\0У\0Ф\0Х\0Ц\0Ч\0Ш\0Щ\0Ъ\0Ы\0Ь\0Э\0Ю\0Я\0\u23be\0\u23bf\0\u23c0\0\u23c1\0\u23c2\0\u23c3\0\u23c4\0\u23c5\0\u23c6\0\u23c7\0\u23c8\0\u23c9\0\u23ca\0\u23cb\0\u23cc\0а\0б\0в\0г\0д\0е\0ё\0ж\0з\0и\0й\0к\0л\0м\0н\0о\0п\0р\0с\0т\0у\0ф\0х\0ц\0ч\0ш\0щ\0ъ\0ы\0ь\0э\0ю\0я\0\u30f7\0\u30f8\0\u30f9\0\u30fa\0\u22da\0\u22db\0\u2153\0\u2154\0\u2155\0\u2713\0\u2318\0\u2423\0\u23ce\0" +  //7区
      "─\0│\0┌\0┐\0┘\0└\0├\0┬\0┤\0┴\0┼\0━\0┃\0┏\0┓\0┛\0┗\0┣\0┳\0┫\0┻\0╋\0┠\0┯\0┨\0┷\0┿\0┝\0┰\0┥\0┸\0╂\0\u3251\0\u3252\0\u3253\0\u3254\0\u3255\0\u3256\0\u3257\0\u3258\0\u3259\0\u325a\0\u325b\0\u325c\0\u325d\0\u325e\0\u325f\0\u32b1\0\u32b2\0\u32b3\0\u32b4\0\u32b5\0\u32b6\0\u32b7\0\u32b8\0\u32b9\0\u32ba\0\u32bb\0\u32bc\0\u32bd\0\u32be\0\u32bf\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u25d0\0\u25d1\0\u25d2\0\u25d3\0\u203c\0\u2047\0\u2048\0\u2049\0\u01cd\0\u01ce\0\u01d0\0\u1e3e\0\u1e3f\0\u01f8\0\u01f9\0\u01d1\0\u01d2\0\u01d4\0\u01d6\0\u01d8\0\u01da\0\u01dc\0\u3000\0\u3000\0" +  //8区
      "\u20ac\0\u00a0\0\u00a1\0\u00a4\0\u00a6\0\u00a9\0\u00aa\0\u00ab\0\u00ad\0\u00ae\0\u00af\0\u00b2\0\u00b3\0\u00b7\0\u00b8\0\u00b9\0\u00ba\0\u00bb\0\u00bc\0\u00bd\0\u00be\0\u00bf\0\u00c0\0\u00c1\0\u00c2\0\u00c3\0\u00c4\0\u00c5\0\u00c6\0\u00c7\0\u00c8\0\u00c9\0\u00ca\0\u00cb\0\u00cc\0\u00cd\0\u00ce\0\u00cf\0\u00d0\0\u00d1\0\u00d2\0\u00d3\0\u00d4\0\u00d5\0\u00d6\0\u00d8\0\u00d9\0\u00da\0\u00db\0\u00dc\0\u00dd\0\u00de\0\u00df\0\u00e0\0\u00e1\0\u00e2\0\u00e3\0\u00e4\0\u00e5\0\u00e6\0\u00e7\0\u00e8\0\u00e9\0\u00ea\0\u00eb\0\u00ec\0\u00ed\0\u00ee\0\u00ef\0\u00f0\0\u00f1\0\u00f2\0\u00f3\0\u00f4\0\u00f5\0\u00f6\0\u00f8\0\u00f9\0\u00fa\0\u00fb\0\u00fc\0\u00fd\0\u00fe\0\u00ff\0\u0100\0\u012a\0\u016a\0\u0112\0\u014c\0\u0101\0\u012b\0\u016b\0\u0113\0\u014d\0" +  //9区
      "\u0104\0\u02d8\0\u0141\0\u013d\0\u015a\0\u0160\0\u015e\0\u0164\0\u0179\0\u017d\0\u017b\0\u0105\0\u02db\0\u0142\0\u013e\0\u015b\0\u02c7\0\u0161\0\u015f\0\u0165\0\u017a\0\u02dd\0\u017e\0\u017c\0\u0154\0\u0102\0\u0139\0\u0106\0\u010c\0\u0118\0\u011a\0\u010e\0\u0143\0\u0147\0\u0150\0\u0158\0\u016e\0\u0170\0\u0162\0\u0155\0\u0103\0\u013a\0\u0107\0\u010d\0\u0119\0\u011b\0\u010f\0\u0111\0\u0144\0\u0148\0\u0151\0\u0159\0\u016f\0\u0171\0\u0163\0\u02d9\0\u0108\0\u011c\0\u0124\0\u0134\0\u015c\0\u016c\0\u0109\0\u011d\0\u0125\0\u0135\0\u015d\0\u016d\0\u0271\0\u028b\0\u027e\0\u0283\0\u0292\0\u026c\0\u026e\0\u0279\0\u0288\0\u0256\0\u0273\0\u027d\0\u0282\0\u0290\0\u027b\0\u026d\0\u025f\0\u0272\0\u029d\0\u028e\0\u0261\0\u014b\0\u0270\0\u0281\0\u0127\0\u0295\0" +  //10区
      "\u0294\0\u0266\0\u0298\0\u01c2\0\u0253\0\u0257\0\u0284\0\u0260\0\u0193\0\u0153\0\u0152\0\u0268\0\u0289\0\u0258\0\u0275\0\u0259\0\u025c\0\u025e\0\u0250\0\u026f\0\u028a\0\u0264\0\u028c\0\u0254\0\u0251\0\u0252\0\u028d\0\u0265\0\u02a2\0\u02a1\0\u0255\0\u0291\0\u027a\0\u0267\0\u025a\0\u00e6\u0300\u01fd\0\u1f70\0\u1f71\0\u0254\u0300\u0254\u0301\u028c\u0300\u028c\u0301\u0259\u0300\u0259\u0301\u025a\u0300\u025a\u0301\u1f72\0\u1f73\0\u0361\0\u02c8\0\u02cc\0\u02d0\0\u02d1\0\u0306\0\u203f\0\u030b\0\u0301\0\u0304\0\u0300\0\u030f\0\u030c\0\u0302\0\u02e5\0\u02e6\0\u02e7\0\u02e8\0\u02e9\0\u02e9\u02e5\u02e5\u02e9\u0325\0\u032c\0\u0339\0\u031c\0\u031f\0\u0320\0\u0308\0\u033d\0\u0329\0\u032f\0\u02de\0\u0324\0\u0330\0\u033c\0\u0334\0\u031d\0\u031e\0\u0318\0\u0319\0\u032a\0\u033a\0\u033b\0\u0303\0\u031a\0" +  //11区
      "\u2776\0\u2777\0\u2778\0\u2779\0\u277a\0\u277b\0\u277c\0\u277d\0\u277e\0\u277f\0\u24eb\0\u24ec\0\u24ed\0\u24ee\0\u24ef\0\u24f0\0\u24f1\0\u24f2\0\u24f3\0\u24f4\0\u2170\0\u2171\0\u2172\0\u2173\0\u2174\0\u2175\0\u2176\0\u2177\0\u2178\0\u2179\0\u217a\0\u217b\0\u24d0\0\u24d1\0\u24d2\0\u24d3\0\u24d4\0\u24d5\0\u24d6\0\u24d7\0\u24d8\0\u24d9\0\u24da\0\u24db\0\u24dc\0\u24dd\0\u24de\0\u24df\0\u24e0\0\u24e1\0\u24e2\0\u24e3\0\u24e4\0\u24e5\0\u24e6\0\u24e7\0\u24e8\0\u24e9\0\u32d0\0\u32d1\0\u32d2\0\u32d3\0\u32d4\0\u32d5\0\u32d6\0\u32d7\0\u32d8\0\u32d9\0\u32da\0\u32db\0\u32dc\0\u32dd\0\u32de\0\u32df\0\u32e0\0\u32e1\0\u32e2\0\u32e3\0\u32fa\0\u32e9\0\u32e5\0\u32ed\0\u32ec\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u2051\0\u2042\0" +  //12区
      "\u2460\0\u2461\0\u2462\0\u2463\0\u2464\0\u2465\0\u2466\0\u2467\0\u2468\0\u2469\0\u246a\0\u246b\0\u246c\0\u246d\0\u246e\0\u246f\0\u2470\0\u2471\0\u2472\0\u2473\0\u2160\0\u2161\0\u2162\0\u2163\0\u2164\0\u2165\0\u2166\0\u2167\0\u2168\0\u2169\0\u216a\0\u3349\0\u3314\0\u3322\0\u334d\0\u3318\0\u3327\0\u3303\0\u3336\0\u3351\0\u3357\0\u330d\0\u3326\0\u3323\0\u332b\0\u334a\0\u333b\0\u339c\0\u339d\0\u339e\0\u338e\0\u338f\0\u33c4\0\u33a1\0\u216b\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u3000\0\u337b\0\u301d\0\u301f\0\u2116\0\u33cd\0\u2121\0\u32a4\0\u32a5\0\u32a6\0\u32a7\0\u32a8\0\u3231\0\u3232\0\u3239\0\u337e\0\u337d\0\u337c\0\u3000\0\u3000\0\u3000\0\u222e\0\u3000\0\u3000\0\u3000\0\u3000\0\u221f\0\u22bf\0\u3000\0\u3000\0\u3000\0\u2756\0\u261e\0" +  //13区
      "\u4ff1\0\u0076\u000b\u3402\0\u4e28\0\u4e2f\0\u4e30\0\u4e8d\0\u4ee1\0\u4efd\0\u4eff\0\u4f03\0\u4f0b\0\u4f60\0\u4f48\0\u4f49\0\u4f56\0\u4f5f\0\u4f6a\0\u4f6c\0\u4f7e\0\u4f8a\0\u4f94\0\u4f97\0\ufa30\0\u4fc9\0\u4fe0\0\u5001\0\u5002\0\u500e\0\u5018\0\u5027\0\u502e\0\u5040\0\u503b\0\u5041\0\u5094\0\u50cc\0\u50f2\0\u50d0\0\u50e6\0\ufa31\0\u5106\0\u5103\0\u510b\0\u511e\0\u5135\0\u514a\0\ufa32\0\u5155\0\u5157\0\u34b5\0\u519d\0\u51c3\0\u51ca\0\u51de\0\u51e2\0\u51ee\0\u5201\0\u34db\0\u5213\0\u5215\0\u5249\0\u5257\0\u5261\0\u5293\0\u52c8\0\ufa33\0\u52cc\0\u52d0\0\u52d6\0\u52db\0\ufa34\0\u52f0\0\u52fb\0\u5300\0\u5307\0\u531c\0\ufa35\0\u5361\0\u5363\0\u537d\0\u5393\0\u539d\0\u53b2\0\u5412\0\u5427\0\u544d\0\u549c\0\u546b\0\u5474\0\u547f\0\u5488\0\u5496\0\u54a1\0" +  //14区
      "\u54a9\0\u54c6\0\u54ff\0\u550e\0\u552b\0\u5535\0\u5550\0\u555e\0\u5581\0\u5586\0\u558e\0\ufa36\0\u55ad\0\u55ce\0\ufa37\0\u5608\0\u560e\0\u563b\0\u5649\0\u5676\0\u5666\0\ufa38\0\u566f\0\u5671\0\u5672\0\u5699\0\u569e\0\u56a9\0\u56ac\0\u56b3\0\u56c9\0\u56ca\0\u570a\0\u007a\u023d\u5721\0\u572f\0\u5733\0\u5734\0\u5770\0\u5777\0\u577c\0\u579c\0\ufa0f\0\u007a\u031b\u57b8\0\u57c7\0\u57c8\0\u57cf\0\u57e4\0\u57ed\0\u57f5\0\u57f6\0\u57ff\0\u5809\0\ufa10\0\u5861\0\u5864\0\ufa39\0\u587c\0\u5889\0\u589e\0\ufa3a\0\u58a9\0\u007b\u006e\u58d2\0\u58ce\0\u58d4\0\u58da\0\u58e0\0\u58e9\0\u590c\0\u8641\0\u595d\0\u596d\0\u598b\0\u5992\0\u59a4\0\u59c3\0\u59d2\0\u59dd\0\u5a13\0\u5a23\0\u5a67\0\u5a6d\0\u5a77\0\u5a7e\0\u5a84\0\u5a9e\0\u5aa7\0\u5ac4\0\u007c\u00bd\u5b19\0\u5b25\0\u525d\0" +  //15区
      "亜\0唖\0娃\0阿\0哀\0愛\0挨\0姶\0逢\0葵\0茜\0穐\0悪\0握\0渥\0旭\0葦\0芦\0鯵\0梓\0圧\0斡\0扱\0宛\0姐\0虻\0飴\0絢\0綾\0鮎\0或\0粟\0袷\0安\0庵\0按\0暗\0案\0闇\0鞍\0杏\0以\0伊\0位\0依\0偉\0囲\0夷\0委\0威\0尉\0惟\0意\0慰\0易\0椅\0為\0畏\0異\0移\0維\0緯\0胃\0萎\0衣\0謂\0違\0遺\0医\0井\0亥\0域\0育\0郁\0磯\0一\0壱\0溢\0逸\0稲\0茨\0芋\0鰯\0允\0印\0咽\0員\0因\0姻\0引\0飲\0淫\0胤\0蔭\0" +  //16区
      "院\0陰\0隠\0韻\0吋\0右\0宇\0烏\0羽\0迂\0雨\0卯\0鵜\0窺\0丑\0碓\0臼\0渦\0嘘\0唄\0欝\0蔚\0鰻\0姥\0厩\0浦\0瓜\0閏\0噂\0云\0運\0雲\0荏\0餌\0叡\0営\0嬰\0影\0映\0曳\0栄\0永\0泳\0洩\0瑛\0盈\0穎\0頴\0英\0衛\0詠\0鋭\0液\0疫\0益\0駅\0悦\0謁\0越\0閲\0榎\0厭\0円\0園\0堰\0奄\0宴\0延\0怨\0掩\0援\0沿\0演\0炎\0焔\0煙\0燕\0猿\0縁\0艶\0苑\0薗\0遠\0鉛\0鴛\0塩\0於\0汚\0甥\0凹\0央\0奥\0往\0応\0" +  //17区
      "押\0旺\0横\0欧\0殴\0王\0翁\0襖\0鴬\0鴎\0黄\0岡\0沖\0荻\0億\0屋\0憶\0臆\0桶\0牡\0乙\0俺\0卸\0恩\0温\0穏\0音\0下\0化\0仮\0何\0伽\0価\0佳\0加\0可\0嘉\0夏\0嫁\0家\0寡\0科\0暇\0果\0架\0歌\0河\0火\0珂\0禍\0禾\0稼\0箇\0花\0苛\0茄\0荷\0華\0菓\0蝦\0課\0嘩\0貨\0迦\0過\0霞\0蚊\0俄\0峨\0我\0牙\0画\0臥\0芽\0蛾\0賀\0雅\0餓\0駕\0介\0会\0解\0回\0塊\0壊\0廻\0快\0怪\0悔\0恢\0懐\0戒\0拐\0改\0" +  //18区
      "魁\0晦\0械\0海\0灰\0界\0皆\0絵\0芥\0蟹\0開\0階\0貝\0凱\0劾\0外\0咳\0害\0崖\0慨\0概\0涯\0碍\0蓋\0街\0該\0鎧\0骸\0浬\0馨\0蛙\0垣\0柿\0蛎\0鈎\0劃\0嚇\0各\0廓\0拡\0撹\0格\0核\0殻\0獲\0確\0穫\0覚\0角\0赫\0較\0郭\0閣\0隔\0革\0学\0岳\0楽\0額\0顎\0掛\0笠\0樫\0橿\0梶\0鰍\0潟\0割\0喝\0恰\0括\0活\0渇\0滑\0葛\0褐\0轄\0且\0鰹\0叶\0椛\0樺\0鞄\0株\0兜\0竃\0蒲\0釜\0鎌\0噛\0鴨\0栢\0茅\0萱\0" +  //19区
      "粥\0刈\0苅\0瓦\0乾\0侃\0冠\0寒\0刊\0勘\0勧\0巻\0喚\0堪\0姦\0完\0官\0寛\0干\0幹\0患\0感\0慣\0憾\0換\0敢\0柑\0桓\0棺\0款\0歓\0汗\0漢\0澗\0潅\0環\0甘\0監\0看\0竿\0管\0簡\0緩\0缶\0翰\0肝\0艦\0莞\0観\0諌\0貫\0還\0鑑\0間\0閑\0関\0陥\0韓\0館\0舘\0丸\0含\0岸\0巌\0玩\0癌\0眼\0岩\0翫\0贋\0雁\0頑\0顔\0願\0企\0伎\0危\0喜\0器\0基\0奇\0嬉\0寄\0岐\0希\0幾\0忌\0揮\0机\0旗\0既\0期\0棋\0棄\0" +  //20区
      "機\0帰\0毅\0気\0汽\0畿\0祈\0季\0稀\0紀\0徽\0規\0記\0貴\0起\0軌\0輝\0飢\0騎\0鬼\0亀\0偽\0儀\0妓\0宜\0戯\0技\0擬\0欺\0犠\0疑\0祇\0義\0蟻\0誼\0議\0掬\0菊\0鞠\0吉\0吃\0喫\0桔\0橘\0詰\0砧\0杵\0黍\0却\0客\0脚\0虐\0逆\0丘\0久\0仇\0休\0及\0吸\0宮\0弓\0急\0救\0朽\0求\0汲\0泣\0灸\0球\0究\0窮\0笈\0級\0糾\0給\0旧\0牛\0去\0居\0巨\0拒\0拠\0挙\0渠\0虚\0許\0距\0鋸\0漁\0禦\0魚\0亨\0享\0京\0" +  //21区
      "供\0侠\0僑\0兇\0競\0共\0凶\0協\0匡\0卿\0叫\0喬\0境\0峡\0強\0彊\0怯\0恐\0恭\0挟\0教\0橋\0況\0狂\0狭\0矯\0胸\0脅\0興\0蕎\0郷\0鏡\0響\0饗\0驚\0仰\0凝\0尭\0暁\0業\0局\0曲\0極\0玉\0桐\0粁\0僅\0勤\0均\0巾\0錦\0斤\0欣\0欽\0琴\0禁\0禽\0筋\0緊\0芹\0菌\0衿\0襟\0謹\0近\0金\0吟\0銀\0九\0倶\0句\0区\0狗\0玖\0矩\0苦\0躯\0駆\0駈\0駒\0具\0愚\0虞\0喰\0空\0偶\0寓\0遇\0隅\0串\0櫛\0釧\0屑\0屈\0" +  //22区
      "掘\0窟\0沓\0靴\0轡\0窪\0熊\0隈\0粂\0栗\0繰\0桑\0鍬\0勲\0君\0薫\0訓\0群\0軍\0郡\0卦\0袈\0祁\0係\0傾\0刑\0兄\0啓\0圭\0珪\0型\0契\0形\0径\0恵\0慶\0慧\0憩\0掲\0携\0敬\0景\0桂\0渓\0畦\0稽\0系\0経\0継\0繋\0罫\0茎\0荊\0蛍\0計\0詣\0警\0軽\0頚\0鶏\0芸\0迎\0鯨\0劇\0戟\0撃\0激\0隙\0桁\0傑\0欠\0決\0潔\0穴\0結\0血\0訣\0月\0件\0倹\0倦\0健\0兼\0券\0剣\0喧\0圏\0堅\0嫌\0建\0憲\0懸\0拳\0捲\0" +  //23区
      "検\0権\0牽\0犬\0献\0研\0硯\0絹\0県\0肩\0見\0謙\0賢\0軒\0遣\0鍵\0険\0顕\0験\0鹸\0元\0原\0厳\0幻\0弦\0減\0源\0玄\0現\0絃\0舷\0言\0諺\0限\0乎\0個\0古\0呼\0固\0姑\0孤\0己\0庫\0弧\0戸\0故\0枯\0湖\0狐\0糊\0袴\0股\0胡\0菰\0虎\0誇\0跨\0鈷\0雇\0顧\0鼓\0五\0互\0伍\0午\0呉\0吾\0娯\0後\0御\0悟\0梧\0檎\0瑚\0碁\0語\0誤\0護\0醐\0乞\0鯉\0交\0佼\0侯\0候\0倖\0光\0公\0功\0効\0勾\0厚\0口\0向\0" +  //24区
      "后\0喉\0坑\0垢\0好\0孔\0孝\0宏\0工\0巧\0巷\0幸\0広\0庚\0康\0弘\0恒\0慌\0抗\0拘\0控\0攻\0昂\0晃\0更\0杭\0校\0梗\0構\0江\0洪\0浩\0港\0溝\0甲\0皇\0硬\0稿\0糠\0紅\0紘\0絞\0綱\0耕\0考\0肯\0肱\0腔\0膏\0航\0荒\0行\0衡\0講\0貢\0購\0郊\0酵\0鉱\0砿\0鋼\0閤\0降\0項\0香\0高\0鴻\0剛\0劫\0号\0合\0壕\0拷\0濠\0豪\0轟\0麹\0克\0刻\0告\0国\0穀\0酷\0鵠\0黒\0獄\0漉\0腰\0甑\0忽\0惚\0骨\0狛\0込\0" +  //25区
      "此\0頃\0今\0困\0坤\0墾\0婚\0恨\0懇\0昏\0昆\0根\0梱\0混\0痕\0紺\0艮\0魂\0些\0佐\0叉\0唆\0嵯\0左\0差\0査\0沙\0瑳\0砂\0詐\0鎖\0裟\0坐\0座\0挫\0債\0催\0再\0最\0哉\0塞\0妻\0宰\0彩\0才\0採\0栽\0歳\0済\0災\0采\0犀\0砕\0砦\0祭\0斎\0細\0菜\0裁\0載\0際\0剤\0在\0材\0罪\0財\0冴\0坂\0阪\0堺\0榊\0肴\0咲\0崎\0埼\0碕\0鷺\0作\0削\0咋\0搾\0昨\0朔\0柵\0窄\0策\0索\0錯\0桜\0鮭\0笹\0匙\0冊\0刷\0" +  //26区
      "察\0拶\0撮\0擦\0札\0殺\0薩\0雑\0皐\0鯖\0捌\0錆\0鮫\0皿\0晒\0三\0傘\0参\0山\0惨\0撒\0散\0桟\0燦\0珊\0産\0算\0纂\0蚕\0讃\0賛\0酸\0餐\0斬\0暫\0残\0仕\0仔\0伺\0使\0刺\0司\0史\0嗣\0四\0士\0始\0姉\0姿\0子\0屍\0市\0師\0志\0思\0指\0支\0孜\0斯\0施\0旨\0枝\0止\0死\0氏\0獅\0祉\0私\0糸\0紙\0紫\0肢\0脂\0至\0視\0詞\0詩\0試\0誌\0諮\0資\0賜\0雌\0飼\0歯\0事\0似\0侍\0児\0字\0寺\0慈\0持\0時\0" +  //27区
      "次\0滋\0治\0爾\0璽\0痔\0磁\0示\0而\0耳\0自\0蒔\0辞\0汐\0鹿\0式\0識\0鴫\0竺\0軸\0宍\0雫\0七\0叱\0執\0失\0嫉\0室\0悉\0湿\0漆\0疾\0質\0実\0蔀\0篠\0偲\0柴\0芝\0屡\0蕊\0縞\0舎\0写\0射\0捨\0赦\0斜\0煮\0社\0紗\0者\0謝\0車\0遮\0蛇\0邪\0借\0勺\0尺\0杓\0灼\0爵\0酌\0釈\0錫\0若\0寂\0弱\0惹\0主\0取\0守\0手\0朱\0殊\0狩\0珠\0種\0腫\0趣\0酒\0首\0儒\0受\0呪\0寿\0授\0樹\0綬\0需\0囚\0収\0周\0" +  //28区
      "宗\0就\0州\0修\0愁\0拾\0洲\0秀\0秋\0終\0繍\0習\0臭\0舟\0蒐\0衆\0襲\0讐\0蹴\0輯\0週\0酋\0酬\0集\0醜\0什\0住\0充\0十\0従\0戎\0柔\0汁\0渋\0獣\0縦\0重\0銃\0叔\0夙\0宿\0淑\0祝\0縮\0粛\0塾\0熟\0出\0術\0述\0俊\0峻\0春\0瞬\0竣\0舜\0駿\0准\0循\0旬\0楯\0殉\0淳\0準\0潤\0盾\0純\0巡\0遵\0醇\0順\0処\0初\0所\0暑\0曙\0渚\0庶\0緒\0署\0書\0薯\0藷\0諸\0助\0叙\0女\0序\0徐\0恕\0鋤\0除\0傷\0償\0" +  //29区
      "勝\0匠\0升\0召\0哨\0商\0唱\0嘗\0奨\0妾\0娼\0宵\0将\0小\0少\0尚\0庄\0床\0廠\0彰\0承\0抄\0招\0掌\0捷\0昇\0昌\0昭\0晶\0松\0梢\0樟\0樵\0沼\0消\0渉\0湘\0焼\0焦\0照\0症\0省\0硝\0礁\0祥\0称\0章\0笑\0粧\0紹\0肖\0菖\0蒋\0蕉\0衝\0裳\0訟\0証\0詔\0詳\0象\0賞\0醤\0鉦\0鍾\0鐘\0障\0鞘\0上\0丈\0丞\0乗\0冗\0剰\0城\0場\0壌\0嬢\0常\0情\0擾\0条\0杖\0浄\0状\0畳\0穣\0蒸\0譲\0醸\0錠\0嘱\0埴\0飾\0" +  //30区
      "拭\0植\0殖\0燭\0織\0職\0色\0触\0食\0蝕\0辱\0尻\0伸\0信\0侵\0唇\0娠\0寝\0審\0心\0慎\0振\0新\0晋\0森\0榛\0浸\0深\0申\0疹\0真\0神\0秦\0紳\0臣\0芯\0薪\0親\0診\0身\0辛\0進\0針\0震\0人\0仁\0刃\0塵\0壬\0尋\0甚\0尽\0腎\0訊\0迅\0陣\0靭\0笥\0諏\0須\0酢\0図\0厨\0逗\0吹\0垂\0帥\0推\0水\0炊\0睡\0粋\0翠\0衰\0遂\0酔\0錐\0錘\0随\0瑞\0髄\0崇\0嵩\0数\0枢\0趨\0雛\0据\0杉\0椙\0菅\0頗\0雀\0裾\0" +  //31区
      "澄\0摺\0寸\0世\0瀬\0畝\0是\0凄\0制\0勢\0姓\0征\0性\0成\0政\0整\0星\0晴\0棲\0栖\0正\0清\0牲\0生\0盛\0精\0聖\0声\0製\0西\0誠\0誓\0請\0逝\0醒\0青\0静\0斉\0税\0脆\0隻\0席\0惜\0戚\0斥\0昔\0析\0石\0積\0籍\0績\0脊\0責\0赤\0跡\0蹟\0碩\0切\0拙\0接\0摂\0折\0設\0窃\0節\0説\0雪\0絶\0舌\0蝉\0仙\0先\0千\0占\0宣\0専\0尖\0川\0戦\0扇\0撰\0栓\0栴\0泉\0浅\0洗\0染\0潜\0煎\0煽\0旋\0穿\0箭\0線\0" +  //32区
      "繊\0羨\0腺\0舛\0船\0薦\0詮\0賎\0践\0選\0遷\0銭\0銑\0閃\0鮮\0前\0善\0漸\0然\0全\0禅\0繕\0膳\0糎\0噌\0塑\0岨\0措\0曾\0曽\0楚\0狙\0疏\0疎\0礎\0祖\0租\0粗\0素\0組\0蘇\0訴\0阻\0遡\0鼠\0僧\0創\0双\0叢\0倉\0喪\0壮\0奏\0爽\0宋\0層\0匝\0惣\0想\0捜\0掃\0挿\0掻\0操\0早\0曹\0巣\0槍\0槽\0漕\0燥\0争\0痩\0相\0窓\0糟\0総\0綜\0聡\0草\0荘\0葬\0蒼\0藻\0装\0走\0送\0遭\0鎗\0霜\0騒\0像\0増\0憎\0" +  //33区
      "臓\0蔵\0贈\0造\0促\0側\0則\0即\0息\0捉\0束\0測\0足\0速\0俗\0属\0賊\0族\0続\0卒\0袖\0其\0揃\0存\0孫\0尊\0損\0村\0遜\0他\0多\0太\0汰\0詑\0唾\0堕\0妥\0惰\0打\0柁\0舵\0楕\0陀\0駄\0騨\0体\0堆\0対\0耐\0岱\0帯\0待\0怠\0態\0戴\0替\0泰\0滞\0胎\0腿\0苔\0袋\0貸\0退\0逮\0隊\0黛\0鯛\0代\0台\0大\0第\0醍\0題\0鷹\0滝\0瀧\0卓\0啄\0宅\0托\0択\0拓\0沢\0濯\0琢\0託\0鐸\0濁\0諾\0茸\0凧\0蛸\0只\0" +  //34区
      "叩\0但\0達\0辰\0奪\0脱\0巽\0竪\0辿\0棚\0谷\0狸\0鱈\0樽\0誰\0丹\0単\0嘆\0坦\0担\0探\0旦\0歎\0淡\0湛\0炭\0短\0端\0箪\0綻\0耽\0胆\0蛋\0誕\0鍛\0団\0壇\0弾\0断\0暖\0檀\0段\0男\0談\0値\0知\0地\0弛\0恥\0智\0池\0痴\0稚\0置\0致\0蜘\0遅\0馳\0築\0畜\0竹\0筑\0蓄\0逐\0秩\0窒\0茶\0嫡\0着\0中\0仲\0宙\0忠\0抽\0昼\0柱\0注\0虫\0衷\0註\0酎\0鋳\0駐\0樗\0瀦\0猪\0苧\0著\0貯\0丁\0兆\0凋\0喋\0寵\0" +  //35区
      "帖\0帳\0庁\0弔\0張\0彫\0徴\0懲\0挑\0暢\0朝\0潮\0牒\0町\0眺\0聴\0脹\0腸\0蝶\0調\0諜\0超\0跳\0銚\0長\0頂\0鳥\0勅\0捗\0直\0朕\0沈\0珍\0賃\0鎮\0陳\0津\0墜\0椎\0槌\0追\0鎚\0痛\0通\0塚\0栂\0掴\0槻\0佃\0漬\0柘\0辻\0蔦\0綴\0鍔\0椿\0潰\0坪\0壷\0嬬\0紬\0爪\0吊\0釣\0鶴\0亭\0低\0停\0偵\0剃\0貞\0呈\0堤\0定\0帝\0底\0庭\0廷\0弟\0悌\0抵\0挺\0提\0梯\0汀\0碇\0禎\0程\0締\0艇\0訂\0諦\0蹄\0逓\0" +  //36区
      "邸\0鄭\0釘\0鼎\0泥\0摘\0擢\0敵\0滴\0的\0笛\0適\0鏑\0溺\0哲\0徹\0撤\0轍\0迭\0鉄\0典\0填\0天\0展\0店\0添\0纏\0甜\0貼\0転\0顛\0点\0伝\0殿\0澱\0田\0電\0兎\0吐\0堵\0塗\0妬\0屠\0徒\0斗\0杜\0渡\0登\0菟\0賭\0途\0都\0鍍\0砥\0砺\0努\0度\0土\0奴\0怒\0倒\0党\0冬\0凍\0刀\0唐\0塔\0塘\0套\0宕\0島\0嶋\0悼\0投\0搭\0東\0桃\0梼\0棟\0盗\0淘\0湯\0涛\0灯\0燈\0当\0痘\0祷\0等\0答\0筒\0糖\0統\0到\0" +  //37区
      "董\0蕩\0藤\0討\0謄\0豆\0踏\0逃\0透\0鐙\0陶\0頭\0騰\0闘\0働\0動\0同\0堂\0導\0憧\0撞\0洞\0瞳\0童\0胴\0萄\0道\0銅\0峠\0鴇\0匿\0得\0徳\0涜\0特\0督\0禿\0篤\0毒\0独\0読\0栃\0橡\0凸\0突\0椴\0届\0鳶\0苫\0寅\0酉\0瀞\0噸\0屯\0惇\0敦\0沌\0豚\0遁\0頓\0呑\0曇\0鈍\0奈\0那\0内\0乍\0凪\0薙\0謎\0灘\0捺\0鍋\0楢\0馴\0縄\0畷\0南\0楠\0軟\0難\0汝\0二\0尼\0弐\0迩\0匂\0賑\0肉\0虹\0廿\0日\0乳\0入\0" +  //38区
      "如\0尿\0韮\0任\0妊\0忍\0認\0濡\0禰\0祢\0寧\0葱\0猫\0熱\0年\0念\0捻\0撚\0燃\0粘\0乃\0廼\0之\0埜\0嚢\0悩\0濃\0納\0能\0脳\0膿\0農\0覗\0蚤\0巴\0把\0播\0覇\0杷\0波\0派\0琶\0破\0婆\0罵\0芭\0馬\0俳\0廃\0拝\0排\0敗\0杯\0盃\0牌\0背\0肺\0輩\0配\0倍\0培\0媒\0梅\0楳\0煤\0狽\0買\0売\0賠\0陪\0這\0蝿\0秤\0矧\0萩\0伯\0剥\0博\0拍\0柏\0泊\0白\0箔\0粕\0舶\0薄\0迫\0曝\0漠\0爆\0縛\0莫\0駁\0麦\0" +  //39区
      "函\0箱\0硲\0箸\0肇\0筈\0櫨\0幡\0肌\0畑\0畠\0八\0鉢\0溌\0発\0醗\0髪\0伐\0罰\0抜\0筏\0閥\0鳩\0噺\0塙\0蛤\0隼\0伴\0判\0半\0反\0叛\0帆\0搬\0斑\0板\0氾\0汎\0版\0犯\0班\0畔\0繁\0般\0藩\0販\0範\0釆\0煩\0頒\0飯\0挽\0晩\0番\0盤\0磐\0蕃\0蛮\0匪\0卑\0否\0妃\0庇\0彼\0悲\0扉\0批\0披\0斐\0比\0泌\0疲\0皮\0碑\0秘\0緋\0罷\0肥\0被\0誹\0費\0避\0非\0飛\0樋\0簸\0備\0尾\0微\0枇\0毘\0琵\0眉\0美\0" +  //40区
      "鼻\0柊\0稗\0匹\0疋\0髭\0彦\0膝\0菱\0肘\0弼\0必\0畢\0筆\0逼\0桧\0姫\0媛\0紐\0百\0謬\0俵\0彪\0標\0氷\0漂\0瓢\0票\0表\0評\0豹\0廟\0描\0病\0秒\0苗\0錨\0鋲\0蒜\0蛭\0鰭\0品\0彬\0斌\0浜\0瀕\0貧\0賓\0頻\0敏\0瓶\0不\0付\0埠\0夫\0婦\0富\0冨\0布\0府\0怖\0扶\0敷\0斧\0普\0浮\0父\0符\0腐\0膚\0芙\0譜\0負\0賦\0赴\0阜\0附\0侮\0撫\0武\0舞\0葡\0蕪\0部\0封\0楓\0風\0葺\0蕗\0伏\0副\0復\0幅\0服\0" +  //41区
      "福\0腹\0複\0覆\0淵\0弗\0払\0沸\0仏\0物\0鮒\0分\0吻\0噴\0墳\0憤\0扮\0焚\0奮\0粉\0糞\0紛\0雰\0文\0聞\0丙\0併\0兵\0塀\0幣\0平\0弊\0柄\0並\0蔽\0閉\0陛\0米\0頁\0僻\0壁\0癖\0碧\0別\0瞥\0蔑\0箆\0偏\0変\0片\0篇\0編\0辺\0返\0遍\0便\0勉\0娩\0弁\0鞭\0保\0舗\0鋪\0圃\0捕\0歩\0甫\0補\0輔\0穂\0募\0墓\0慕\0戊\0暮\0母\0簿\0菩\0倣\0俸\0包\0呆\0報\0奉\0宝\0峰\0峯\0崩\0庖\0抱\0捧\0放\0方\0朋\0" +  //42区
      "法\0泡\0烹\0砲\0縫\0胞\0芳\0萌\0蓬\0蜂\0褒\0訪\0豊\0邦\0鋒\0飽\0鳳\0鵬\0乏\0亡\0傍\0剖\0坊\0妨\0帽\0忘\0忙\0房\0暴\0望\0某\0棒\0冒\0紡\0肪\0膨\0謀\0貌\0貿\0鉾\0防\0吠\0頬\0北\0僕\0卜\0墨\0撲\0朴\0牧\0睦\0穆\0釦\0勃\0没\0殆\0堀\0幌\0奔\0本\0翻\0凡\0盆\0摩\0磨\0魔\0麻\0埋\0妹\0昧\0枚\0毎\0哩\0槙\0幕\0膜\0枕\0鮪\0柾\0鱒\0桝\0亦\0俣\0又\0抹\0末\0沫\0迄\0侭\0繭\0麿\0万\0慢\0満\0" +  //43区
      "漫\0蔓\0味\0未\0魅\0巳\0箕\0岬\0密\0蜜\0湊\0蓑\0稔\0脈\0妙\0粍\0民\0眠\0務\0夢\0無\0牟\0矛\0霧\0鵡\0椋\0婿\0娘\0冥\0名\0命\0明\0盟\0迷\0銘\0鳴\0姪\0牝\0滅\0免\0棉\0綿\0緬\0面\0麺\0摸\0模\0茂\0妄\0孟\0毛\0猛\0盲\0網\0耗\0蒙\0儲\0木\0黙\0目\0杢\0勿\0餅\0尤\0戻\0籾\0貰\0問\0悶\0紋\0門\0匁\0也\0冶\0夜\0爺\0耶\0野\0弥\0矢\0厄\0役\0約\0薬\0訳\0躍\0靖\0柳\0薮\0鑓\0愉\0愈\0油\0癒\0" +  //44区
      "諭\0輸\0唯\0佑\0優\0勇\0友\0宥\0幽\0悠\0憂\0揖\0有\0柚\0湧\0涌\0猶\0猷\0由\0祐\0裕\0誘\0遊\0邑\0郵\0雄\0融\0夕\0予\0余\0与\0誉\0輿\0預\0傭\0幼\0妖\0容\0庸\0揚\0揺\0擁\0曜\0楊\0様\0洋\0溶\0熔\0用\0窯\0羊\0耀\0葉\0蓉\0要\0謡\0踊\0遥\0陽\0養\0慾\0抑\0欲\0沃\0浴\0翌\0翼\0淀\0羅\0螺\0裸\0来\0莱\0頼\0雷\0洛\0絡\0落\0酪\0乱\0卵\0嵐\0欄\0濫\0藍\0蘭\0覧\0利\0吏\0履\0李\0梨\0理\0璃\0" +  //45区
      "痢\0裏\0裡\0里\0離\0陸\0律\0率\0立\0葎\0掠\0略\0劉\0流\0溜\0琉\0留\0硫\0粒\0隆\0竜\0龍\0侶\0慮\0旅\0虜\0了\0亮\0僚\0両\0凌\0寮\0料\0梁\0涼\0猟\0療\0瞭\0稜\0糧\0良\0諒\0遼\0量\0陵\0領\0力\0緑\0倫\0厘\0林\0淋\0燐\0琳\0臨\0輪\0隣\0鱗\0麟\0瑠\0塁\0涙\0累\0類\0令\0伶\0例\0冷\0励\0嶺\0怜\0玲\0礼\0苓\0鈴\0隷\0零\0霊\0麗\0齢\0暦\0歴\0列\0劣\0烈\0裂\0廉\0恋\0憐\0漣\0煉\0簾\0練\0聯\0" +  //46区
      "蓮\0連\0錬\0呂\0魯\0櫓\0炉\0賂\0路\0露\0労\0婁\0廊\0弄\0朗\0楼\0榔\0浪\0漏\0牢\0狼\0篭\0老\0聾\0蝋\0郎\0六\0麓\0禄\0肋\0録\0論\0倭\0和\0話\0歪\0賄\0脇\0惑\0枠\0鷲\0亙\0亘\0鰐\0詫\0藁\0蕨\0椀\0湾\0碗\0腕\0\u0078\u039f\u5b41\0\u5b56\0\u5b7d\0\u5b93\0\u5bd8\0\u5bec\0\u5c12\0\u5c1e\0\u5c23\0\u5c2b\0\u378d\0\u5c62\0\ufa3b\0\ufa3c\0\u007b\u02b4\u5c7a\0\u5c8f\0\u5c9f\0\u5ca3\0\u5caa\0\u5cba\0\u5ccb\0\u5cd0\0\u5cd2\0\u5cf4\0\u007d\u0234\u37e2\0\u5d0d\0\u5d27\0\ufa11\0\u5d46\0\u5d47\0\u5d53\0\u5d4a\0\u5d6d\0\u5d81\0\u5da0\0\u5da4\0\u5da7\0\u5db8\0\u5dcb\0\u541e\0" +  //47区
      "弌\0丐\0丕\0个\0丱\0丶\0丼\0丿\0乂\0乖\0乘\0亂\0亅\0豫\0亊\0舒\0弍\0于\0亞\0亟\0亠\0亢\0亰\0亳\0亶\0从\0仍\0仄\0仆\0仂\0仗\0仞\0仭\0仟\0价\0伉\0佚\0估\0佛\0佝\0佗\0佇\0佶\0侈\0侏\0侘\0佻\0佩\0佰\0侑\0佯\0來\0侖\0儘\0俔\0俟\0俎\0俘\0俛\0俑\0俚\0俐\0俤\0俥\0倚\0倨\0倔\0倪\0倥\0倅\0伜\0俶\0倡\0倩\0倬\0俾\0俯\0們\0倆\0偃\0假\0會\0偕\0偐\0偈\0做\0偖\0偬\0偸\0傀\0傚\0傅\0傴\0傲\0" +  //48区
      "僉\0僊\0傳\0僂\0僖\0僞\0僥\0僭\0僣\0僮\0價\0僵\0儉\0儁\0儂\0儖\0儕\0儔\0儚\0儡\0儺\0儷\0儼\0儻\0儿\0兀\0兒\0兌\0兔\0兢\0竸\0兩\0兪\0兮\0冀\0冂\0囘\0册\0冉\0冏\0冑\0冓\0冕\0冖\0冤\0冦\0冢\0冩\0冪\0冫\0决\0冱\0冲\0冰\0况\0冽\0凅\0凉\0凛\0几\0處\0凩\0凭\0凰\0凵\0凾\0刄\0刋\0刔\0刎\0刧\0刪\0刮\0刳\0刹\0剏\0剄\0剋\0剌\0剞\0剔\0剪\0剴\0剩\0剳\0剿\0剽\0劍\0劔\0劒\0剱\0劈\0劑\0辨\0" +  //49区
      "辧\0劬\0劭\0劼\0劵\0勁\0勍\0勗\0勞\0勣\0勦\0飭\0勠\0勳\0勵\0勸\0勹\0匆\0匈\0甸\0匍\0匐\0匏\0匕\0匚\0匣\0匯\0匱\0匳\0匸\0區\0卆\0卅\0丗\0卉\0卍\0凖\0卞\0卩\0卮\0夘\0卻\0卷\0厂\0厖\0厠\0厦\0厥\0厮\0厰\0厶\0參\0簒\0雙\0叟\0曼\0燮\0叮\0叨\0叭\0叺\0吁\0吽\0呀\0听\0吭\0吼\0吮\0吶\0吩\0吝\0呎\0咏\0呵\0咎\0呟\0呱\0呷\0呰\0咒\0呻\0咀\0呶\0咄\0咐\0咆\0哇\0咢\0咸\0咥\0咬\0哄\0哈\0咨\0" +  //50区
      "咫\0哂\0咤\0咾\0咼\0哘\0哥\0哦\0唏\0唔\0哽\0哮\0哭\0哺\0哢\0唹\0啀\0啣\0啌\0售\0啜\0啅\0啖\0啗\0唸\0唳\0啝\0喙\0喀\0咯\0喊\0喟\0啻\0啾\0喘\0喞\0單\0啼\0喃\0喩\0喇\0喨\0嗚\0嗅\0嗟\0嗄\0嗜\0嗤\0嗔\0嘔\0嗷\0嘖\0嗾\0嗽\0嘛\0嗹\0噎\0噐\0營\0嘴\0嘶\0嘲\0嘸\0噫\0噤\0嘯\0噬\0噪\0嚆\0嚀\0嚊\0嚠\0嚔\0嚏\0嚥\0嚮\0嚶\0嚴\0囂\0嚼\0囁\0囃\0囀\0囈\0囎\0囑\0囓\0囗\0囮\0囹\0圀\0囿\0圄\0圉\0" +  //51区
      "圈\0國\0圍\0圓\0團\0圖\0嗇\0圜\0圦\0圷\0圸\0坎\0圻\0址\0坏\0坩\0埀\0垈\0坡\0坿\0垉\0垓\0垠\0垳\0垤\0垪\0垰\0埃\0埆\0埔\0埒\0埓\0堊\0埖\0埣\0堋\0堙\0堝\0塲\0堡\0塢\0塋\0塰\0毀\0塒\0堽\0塹\0墅\0墹\0墟\0墫\0墺\0壞\0墻\0墸\0墮\0壅\0壓\0壑\0壗\0壙\0壘\0壥\0壜\0壤\0壟\0壯\0壺\0壹\0壻\0壼\0壽\0夂\0夊\0夐\0夛\0梦\0夥\0夬\0夭\0夲\0夸\0夾\0竒\0奕\0奐\0奎\0奚\0奘\0奢\0奠\0奧\0奬\0奩\0" +  //52区
      "奸\0妁\0妝\0佞\0侫\0妣\0妲\0姆\0姨\0姜\0妍\0姙\0姚\0娥\0娟\0娑\0娜\0娉\0娚\0婀\0婬\0婉\0娵\0娶\0婢\0婪\0媚\0媼\0媾\0嫋\0嫂\0媽\0嫣\0嫗\0嫦\0嫩\0嫖\0嫺\0嫻\0嬌\0嬋\0嬖\0嬲\0嫐\0嬪\0嬶\0嬾\0孃\0孅\0孀\0孑\0孕\0孚\0孛\0孥\0孩\0孰\0孳\0孵\0學\0斈\0孺\0宀\0它\0宦\0宸\0寃\0寇\0寉\0寔\0寐\0寤\0實\0寢\0寞\0寥\0寫\0寰\0寶\0寳\0尅\0將\0專\0對\0尓\0尠\0尢\0尨\0尸\0尹\0屁\0屆\0屎\0屓\0" +  //53区
      "屐\0屏\0孱\0屬\0屮\0乢\0屶\0屹\0岌\0岑\0岔\0妛\0岫\0岻\0岶\0岼\0岷\0峅\0岾\0峇\0峙\0峩\0峽\0峺\0峭\0嶌\0峪\0崋\0崕\0崗\0嵜\0崟\0崛\0崑\0崔\0崢\0崚\0崙\0崘\0嵌\0嵒\0嵎\0嵋\0嵬\0嵳\0嵶\0嶇\0嶄\0嶂\0嶢\0嶝\0嶬\0嶮\0嶽\0嶐\0嶷\0嶼\0巉\0巍\0巓\0巒\0巖\0巛\0巫\0已\0巵\0帋\0帚\0帙\0帑\0帛\0帶\0帷\0幄\0幃\0幀\0幎\0幗\0幔\0幟\0幢\0幤\0幇\0幵\0并\0幺\0麼\0广\0庠\0廁\0廂\0廈\0廐\0廏\0" +  //54区
      "廖\0廣\0廝\0廚\0廛\0廢\0廡\0廨\0廩\0廬\0廱\0廳\0廰\0廴\0廸\0廾\0弃\0弉\0彝\0彜\0弋\0弑\0弖\0弩\0弭\0弸\0彁\0彈\0彌\0彎\0弯\0彑\0彖\0彗\0彙\0彡\0彭\0彳\0彷\0徃\0徂\0彿\0徊\0很\0徑\0徇\0從\0徙\0徘\0徠\0徨\0徭\0徼\0忖\0忻\0忤\0忸\0忱\0忝\0悳\0忿\0怡\0恠\0怙\0怐\0怩\0怎\0怱\0怛\0怕\0怫\0怦\0怏\0怺\0恚\0恁\0恪\0恷\0恟\0恊\0恆\0恍\0恣\0恃\0恤\0恂\0恬\0恫\0恙\0悁\0悍\0惧\0悃\0悚\0" +  //55区
      "悄\0悛\0悖\0悗\0悒\0悧\0悋\0惡\0悸\0惠\0惓\0悴\0忰\0悽\0惆\0悵\0惘\0慍\0愕\0愆\0惶\0惷\0愀\0惴\0惺\0愃\0愡\0惻\0惱\0愍\0愎\0慇\0愾\0愨\0愧\0慊\0愿\0愼\0愬\0愴\0愽\0慂\0慄\0慳\0慷\0慘\0慙\0慚\0慫\0慴\0慯\0慥\0慱\0慟\0慝\0慓\0慵\0憙\0憖\0憇\0憬\0憔\0憚\0憊\0憑\0憫\0憮\0懌\0懊\0應\0懷\0懈\0懃\0懆\0憺\0懋\0罹\0懍\0懦\0懣\0懶\0懺\0懴\0懿\0懽\0懼\0懾\0戀\0戈\0戉\0戍\0戌\0戔\0戛\0" +  //56区
      "戞\0戡\0截\0戮\0戰\0戲\0戳\0扁\0扎\0扞\0扣\0扛\0扠\0扨\0扼\0抂\0抉\0找\0抒\0抓\0抖\0拔\0抃\0抔\0拗\0拑\0抻\0拏\0拿\0拆\0擔\0拈\0拜\0拌\0拊\0拂\0拇\0抛\0拉\0挌\0拮\0拱\0挧\0挂\0挈\0拯\0拵\0捐\0挾\0捍\0搜\0捏\0掖\0掎\0掀\0掫\0捶\0掣\0掏\0掉\0掟\0掵\0捫\0捩\0掾\0揩\0揀\0揆\0揣\0揉\0插\0揶\0揄\0搖\0搴\0搆\0搓\0搦\0搶\0攝\0搗\0搨\0搏\0摧\0摯\0摶\0摎\0攪\0撕\0撓\0撥\0撩\0撈\0撼\0" +  //57区
      "據\0擒\0擅\0擇\0撻\0擘\0擂\0擱\0擧\0舉\0擠\0擡\0抬\0擣\0擯\0攬\0擶\0擴\0擲\0擺\0攀\0擽\0攘\0攜\0攅\0攤\0攣\0攫\0攴\0攵\0攷\0收\0攸\0畋\0效\0敖\0敕\0敍\0敘\0敞\0敝\0敲\0數\0斂\0斃\0變\0斛\0斟\0斫\0斷\0旃\0旆\0旁\0旄\0旌\0旒\0旛\0旙\0无\0旡\0旱\0杲\0昊\0昃\0旻\0杳\0昵\0昶\0昴\0昜\0晏\0晄\0晉\0晁\0晞\0晝\0晤\0晧\0晨\0晟\0晢\0晰\0暃\0暈\0暎\0暉\0暄\0暘\0暝\0曁\0暹\0曉\0暾\0暼\0" +  //58区
      "曄\0暸\0曖\0曚\0曠\0昿\0曦\0曩\0曰\0曵\0曷\0朏\0朖\0朞\0朦\0朧\0霸\0朮\0朿\0朶\0杁\0朸\0朷\0杆\0杞\0杠\0杙\0杣\0杤\0枉\0杰\0枩\0杼\0杪\0枌\0枋\0枦\0枡\0枅\0枷\0柯\0枴\0柬\0枳\0柩\0枸\0柤\0柞\0柝\0柢\0柮\0枹\0柎\0柆\0柧\0檜\0栞\0框\0栩\0桀\0桍\0栲\0桎\0梳\0栫\0桙\0档\0桷\0桿\0梟\0梏\0梭\0梔\0條\0梛\0梃\0檮\0梹\0桴\0梵\0梠\0梺\0椏\0梍\0桾\0椁\0棊\0椈\0棘\0椢\0椦\0棡\0椌\0棍\0" +  //59区
      "棔\0棧\0棕\0椶\0椒\0椄\0棗\0棣\0椥\0棹\0棠\0棯\0椨\0椪\0椚\0椣\0椡\0棆\0楹\0楷\0楜\0楸\0楫\0楔\0楾\0楮\0椹\0楴\0椽\0楙\0椰\0楡\0楞\0楝\0榁\0楪\0榲\0榮\0槐\0榿\0槁\0槓\0榾\0槎\0寨\0槊\0槝\0榻\0槃\0榧\0樮\0榑\0榠\0榜\0榕\0榴\0槞\0槨\0樂\0樛\0槿\0權\0槹\0槲\0槧\0樅\0榱\0樞\0槭\0樔\0槫\0樊\0樒\0櫁\0樣\0樓\0橄\0樌\0橲\0樶\0橸\0橇\0橢\0橙\0橦\0橈\0樸\0樢\0檐\0檍\0檠\0檄\0檢\0檣\0" +  //60区
      "檗\0蘗\0檻\0櫃\0櫂\0檸\0檳\0檬\0櫞\0櫑\0櫟\0檪\0櫚\0櫪\0櫻\0欅\0蘖\0櫺\0欒\0欖\0鬱\0欟\0欸\0欷\0盜\0欹\0飮\0歇\0歃\0歉\0歐\0歙\0歔\0歛\0歟\0歡\0歸\0歹\0歿\0殀\0殄\0殃\0殍\0殘\0殕\0殞\0殤\0殪\0殫\0殯\0殲\0殱\0殳\0殷\0殼\0毆\0毋\0毓\0毟\0毬\0毫\0毳\0毯\0麾\0氈\0氓\0气\0氛\0氤\0氣\0汞\0汕\0汢\0汪\0沂\0沍\0沚\0沁\0沛\0汾\0汨\0汳\0沒\0沐\0泄\0泱\0泓\0沽\0泗\0泅\0泝\0沮\0沱\0沾\0" +  //61区
      "沺\0泛\0泯\0泙\0泪\0洟\0衍\0洶\0洫\0洽\0洸\0洙\0洵\0洳\0洒\0洌\0浣\0涓\0浤\0浚\0浹\0浙\0涎\0涕\0濤\0涅\0淹\0渕\0渊\0涵\0淇\0淦\0涸\0淆\0淬\0淞\0淌\0淨\0淒\0淅\0淺\0淙\0淤\0淕\0淪\0淮\0渭\0湮\0渮\0渙\0湲\0湟\0渾\0渣\0湫\0渫\0湶\0湍\0渟\0湃\0渺\0湎\0渤\0滿\0渝\0游\0溂\0溪\0溘\0滉\0溷\0滓\0溽\0溯\0滄\0溲\0滔\0滕\0溏\0溥\0滂\0溟\0潁\0漑\0灌\0滬\0滸\0滾\0漿\0滲\0漱\0滯\0漲\0滌\0" +  //62区
      "漾\0漓\0滷\0澆\0潺\0潸\0澁\0澀\0潯\0潛\0濳\0潭\0澂\0潼\0潘\0澎\0澑\0濂\0潦\0澳\0澣\0澡\0澤\0澹\0濆\0澪\0濟\0濕\0濬\0濔\0濘\0濱\0濮\0濛\0瀉\0瀋\0濺\0瀑\0瀁\0瀏\0濾\0瀛\0瀚\0潴\0瀝\0瀘\0瀟\0瀰\0瀾\0瀲\0灑\0灣\0炙\0炒\0炯\0烱\0炬\0炸\0炳\0炮\0烟\0烋\0烝\0烙\0焉\0烽\0焜\0焙\0煥\0煕\0熈\0煦\0煢\0煌\0煖\0煬\0熏\0燻\0熄\0熕\0熨\0熬\0燗\0熹\0熾\0燒\0燉\0燔\0燎\0燠\0燬\0燧\0燵\0燼\0" +  //63区
      "燹\0燿\0爍\0爐\0爛\0爨\0爭\0爬\0爰\0爲\0爻\0爼\0爿\0牀\0牆\0牋\0牘\0牴\0牾\0犂\0犁\0犇\0犒\0犖\0犢\0犧\0犹\0犲\0狃\0狆\0狄\0狎\0狒\0狢\0狠\0狡\0狹\0狷\0倏\0猗\0猊\0猜\0猖\0猝\0猴\0猯\0猩\0猥\0猾\0獎\0獏\0默\0獗\0獪\0獨\0獰\0獸\0獵\0獻\0獺\0珈\0玳\0珎\0玻\0珀\0珥\0珮\0珞\0璢\0琅\0瑯\0琥\0珸\0琲\0琺\0瑕\0琿\0瑟\0瑙\0瑁\0瑜\0瑩\0瑰\0瑣\0瑪\0瑶\0瑾\0璋\0璞\0璧\0瓊\0瓏\0瓔\0珱\0" +  //64区
      "瓠\0瓣\0瓧\0瓩\0瓮\0瓲\0瓰\0瓱\0瓸\0瓷\0甄\0甃\0甅\0甌\0甎\0甍\0甕\0甓\0甞\0甦\0甬\0甼\0畄\0畍\0畊\0畉\0畛\0畆\0畚\0畩\0畤\0畧\0畫\0畭\0畸\0當\0疆\0疇\0畴\0疊\0疉\0疂\0疔\0疚\0疝\0疥\0疣\0痂\0疳\0痃\0疵\0疽\0疸\0疼\0疱\0痍\0痊\0痒\0痙\0痣\0痞\0痾\0痿\0痼\0瘁\0痰\0痺\0痲\0痳\0瘋\0瘍\0瘉\0瘟\0瘧\0瘠\0瘡\0瘢\0瘤\0瘴\0瘰\0瘻\0癇\0癈\0癆\0癜\0癘\0癡\0癢\0癨\0癩\0癪\0癧\0癬\0癰\0" +  //65区
      "癲\0癶\0癸\0發\0皀\0皃\0皈\0皋\0皎\0皖\0皓\0皙\0皚\0皰\0皴\0皸\0皹\0皺\0盂\0盍\0盖\0盒\0盞\0盡\0盥\0盧\0盪\0蘯\0盻\0眈\0眇\0眄\0眩\0眤\0眞\0眥\0眦\0眛\0眷\0眸\0睇\0睚\0睨\0睫\0睛\0睥\0睿\0睾\0睹\0瞎\0瞋\0瞑\0瞠\0瞞\0瞰\0瞶\0瞹\0瞿\0瞼\0瞽\0瞻\0矇\0矍\0矗\0矚\0矜\0矣\0矮\0矼\0砌\0砒\0礦\0砠\0礪\0硅\0碎\0硴\0碆\0硼\0碚\0碌\0碣\0碵\0碪\0碯\0磑\0磆\0磋\0磔\0碾\0碼\0磅\0磊\0磬\0" +  //66区
      "磧\0磚\0磽\0磴\0礇\0礒\0礑\0礙\0礬\0礫\0祀\0祠\0祗\0祟\0祚\0祕\0祓\0祺\0祿\0禊\0禝\0禧\0齋\0禪\0禮\0禳\0禹\0禺\0秉\0秕\0秧\0秬\0秡\0秣\0稈\0稍\0稘\0稙\0稠\0稟\0禀\0稱\0稻\0稾\0稷\0穃\0穗\0穉\0穡\0穢\0穩\0龝\0穰\0穹\0穽\0窈\0窗\0窕\0窘\0窖\0窩\0竈\0窰\0窶\0竅\0竄\0窿\0邃\0竇\0竊\0竍\0竏\0竕\0竓\0站\0竚\0竝\0竡\0竢\0竦\0竭\0竰\0笂\0笏\0笊\0笆\0笳\0笘\0笙\0笞\0笵\0笨\0笶\0筐\0" +  //67区
      "筺\0笄\0筍\0笋\0筌\0筅\0筵\0筥\0筴\0筧\0筰\0筱\0筬\0筮\0箝\0箘\0箟\0箍\0箜\0箚\0箋\0箒\0箏\0筝\0箙\0篋\0篁\0篌\0篏\0箴\0篆\0篝\0篩\0簑\0簔\0篦\0篥\0籠\0簀\0簇\0簓\0篳\0篷\0簗\0簍\0篶\0簣\0簧\0簪\0簟\0簷\0簫\0簽\0籌\0籃\0籔\0籏\0籀\0籐\0籘\0籟\0籤\0籖\0籥\0籬\0籵\0粃\0粐\0粤\0粭\0粢\0粫\0粡\0粨\0粳\0粲\0粱\0粮\0粹\0粽\0糀\0糅\0糂\0糘\0糒\0糜\0糢\0鬻\0糯\0糲\0糴\0糶\0糺\0紆\0" +  //68区
      "紂\0紜\0紕\0紊\0絅\0絋\0紮\0紲\0紿\0紵\0絆\0絳\0絖\0絎\0絲\0絨\0絮\0絏\0絣\0經\0綉\0絛\0綏\0絽\0綛\0綺\0綮\0綣\0綵\0緇\0綽\0綫\0總\0綢\0綯\0緜\0綸\0綟\0綰\0緘\0緝\0緤\0緞\0緻\0緲\0緡\0縅\0縊\0縣\0縡\0縒\0縱\0縟\0縉\0縋\0縢\0繆\0繦\0縻\0縵\0縹\0繃\0縷\0縲\0縺\0繧\0繝\0繖\0繞\0繙\0繚\0繹\0繪\0繩\0繼\0繻\0纃\0緕\0繽\0辮\0繿\0纈\0纉\0續\0纒\0纐\0纓\0纔\0纖\0纎\0纛\0纜\0缸\0缺\0" +  //69区
      "罅\0罌\0罍\0罎\0罐\0网\0罕\0罔\0罘\0罟\0罠\0罨\0罩\0罧\0罸\0羂\0羆\0羃\0羈\0羇\0羌\0羔\0羞\0羝\0羚\0羣\0羯\0羲\0羹\0羮\0羶\0羸\0譱\0翅\0翆\0翊\0翕\0翔\0翡\0翦\0翩\0翳\0翹\0飜\0耆\0耄\0耋\0耒\0耘\0耙\0耜\0耡\0耨\0耿\0耻\0聊\0聆\0聒\0聘\0聚\0聟\0聢\0聨\0聳\0聲\0聰\0聶\0聹\0聽\0聿\0肄\0肆\0肅\0肛\0肓\0肚\0肭\0冐\0肬\0胛\0胥\0胙\0胝\0胄\0胚\0胖\0脉\0胯\0胱\0脛\0脩\0脣\0脯\0腋\0" +  //70区
      "隋\0腆\0脾\0腓\0腑\0胼\0腱\0腮\0腥\0腦\0腴\0膃\0膈\0膊\0膀\0膂\0膠\0膕\0膤\0膣\0腟\0膓\0膩\0膰\0膵\0膾\0膸\0膽\0臀\0臂\0膺\0臉\0臍\0臑\0臙\0臘\0臈\0臚\0臟\0臠\0臧\0臺\0臻\0臾\0舁\0舂\0舅\0與\0舊\0舍\0舐\0舖\0舩\0舫\0舸\0舳\0艀\0艙\0艘\0艝\0艚\0艟\0艤\0艢\0艨\0艪\0艫\0舮\0艱\0艷\0艸\0艾\0芍\0芒\0芫\0芟\0芻\0芬\0苡\0苣\0苟\0苒\0苴\0苳\0苺\0莓\0范\0苻\0苹\0苞\0茆\0苜\0茉\0苙\0" +  //71区
      "茵\0茴\0茖\0茲\0茱\0荀\0茹\0荐\0荅\0茯\0茫\0茗\0茘\0莅\0莚\0莪\0莟\0莢\0莖\0茣\0莎\0莇\0莊\0荼\0莵\0荳\0荵\0莠\0莉\0莨\0菴\0萓\0菫\0菎\0菽\0萃\0菘\0萋\0菁\0菷\0萇\0菠\0菲\0萍\0萢\0萠\0莽\0萸\0蔆\0菻\0葭\0萪\0萼\0蕚\0蒄\0葷\0葫\0蒭\0葮\0蒂\0葩\0葆\0萬\0葯\0葹\0萵\0蓊\0葢\0蒹\0蒿\0蒟\0蓙\0蓍\0蒻\0蓚\0蓐\0蓁\0蓆\0蓖\0蒡\0蔡\0蓿\0蓴\0蔗\0蔘\0蔬\0蔟\0蔕\0蔔\0蓼\0蕀\0蕣\0蕘\0蕈\0" +  //72区
      "蕁\0蘂\0蕋\0蕕\0薀\0薤\0薈\0薑\0薊\0薨\0蕭\0薔\0薛\0藪\0薇\0薜\0蕷\0蕾\0薐\0藉\0薺\0藏\0薹\0藐\0藕\0藝\0藥\0藜\0藹\0蘊\0蘓\0蘋\0藾\0藺\0蘆\0蘢\0蘚\0蘰\0蘿\0虍\0乕\0虔\0號\0虧\0虱\0蚓\0蚣\0蚩\0蚪\0蚋\0蚌\0蚶\0蚯\0蛄\0蛆\0蚰\0蛉\0蠣\0蚫\0蛔\0蛞\0蛩\0蛬\0蛟\0蛛\0蛯\0蜒\0蜆\0蜈\0蜀\0蜃\0蛻\0蜑\0蜉\0蜍\0蛹\0蜊\0蜴\0蜿\0蜷\0蜻\0蜥\0蜩\0蜚\0蝠\0蝟\0蝸\0蝌\0蝎\0蝴\0蝗\0蝨\0蝮\0蝙\0" +  //73区
      "蝓\0蝣\0蝪\0蠅\0螢\0螟\0螂\0螯\0蟋\0螽\0蟀\0蟐\0雖\0螫\0蟄\0螳\0蟇\0蟆\0螻\0蟯\0蟲\0蟠\0蠏\0蠍\0蟾\0蟶\0蟷\0蠎\0蟒\0蠑\0蠖\0蠕\0蠢\0蠡\0蠱\0蠶\0蠹\0蠧\0蠻\0衄\0衂\0衒\0衙\0衞\0衢\0衫\0袁\0衾\0袞\0衵\0衽\0袵\0衲\0袂\0袗\0袒\0袮\0袙\0袢\0袍\0袤\0袰\0袿\0袱\0裃\0裄\0裔\0裘\0裙\0裝\0裹\0褂\0裼\0裴\0裨\0裲\0褄\0褌\0褊\0褓\0襃\0褞\0褥\0褪\0褫\0襁\0襄\0褻\0褶\0褸\0襌\0褝\0襠\0襞\0" +  //74区
      "襦\0襤\0襭\0襪\0襯\0襴\0襷\0襾\0覃\0覈\0覊\0覓\0覘\0覡\0覩\0覦\0覬\0覯\0覲\0覺\0覽\0覿\0觀\0觚\0觜\0觝\0觧\0觴\0觸\0訃\0訖\0訐\0訌\0訛\0訝\0訥\0訶\0詁\0詛\0詒\0詆\0詈\0詼\0詭\0詬\0詢\0誅\0誂\0誄\0誨\0誡\0誑\0誥\0誦\0誚\0誣\0諄\0諍\0諂\0諚\0諫\0諳\0諧\0諤\0諱\0謔\0諠\0諢\0諷\0諞\0諛\0謌\0謇\0謚\0諡\0謖\0謐\0謗\0謠\0謳\0鞫\0謦\0謫\0謾\0謨\0譁\0譌\0譏\0譎\0證\0譖\0譛\0譚\0譫\0" +  //75区
      "譟\0譬\0譯\0譴\0譽\0讀\0讌\0讎\0讒\0讓\0讖\0讙\0讚\0谺\0豁\0谿\0豈\0豌\0豎\0豐\0豕\0豢\0豬\0豸\0豺\0貂\0貉\0貅\0貊\0貍\0貎\0貔\0豼\0貘\0戝\0貭\0貪\0貽\0貲\0貳\0貮\0貶\0賈\0賁\0賤\0賣\0賚\0賽\0賺\0賻\0贄\0贅\0贊\0贇\0贏\0贍\0贐\0齎\0贓\0賍\0贔\0贖\0赧\0赭\0赱\0赳\0趁\0趙\0跂\0趾\0趺\0跏\0跚\0跖\0跌\0跛\0跋\0跪\0跫\0跟\0跣\0跼\0踈\0踉\0跿\0踝\0踞\0踐\0踟\0蹂\0踵\0踰\0踴\0蹊\0" +  //76区
      "蹇\0蹉\0蹌\0蹐\0蹈\0蹙\0蹤\0蹠\0踪\0蹣\0蹕\0蹶\0蹲\0蹼\0躁\0躇\0躅\0躄\0躋\0躊\0躓\0躑\0躔\0躙\0躪\0躡\0躬\0躰\0軆\0躱\0躾\0軅\0軈\0軋\0軛\0軣\0軼\0軻\0軫\0軾\0輊\0輅\0輕\0輒\0輙\0輓\0輜\0輟\0輛\0輌\0輦\0輳\0輻\0輹\0轅\0轂\0輾\0轌\0轉\0轆\0轎\0轗\0轜\0轢\0轣\0轤\0辜\0辟\0辣\0辭\0辯\0辷\0迚\0迥\0迢\0迪\0迯\0邇\0迴\0逅\0迹\0迺\0逑\0逕\0逡\0逍\0逞\0逖\0逋\0逧\0逶\0逵\0逹\0迸\0" +  //77区
      "遏\0遐\0遑\0遒\0逎\0遉\0逾\0遖\0遘\0遞\0遨\0遯\0遶\0隨\0遲\0邂\0遽\0邁\0邀\0邊\0邉\0邏\0邨\0邯\0邱\0邵\0郢\0郤\0扈\0郛\0鄂\0鄒\0鄙\0鄲\0鄰\0酊\0酖\0酘\0酣\0酥\0酩\0酳\0酲\0醋\0醉\0醂\0醢\0醫\0醯\0醪\0醵\0醴\0醺\0釀\0釁\0釉\0釋\0釐\0釖\0釟\0釡\0釛\0釼\0釵\0釶\0鈞\0釿\0鈔\0鈬\0鈕\0鈑\0鉞\0鉗\0鉅\0鉉\0鉤\0鉈\0銕\0鈿\0鉋\0鉐\0銜\0銖\0銓\0銛\0鉚\0鋏\0銹\0銷\0鋩\0錏\0鋺\0鍄\0錮\0" +  //78区
      "錙\0錢\0錚\0錣\0錺\0錵\0錻\0鍜\0鍠\0鍼\0鍮\0鍖\0鎰\0鎬\0鎭\0鎔\0鎹\0鏖\0鏗\0鏨\0鏥\0鏘\0鏃\0鏝\0鏐\0鏈\0鏤\0鐚\0鐔\0鐓\0鐃\0鐇\0鐐\0鐶\0鐫\0鐵\0鐡\0鐺\0鑁\0鑒\0鑄\0鑛\0鑠\0鑢\0鑞\0鑪\0鈩\0鑰\0鑵\0鑷\0鑽\0鑚\0鑼\0鑾\0钁\0鑿\0閂\0閇\0閊\0閔\0閖\0閘\0閙\0閠\0閨\0閧\0閭\0閼\0閻\0閹\0閾\0闊\0濶\0闃\0闍\0闌\0闕\0闔\0闖\0關\0闡\0闥\0闢\0阡\0阨\0阮\0阯\0陂\0陌\0陏\0陋\0陷\0陜\0陞\0" +  //79区
      "陝\0陟\0陦\0陲\0陬\0隍\0隘\0隕\0隗\0險\0隧\0隱\0隲\0隰\0隴\0隶\0隸\0隹\0雎\0雋\0雉\0雍\0襍\0雜\0霍\0雕\0雹\0霄\0霆\0霈\0霓\0霎\0霑\0霏\0霖\0霙\0霤\0霪\0霰\0霹\0霽\0霾\0靄\0靆\0靈\0靂\0靉\0靜\0靠\0靤\0靦\0靨\0勒\0靫\0靱\0靹\0鞅\0靼\0鞁\0靺\0鞆\0鞋\0鞏\0鞐\0鞜\0鞨\0鞦\0鞣\0鞳\0鞴\0韃\0韆\0韈\0韋\0韜\0韭\0齏\0韲\0竟\0韶\0韵\0頏\0頌\0頸\0頤\0頡\0頷\0頽\0顆\0顏\0顋\0顫\0顯\0顰\0" +  //80区
      "顱\0顴\0顳\0颪\0颯\0颱\0颶\0飄\0飃\0飆\0飩\0飫\0餃\0餉\0餒\0餔\0餘\0餡\0餝\0餞\0餤\0餠\0餬\0餮\0餽\0餾\0饂\0饉\0饅\0饐\0饋\0饑\0饒\0饌\0饕\0馗\0馘\0馥\0馭\0馮\0馼\0駟\0駛\0駝\0駘\0駑\0駭\0駮\0駱\0駲\0駻\0駸\0騁\0騏\0騅\0駢\0騙\0騫\0騷\0驅\0驂\0驀\0驃\0騾\0驕\0驍\0驛\0驗\0驟\0驢\0驥\0驤\0驩\0驫\0驪\0骭\0骰\0骼\0髀\0髏\0髑\0髓\0體\0髞\0髟\0髢\0髣\0髦\0髯\0髫\0髮\0髴\0髱\0髷\0" +  //81区
      "髻\0鬆\0鬘\0鬚\0鬟\0鬢\0鬣\0鬥\0鬧\0鬨\0鬩\0鬪\0鬮\0鬯\0鬲\0魄\0魃\0魏\0魍\0魎\0魑\0魘\0魴\0鮓\0鮃\0鮑\0鮖\0鮗\0鮟\0鮠\0鮨\0鮴\0鯀\0鯊\0鮹\0鯆\0鯏\0鯑\0鯒\0鯣\0鯢\0鯤\0鯔\0鯡\0鰺\0鯲\0鯱\0鯰\0鰕\0鰔\0鰉\0鰓\0鰌\0鰆\0鰈\0鰒\0鰊\0鰄\0鰮\0鰛\0鰥\0鰤\0鰡\0鰰\0鱇\0鰲\0鱆\0鰾\0鱚\0鱠\0鱧\0鱶\0鱸\0鳧\0鳬\0鳰\0鴉\0鴈\0鳫\0鴃\0鴆\0鴪\0鴦\0鶯\0鴣\0鴟\0鵄\0鴕\0鴒\0鵁\0鴿\0鴾\0鵆\0鵈\0" +  //82区
      "鵝\0鵞\0鵤\0鵑\0鵐\0鵙\0鵲\0鶉\0鶇\0鶫\0鵯\0鵺\0鶚\0鶤\0鶩\0鶲\0鷄\0鷁\0鶻\0鶸\0鶺\0鷆\0鷏\0鷂\0鷙\0鷓\0鷸\0鷦\0鷭\0鷯\0鷽\0鸚\0鸛\0鸞\0鹵\0鹹\0鹽\0麁\0麈\0麋\0麌\0麒\0麕\0麑\0麝\0麥\0麩\0麸\0麪\0麭\0靡\0黌\0黎\0黏\0黐\0黔\0黜\0點\0黝\0黠\0黥\0黨\0黯\0黴\0黶\0黷\0黹\0黻\0黼\0黽\0鼇\0鼈\0皷\0鼕\0鼡\0鼬\0鼾\0齊\0齒\0齔\0齣\0齟\0齠\0齡\0齦\0齧\0齬\0齪\0齷\0齲\0齶\0龕\0龜\0龠\0" +  //83区
      "堯\0槇\0遙\0瑤\0\u51dc\0\u7199\0\u5653\0\u5de2\0\u5e14\0\u5e18\0\u5e58\0\u5e5e\0\u5ebe\0\uf928\0\u5ecb\0\u5ef9\0\u5f00\0\u5f02\0\u5f07\0\u5f1d\0\u5f23\0\u5f34\0\u5f36\0\u5f3d\0\u5f40\0\u5f45\0\u5f54\0\u5f58\0\u5f64\0\u5f67\0\u5f7d\0\u5f89\0\u5f9c\0\u5fa7\0\u5faf\0\u5fb5\0\u5fb7\0\u5fc9\0\u5fde\0\u5fe1\0\u5fe9\0\u600d\0\u6014\0\u6018\0\u6033\0\u6035\0\u6047\0\ufa3d\0\u609d\0\u609e\0\u60cb\0\u60d4\0\u60d5\0\u60dd\0\u60f8\0\u611c\0\u612b\0\u6130\0\u6137\0\ufa3e\0\u618d\0\ufa3f\0\u61bc\0\u61b9\0\ufa40\0\u6222\0\u623e\0\u6243\0\u6256\0\u625a\0\u626f\0\u6285\0\u62c4\0\u62d6\0\u62fc\0\u630a\0\u6318\0\u6339\0\u6343\0\u6365\0\u637c\0\u63e5\0\u63ed\0\u63f5\0\u6410\0\u6414\0\u6422\0\u6479\0\u6451\0\u6460\0\u646d\0\u64ce\0\u64be\0\u64bf\0" +  //84区
      "\u64c4\0\u64ca\0\u64d0\0\u64f7\0\u64fb\0\u6522\0\u6529\0\ufa41\0\u6567\0\u659d\0\ufa42\0\u6600\0\u6609\0\u6615\0\u661e\0\u663a\0\u6622\0\u6624\0\u662b\0\u6630\0\u6631\0\u6633\0\u66fb\0\u6648\0\u664c\0\u0082\u01c4\u6659\0\u665a\0\u6661\0\u6665\0\u6673\0\u6677\0\u6678\0\u668d\0\ufa43\0\u66a0\0\u66b2\0\u66bb\0\u66c6\0\u66c8\0\u3b22\0\u66db\0\u66e8\0\u66fa\0\u6713\0\uf929\0\u6733\0\u6766\0\u6747\0\u6748\0\u677b\0\u6781\0\u6793\0\u6798\0\u679b\0\u67bb\0\u67f9\0\u67c0\0\u67d7\0\u67fc\0\u6801\0\u6852\0\u681d\0\u682c\0\u6831\0\u685b\0\u6872\0\u6875\0\ufa44\0\u68a3\0\u68a5\0\u68b2\0\u68c8\0\u68d0\0\u68e8\0\u68ed\0\u68f0\0\u68f1\0\u68fc\0\u690a\0\u6949\0\u0083\u01c4\u6935\0\u6942\0\u6957\0\u6963\0\u6964\0\u6968\0\u6980\0\ufa14\0\u69a5\0\u69ad\0\u69cf\0\u3bb6\0" +  //85区
      "\u3bc3\0\u69e2\0\u69e9\0\u69ea\0\u69f5\0\u69f6\0\u6a0f\0\u6a15\0\u0083\u033f\u6a3b\0\u6a3e\0\u6a45\0\u6a50\0\u6a56\0\u6a5b\0\u6a6b\0\u6a73\0\u0083\u0363\u6a89\0\u6a94\0\u6a9d\0\u6a9e\0\u6aa5\0\u6ae4\0\u6ae7\0\u3c0f\0\uf91d\0\u6b1b\0\u6b1e\0\u6b2c\0\u6b35\0\u6b46\0\u6b56\0\u6b60\0\u6b65\0\u6b67\0\u6b77\0\u6b82\0\u6ba9\0\u6bad\0\uf970\0\u6bcf\0\u6bd6\0\u6bd7\0\u6bff\0\u6c05\0\u6c10\0\u6c33\0\u6c59\0\u6c5c\0\u6caa\0\u6c74\0\u6c76\0\u6c85\0\u6c86\0\u6c98\0\u6c9c\0\u6cfb\0\u6cc6\0\u6cd4\0\u6ce0\0\u6ceb\0\u6cee\0\u0085\u00fe\u6d04\0\u6d0e\0\u6d2e\0\u6d31\0\u6d39\0\u6d3f\0\u6d58\0\u6d65\0\ufa45\0\u6d82\0\u6d87\0\u6d89\0\u6d94\0\u6daa\0\u6dac\0\u6dbf\0\u6dc4\0\u6dd6\0\u6dda\0\u6ddb\0\u6ddd\0\u6dfc\0\ufa46\0\u6e34\0\u6e44\0\u6e5c\0\u6e5e\0\u6eab\0\u6eb1\0\u6ec1\0" +  //86区
      "\u6ec7\0\u6ece\0\u6f10\0\u6f1a\0\ufa47\0\u6f2a\0\u6f2f\0\u6f33\0\u6f51\0\u6f59\0\u6f5e\0\u6f61\0\u6f62\0\u6f7e\0\u6f88\0\u6f8c\0\u6f8d\0\u6f94\0\u6fa0\0\u6fa7\0\u6fb6\0\u6fbc\0\u6fc7\0\u6fca\0\u6ff9\0\u6ff0\0\u6ff5\0\u7005\0\u7006\0\u7028\0\u704a\0\u705d\0\u705e\0\u704e\0\u7064\0\u7075\0\u7085\0\u70a4\0\u70ab\0\u70b7\0\u70d4\0\u70d8\0\u70e4\0\u710f\0\u712b\0\u711e\0\u7120\0\u712e\0\u7130\0\u7146\0\u7147\0\u7151\0\ufa48\0\u7152\0\u715c\0\u7160\0\u7168\0\ufa15\0\u7185\0\u7187\0\u7192\0\u71c1\0\u71ba\0\u71c4\0\u71fe\0\u7200\0\u7215\0\u7255\0\u7256\0\u3e3f\0\u728d\0\u729b\0\u72be\0\u72c0\0\u72fb\0\u0087\u03f1\u7327\0\u7328\0\ufa16\0\u7350\0\u7366\0\u737c\0\u7395\0\u739f\0\u73a0\0\u73a2\0\u73a6\0\u73ab\0\u73c9\0\u73cf\0\u73d6\0\u73d9\0\u73e3\0\u73e9\0" +  //87区
      "\u7407\0\u740a\0\u741a\0\u741b\0\ufa4a\0\u7426\0\u7428\0\u742a\0\u742b\0\u742c\0\u742e\0\u742f\0\u7430\0\u7444\0\u7446\0\u7447\0\u744b\0\u7457\0\u7462\0\u746b\0\u746d\0\u7486\0\u7487\0\u7489\0\u7498\0\u749c\0\u749f\0\u74a3\0\u7490\0\u74a6\0\u74a8\0\u74a9\0\u74b5\0\u74bf\0\u74c8\0\u74c9\0\u74da\0\u74ff\0\u7501\0\u7517\0\u752f\0\u756f\0\u7579\0\u7592\0\u3f72\0\u75ce\0\u75e4\0\u7600\0\u7602\0\u7608\0\u7615\0\u7616\0\u7619\0\u761e\0\u762d\0\u7635\0\u7643\0\u764b\0\u7664\0\u7665\0\u766d\0\u766f\0\u7671\0\u7681\0\u769b\0\u769d\0\u769e\0\u76a6\0\u76aa\0\u76b6\0\u76c5\0\u76cc\0\u76ce\0\u76d4\0\u76e6\0\u76f1\0\u76fc\0\u770a\0\u7719\0\u7734\0\u7736\0\u7746\0\u774d\0\u774e\0\u775c\0\u775f\0\u7762\0\u777a\0\u7780\0\u7794\0\u77aa\0\u77e0\0\u782d\0\u008b\u008e" +  //88区
      "\u7843\0\u784e\0\u784f\0\u7851\0\u7868\0\u786e\0\ufa4b\0\u78b0\0\u008b\u010e\u78ad\0\u78e4\0\u78f2\0\u7900\0\u78f7\0\u791c\0\u792e\0\u7931\0\u7934\0\ufa4c\0\ufa4d\0\u7945\0\u7946\0\ufa4e\0\ufa4f\0\ufa50\0\u795c\0\ufa51\0\ufa19\0\ufa1a\0\u7979\0\ufa52\0\ufa53\0\ufa1b\0\u7998\0\u79b1\0\u79b8\0\u79c8\0\u79ca\0\u008b\u0371\u79d4\0\u79de\0\u79eb\0\u79ed\0\u7a03\0\ufa54\0\u7a39\0\u7a5d\0\u7a6d\0\ufa55\0\u7a85\0\u7aa0\0\u008c\u01c4\u7ab3\0\u7abb\0\u7ace\0\u7aeb\0\u7afd\0\u7b12\0\u7b2d\0\u7b3b\0\u7b47\0\u7b4e\0\u7b60\0\u7b6d\0\u7b6f\0\u7b72\0\u7b9e\0\ufa56\0\u7bd7\0\u7bd9\0\u7c01\0\u7c31\0\u7c1e\0\u7c20\0\u7c33\0\u7c36\0\u4264\0\u008d\u01a1\u7c59\0\u7c6d\0\u7c79\0\u7c8f\0\u7c94\0\u7ca0\0\u7cbc\0\u7cd5\0\u7cd9\0\u7cdd\0\u7d07\0\u7d08\0\u7d13\0\u7d1d\0\u7d23\0\u7d31\0" +  //89区
      "\u7d41\0\u7d48\0\u7d53\0\u7d5c\0\u7d7a\0\u7d83\0\u7d8b\0\u7da0\0\u7da6\0\u7dc2\0\u7dcc\0\u7dd6\0\u7de3\0\ufa57\0\u7e28\0\u7e08\0\u7e11\0\u7e15\0\ufa59\0\u7e47\0\u7e52\0\u7e61\0\u7e8a\0\u7e8d\0\u7f47\0\ufa5a\0\u7f91\0\u7f97\0\u7fbf\0\u7fce\0\u7fdb\0\u7fdf\0\u7fec\0\u7fee\0\u7ffa\0\ufa5b\0\u8014\0\u8026\0\u8035\0\u8037\0\u803c\0\u80ca\0\u80d7\0\u80e0\0\u80f3\0\u8118\0\u814a\0\u8160\0\u8167\0\u8168\0\u816d\0\u81bb\0\u81ca\0\u81cf\0\u81d7\0\ufa5c\0\u4453\0\u445b\0\u8260\0\u8274\0\u0090\u02ff\u828e\0\u82a1\0\u82a3\0\u82a4\0\u82a9\0\u82ae\0\u82b7\0\u82be\0\u82bf\0\u82c6\0\u82d5\0\u82fd\0\u82fe\0\u8300\0\u8301\0\u8362\0\u8322\0\u832d\0\u833a\0\u8343\0\u8347\0\u8351\0\u8355\0\u837d\0\u8386\0\u8392\0\u8398\0\u83a7\0\u83a9\0\u83bf\0\u83c0\0\u83c7\0\u83cf\0" +  //90区
      "\u83d1\0\u83e1\0\u83ea\0\u8401\0\u8406\0\u840a\0\ufa5f\0\u8448\0\u845f\0\u8470\0\u8473\0\u8485\0\u849e\0\u84af\0\u84b4\0\u84ba\0\u84c0\0\u84c2\0\u0091\u0240\u8532\0\u851e\0\u8523\0\u852f\0\u8559\0\u8564\0\ufa1f\0\u85ad\0\u857a\0\u858c\0\u858f\0\u85a2\0\u85b0\0\u85cb\0\u85ce\0\u85ed\0\u8612\0\u85ff\0\u8604\0\u8605\0\u8610\0\u0092\u00f4\u8618\0\u8629\0\u8638\0\u8657\0\u865b\0\uf936\0\u8662\0\u459d\0\u866c\0\u8675\0\u8698\0\u86b8\0\u86fa\0\u86fc\0\u86fd\0\u870b\0\u8771\0\u8787\0\u8788\0\u87ac\0\u87ad\0\u87b5\0\u45ea\0\u87d6\0\u87ec\0\u8806\0\u880a\0\u8810\0\u8814\0\u881f\0\u8898\0\u88aa\0\u88ca\0\u88ce\0\u0093\u0284\u88f5\0\u891c\0\ufa60\0\u8918\0\u8919\0\u891a\0\u8927\0\u8930\0\u8932\0\u8939\0\u8940\0\u8994\0\ufa61\0\u89d4\0\u89e5\0\u89f6\0\u8a12\0\u8a15\0" +  //91区
      "\u8a22\0\u8a37\0\u8a47\0\u8a4e\0\u8a5d\0\u8a61\0\u8a75\0\u8a79\0\u8aa7\0\u8ad0\0\u8adf\0\u8af4\0\u8af6\0\ufa22\0\ufa62\0\ufa63\0\u8b46\0\u8b54\0\u8b59\0\u8b69\0\u8b9d\0\u8c49\0\u8c68\0\ufa64\0\u8ce1\0\u8cf4\0\u8cf8\0\u8cfe\0\ufa65\0\u8d12\0\u8d1b\0\u8daf\0\u8dce\0\u8dd1\0\u8dd7\0\u8e20\0\u8e23\0\u8e3d\0\u8e70\0\u8e7b\0\u0096\u0277\u8ec0\0\u4844\0\u8efa\0\u8f1e\0\u8f2d\0\u8f36\0\u8f54\0\u0096\u03cd\u8fa6\0\u8fb5\0\u8fe4\0\u8fe8\0\u8fee\0\u9008\0\u902d\0\ufa67\0\u9088\0\u9095\0\u9097\0\u9099\0\u909b\0\u90a2\0\u90b3\0\u90be\0\u90c4\0\u90c5\0\u90c7\0\u90d7\0\u90dd\0\u90de\0\u90ef\0\u90f4\0\ufa26\0\u9114\0\u9115\0\u9116\0\u9122\0\u9123\0\u9127\0\u912f\0\u9131\0\u9134\0\u913d\0\u9148\0\u915b\0\u9183\0\u919e\0\u91ac\0\u91b1\0\u91bc\0\u91d7\0\u91fb\0\u91e4\0" +  //92区
      "\u91e5\0\u91ed\0\u91f1\0\u9207\0\u9210\0\u9238\0\u9239\0\u923a\0\u923c\0\u9240\0\u9243\0\u924f\0\u9278\0\u9288\0\u92c2\0\u92cb\0\u92cc\0\u92d3\0\u92e0\0\u92ff\0\u9304\0\u931f\0\u9321\0\u9325\0\u9348\0\u9349\0\u934a\0\u9364\0\u9365\0\u936a\0\u9370\0\u939b\0\u93a3\0\u93ba\0\u93c6\0\u93de\0\u93df\0\u9404\0\u93fd\0\u9433\0\u944a\0\u9463\0\u946b\0\u9471\0\u9472\0\u958e\0\u959f\0\u95a6\0\u95a9\0\u95ac\0\u95b6\0\u95bd\0\u95cb\0\u95d0\0\u95d3\0\u49b0\0\u95da\0\u95de\0\u9658\0\u9684\0\uf9dc\0\u969d\0\u96a4\0\u96a5\0\u96d2\0\u96de\0\ufa68\0\u96e9\0\u96ef\0\u9733\0\u973b\0\u974d\0\u974e\0\u974f\0\u975a\0\u976e\0\u9773\0\u9795\0\u97ae\0\u97ba\0\u97c1\0\u97c9\0\u97de\0\u97db\0\u97f4\0\ufa69\0\u980a\0\u981e\0\u982b\0\u9830\0\ufa6a\0\u9852\0\u9853\0\u9856\0" +  //93区
      "\u9857\0\u9859\0\u985a\0\uf9d0\0\u9865\0\u986c\0\u98ba\0\u98c8\0\u98e7\0\u9958\0\u999e\0\u9a02\0\u9a03\0\u9a24\0\u9a2d\0\u9a2e\0\u9a38\0\u9a4a\0\u9a4e\0\u9a52\0\u9ab6\0\u9ac1\0\u9ac3\0\u9ace\0\u9ad6\0\u9af9\0\u9b02\0\u9b08\0\u9b20\0\u4c17\0\u9b2d\0\u9b5e\0\u9b79\0\u9b66\0\u9b72\0\u9b75\0\u9b84\0\u9b8a\0\u9b8f\0\u9b9e\0\u9ba7\0\u9bc1\0\u9bce\0\u9be5\0\u9bf8\0\u9bfd\0\u9c00\0\u9c23\0\u9c41\0\u9c4f\0\u9c50\0\u9c53\0\u9c63\0\u9c65\0\u9c77\0\u9d1d\0\u9d1e\0\u9d43\0\u9d47\0\u9d52\0\u9d63\0\u9d70\0\u9d7c\0\u9d8a\0\u9d96\0\u9dc0\0\u9dac\0\u9dbc\0\u9dd7\0\u009e\u0190\u9de7\0\u9e07\0\u9e15\0\u9e7c\0\u9e9e\0\u9ea4\0\u9eac\0\u9eaf\0\u9eb4\0\u9eb5\0\u9ec3\0\u9ed1\0\u9f10\0\u9f39\0\u9f57\0\u9f90\0\u9f94\0\u9f97\0\u9fa2\0\u59f8\0\u5c5b\0\u5e77\0\u7626\0\u7e6b\0"  //94区
      ).toCharArray ();

    public FontPage.Han fnpHanPage;  //対応する半角フォントのページ

    public Zen (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName) {
      this (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName, null, 0);
    }
    public Zen (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName,
                byte[] memoryArray, int memoryAddress) {
      super (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName,
             94, 94, 77,
             memoryArray, memoryAddress,
             new char[][] { FULL_BASE });
    }

    //setHanPage (HanPage)
    //  対応する半角フォントのページを設定する
    public void setHanPage (FontPage.Han hanPage) {
      fnpHanPage = hanPage;
      fnpMaximumFontDataFileLength = hanPage.fnpBinaryBytes + fnpBinaryBytes;  //半角+全角
    }

    //yes = fnpIsFontDataFileLength (longLength)
    //  フォントデータファイルの長さか
    //  半角と全角は半角+全角を受け付ける
    @Override public boolean fnpIsFontDataFileLength (long longLength) {
      return (longLength == (long) fnpBinaryBytes ||  //全角のみ
              longLength == (long) (fnpHanPage.fnpBinaryBytes + fnpBinaryBytes));  //半角+全角
    }

    //success = fnpLoadFontDataArray (array, start, length)
    //  フォントデータファイルを配列から読み込む
    //  半角と全角は半角+全角を受け付ける
    @Override public boolean fnpLoadFontDataArray (byte[] array, int start, int length) {
      if (length == fnpCharacterBytes * 94 * 77) {  //94点×77区
        System.arraycopy (array, start, fnpBinaryArray, 0, fnpCharacterBytes * 94 * 8);  //1区～8区
        Arrays.fill (fnpBinaryArray, fnpCharacterBytes * 94 * 8, fnpCharacterBytes * 94 * 15, (byte) 0);  //9区～15区
        System.arraycopy (array, start + fnpCharacterBytes * 94 * 8, fnpBinaryArray, fnpCharacterBytes * 94 * 15, fnpCharacterBytes * 94 * 69);  //16区～84区
        Arrays.fill (fnpBinaryArray, fnpCharacterBytes * 94 * 84, fnpCharacterBytes * 94 * 94, (byte) 0);  //85区～94区
      } else if (length == fnpBinaryBytes) {  //全角のみ
        System.arraycopy (array, start, fnpBinaryArray, 0, fnpBinaryBytes);  //全角をコピーする
      } else if (length == fnpHanPage.fnpBinaryBytes + fnpBinaryBytes) {  //半角+全角
        System.arraycopy (array, start + fnpHanPage.fnpBinaryBytes, fnpBinaryArray, 0, fnpBinaryBytes);  //全角をコピーする
      } else {
        return false;
      }
      fnpBinaryToMemory ();
      fnpBinaryToImage ();
      if (!fnpReady) {
        System.out.println (Multilingual.mlnJapanese ? fnpNameJa + " フォントの準備ができました" :
                            fnpNameEn + " font is ready");
        fnpReady = true;  //フォントデータが有効
      }
      return true;
    }

    //array = fnpSaveFontDataArray ()
    //  フォントデータファイルを配列に書き出す
    //  全角は半角+全角を出力する
    @Override public byte[] fnpSaveFontDataArray () {
      byte[] array = new byte[fnpHanPage.fnpBinaryBytes + fnpBinaryBytes];  //半角+全角
      System.arraycopy (fnpHanPage.fnpBinaryArray, 0, array, 0, fnpHanPage.fnpBinaryBytes);  //半角をコピーする
      System.arraycopy (fnpBinaryArray, 0, array, fnpHanPage.fnpBinaryBytes, fnpBinaryBytes);  //全角をコピーする
      return array;
    }

    //fnpGetExtension ()
    //  フォントデータファイルの拡張子を返す
    //  全角と半角は.fonまたは.f<1文字の高さ>、それ以外は.dat
    @Override public String fnpGetExtension () {
      if (fnpExtension == null) {
        fnpExtension = fnpCharacterHeight == 16 ? ".fon" : ".f" + fnpCharacterHeight;
      }
      return fnpExtension;
    }

    //success = fnpInputImage (image)
    //  イメージを入力する
    //  イメージの幅と高さが合っていないとき失敗する
    //  全角の高さは94区だが、77区のときも1区～8区,16区～84区として受け付ける
    @Override public boolean fnpInputImage (BufferedImage image) {
      if (image.getWidth () == fnpImageWidth &&
          image.getHeight () == fnpCharacterHeight * 77) {  //94点×77区のとき
        Raster raster1to8 = image.getData (new Rectangle (0, 0, fnpImageWidth, fnpCharacterHeight * 8));  //1区～8区
        Raster raster16to84 = image.getData (new Rectangle (0, fnpCharacterHeight * 8, fnpImageWidth, fnpCharacterHeight * 69));  //16区～84区
        raster16to84 = raster16to84.createTranslatedChild (0, fnpCharacterHeight * 15);
        BufferedImage newImage = new BufferedImage (fnpImageWidth, fnpImageHeight, BufferedImage.TYPE_INT_RGB);  //不透明なので初期値は黒
        newImage.setData (raster1to8);  //1区～8区
        newImage.setData (raster16to84);  //16区～84区
        image = newImage;
      }
      return super.fnpInputImage (image);
    }

  }  //class FontPage.Zen



  //class FontPage.Prn
  //  CZ-8PC4のANK。16桁×32行。前半256文字がカタカナ、後半256文字がひらがな
  public static final class Prn extends FontPage {

    //CZ-8PC4のANK文字テーブル
    //  前半256文字がカタカナ、後半256文字がひらがなの512文字構成とする
    //
    //  特殊文字
    //    7f  π
    //    80  ▁  U+2581  LOWER ONE EIGHTH BLOCK
    //    81  ▂  U+2582  LOWER ONE QUARTER BLOCK
    //    82  ▃  U+2583  LOWER THREE EIGHTHS BLOCK
    //    83  ▄  U+2584  LOWER HALF BLOCK
    //    84  ▅  U+2585  LOWER FIVE EIGHTHS BLOCK
    //    85  ▆  U+2586  LOWER THREE QUARTERS BLOCK
    //    86  ▇  U+2587  LOWER SEVEN EIGHTHS BLOCK
    //    87  █  U+2588  FULL BLOCK
    //    88  ▏  U+258F  LEFT ONE EIGHTH BLOCK
    //    89  ▎  U+258E  LEFT ONE QUARTER BLOCK
    //    8a  ▍  U+258D  LEFT THREE EIGHTHS BLOCK
    //    8b  ▌  U+258C  LEFT HALF BLOCK
    //    8c  ▋  U+258B  LEFT FIVE EIGHTHS BLOCK
    //    8d  ▊  U+258A  LEFT THREE QUARTERS BLOCK
    //    8e  ▉  U+2589  LEFT SEVEN EIGHTHS BLOCK
    //    8f  ╱  U+2571  BOX DRAWINGS LIGHT DIAGONAL UPPER RIGHT TO LOWER LEFT
    //    90  ─  U+2500  BOX DRAWINGS LIGHT HORIZONTAL
    //    91  │  U+2502  BOX DRAWINGS LIGHT VERTICAL
    //    92  ┴  U+2534  BOX DRAWINGS LIGHT UP AND HORIZONTAL
    //    93  ┬  U+252C  BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
    //    94  ┤  U+2524  BOX DRAWINGS LIGHT VERTICAL AND LEFT
    //    95  ├  U+251C  BOX DRAWINGS LIGHT VERTICAL AND RIGHT
    //    96  ┼  U+253C  BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
    //    97  ┐  U+2510  BOX DRAWINGS LIGHT DOWN AND LEFT
    //    98  ┘  U+2518  BOX DRAWINGS LIGHT UP AND LEFT
    //    99  └  U+2514  BOX DRAWINGS LIGHT UP AND RIGHT
    //    9a  ┌  U+250C  BOX DRAWINGS LIGHT DOWN AND RIGHT
    //    9b  ╮  U+256E  BOX DRAWINGS LIGHT ARC DOWN AND LEFT
    //    9c  ╰  U+2570  BOX DRAWINGS LIGHT ARC UP AND RIGHT
    //    9d  ╯  U+256F  BOX DRAWINGS LIGHT ARC UP AND LEFT
    //    9e  ╭  U+256D  BOX DRAWINGS LIGHT ARC DOWN AND RIGHT
    //    9f  ╲  U+2572  BOX DRAWINGS LIGHT DIAGONAL UPPER LEFT TO LOWER RIGHT
    //    e0  ●  U+25CF  BLACK CIRCLE
    //    e1  ○  U+25CB  WHITE CIRCLE
    //    e2  ♠  U+2660  BLACK SPADE SUIT
    //    e3  ♥  U+2665  BLACK HEART SUIT
    //    e4  ♦  U+2666  BLACK DIAMOND SUIT
    //    e5  ♣  U+2663  BLACK CLUB SUIT
    //    e6  ◢  U+25E2  BLACK LOWER RIGHT TRIANGLE
    //    e7  ◣  U+25E3  BLACK LOWER LEFT TRIANGLE
    //    e8  ╳  U+2573  BOX DRAWINGS LIGHT DIAGONAL CROSS
    //    e9  \u2598  U+2598  QUADRANT UPPER LEFT
    //    ea  \u2596  U+2596  QUADRANT LOWER LEFT
    //    eb  \u259d  U+259D  QUADRANT UPPER RIGHT
    //    ec  \u2597  U+2597  QUADRANT LOWER RIGHT
    //    ed  \u259e  U+259E  QUADRANT UPPER RIGHT AND LOWER LEFT
    //    ee  \u259a  U+259A  QUADRANT UPPER LEFT AND LOWER RIGHT
    //    ef  □  U+25A1  WHITE SQUARE
    //    a0  ▓  U+2593  DARK SHADE
    //    a1  土
    //    a2  金
    //    a3  木
    //    a4  水
    //    a5  火
    //    a6  月
    //    a7  日
    //    a8  時
    //    a9  分
    //    aa  秒
    //    ab  年
    //    ac  円
    //    ad  人
    //    ae  生
    //    af  〒

    //半角フォントから作る文字
    public static final char[] HALF_BASE = (
      //カタカナ
      //123456789abcdef
      "                " +  //00
      "                " +  //01
      " !\"#$%&'()*+,-./" +  //02
      "0123456789:;<=>?" +  //03
      "@ABCDEFGHIJKLMNO" +  //04
      "PQRSTUVWXYZ[\u00a5]^_" +  //05
      "`abcdefghijklmno" +  //06
      "pqrstuvwxyz{|}\u00af " +  //07
      "\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588\u258f\u258e\u258d\u258c\u258b\u258a\u2589\u2571" +  //08
      "           \u256e\u2570\u256f\u256d\u2572" +  //09
      " ｡｢｣､･ｦｧｨｩｪｫｬｭｮｯ" +  //0a
      "ｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ" +  //0b
      "ﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ" +  //0c
      "ﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ" +  //0d
      "  \u2660\u2665\u2666\u2663\u25e2\u25e3\u2573       " +  //0e
      "\u2593               " +  //0f
      //ひらがな
      //123456789abcdef
      "                " +  //10
      "                " +  //11
      " !\"#$%&'()*+,-./" +  //12
      "0123456789:;<=>?" +  //13
      "@ABCDEFGHIJKLMNO" +  //14
      "PQRSTUVWXYZ[\u00a5]^_" +  //15
      "`abcdefghijklmno" +  //16
      "pqrstuvwxyz{|}\u00af " +  //17
      "\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588\u258f\u258e\u258d\u258c\u258b\u258a\u2589\u2571" +  //18
      "           \u256e\u2570\u256f\u256d\u2572" +  //19
      "                " +  //1a
      "                " +  //1b
      "                " +  //1c
      "                " +  //1d
      "  \u2660\u2665\u2666\u2663\u25e2\u25e3\u2573       " +  //1e
      "\u2593               "  //1f
      ).toCharArray ();

    //全角フォントから作る文字
    public static final char[] FULL_BASE = (
      //カタカナ
      //0 1 2 3 4 5 6 7 8 9 a b c d e f
      "　　　　　　　　　　　　　　　　" +  //00
      "　　　　　　　　　　　　　　　　" +  //01
      "　！”＃＄％＆’（）＊＋，－．／" +  //02
      "０１２３４５６７８９：；＜＝＞？" +  //03
      "＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ" +  //04
      "ＰＱＲＳＴＵＶＷＸＹＺ［￥］＾＿" +  //05
      "｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏ" +  //06
      "ｐｑｒｓｔｕｖｗｘｙｚ｛｜｝￣π" +  //07
      "　　　　　　　　　　　　　　　　" +  //08
      "─│┴┬┤├┼┐┘└┌　　　　　" +  //09
      "　　　　　　　　　　　　　　　　" +  //0a
      "　　　　　　　　　　　　　　　　" +  //0b
      "　　　　　　　　　　　　　　　　" +  //0c
      "　　　　　　　　　　　　　　　　" +  //0d
      "●○　　　　　　　\u2598\u2596\u259d\u2597\u259e\u259a□" +  //0e
      "　土金木水火月日時分秒年円人生〒" +  //0f
      //ひらがな
      //0 1 2 3 4 5 6 7 8 9 a b c d e f
      "　　　　　　　　　　　　　　　　" +  //10
      "　　　　　　　　　　　　　　　　" +  //11
      "　！”＃＄％＆’（）＊＋，－．／" +  //12
      "０１２３４５６７８９：；＜＝＞？" +  //13
      "＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ" +  //14
      "ＰＱＲＳＴＵＶＷＸＹＺ［￥］＾＿" +  //15
      "｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏ" +  //16
      "ｐｑｒｓｔｕｖｗｘｙｚ｛｜｝￣π" +  //17
      "　　　　　　　　　　　　　　　　" +  //18
      "─│┴┬┤├┼┐┘└┌　　　　　" +  //19
      "　。「」、・をぁぃぅぇぉゃゅょっ" +  //1a
      "ーあいうえおかきくけこさしすせそ" +  //1b
      "たちつてとなにぬねのはひふへほま" +  //1c
      "みむめもやゆよらりるれろわん゛゜" +  //1d
      "●○　　　　　　　\u2598\u2596\u259d\u2597\u259e\u259a□" +  //1e
      "　土金木水火月日時分秒年円人生〒"  //1f
      ).toCharArray ();

    public Prn (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName) {
      super (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName,
             16, 32, 32,
             null, 0,
             new char[][] { FULL_BASE, HALF_BASE });
    }

  }  //class FontPage.Prn



  //class FontPage.Lcd
  //  LCDインジケータのANK。16桁×16行
  public static final class Lcd extends FontPage {

    public Lcd (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName) {
      super (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName,
             16, 16, 16,
             null, 0,
             new char[][] {});
      if (characterHeight == 6) {
        fnpLoadFontDataArray (LCD4X6_FONT, 0, LCD4X6_FONT.length);
      } else if (characterHeight == 8) {
        fnpLoadFontDataArray (LCD6X8_FONT, 0, LCD6X8_FONT.length);
      }
    }

/*
    //  Lcd4x6フォント
    public static final byte[] LCD4X6_FONT = {
      //0x00 NL
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x01 SH
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x02 SX
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x03 EX
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x04 ET
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x05 EQ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x06 AK
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x07 BL
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x08 BS
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x09 HT
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0a LF
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0b VT
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0c FF
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0d CR
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0e SO
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0f SI
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x10 DE
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x11 D1
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x12 D2
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x13 D3
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x14 D4
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x15 NK
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x16 SN
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x17 EB
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x18 CN
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x19 EM
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1a SB
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1b EC
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1c →
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1d ←
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1e ↑
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1f ↓
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x20 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x21 ！
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      0b01000000,
      0b00000000,
      //0x22 ”
      0b10100000,
      0b10100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x23 ＃
      0b10100000,
      0b11100000,
      0b10100000,
      0b11100000,
      0b10100000,
      0b00000000,
      //0x24 ＄
      0b01100000,
      0b11000000,
      0b01000000,
      0b01100000,
      0b11000000,
      0b00000000,
      //0x25 ％
      0b10100000,
      0b11000000,
      0b01000000,
      0b01100000,
      0b10100000,
      0b00000000,
      //0x26 ＆
      0b11000000,
      0b11000000,
      0b01000000,
      0b10100000,
      0b11000000,
      0b00000000,
      //0x27 ’
      0b01000000,
      0b01000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x28 （
      0b00100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0x29 ）
      0b10000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0x2a ＊
      0b00000000,
      0b10100000,
      0b01000000,
      0b10100000,
      0b00000000,
      0b00000000,
      //0x2b ＋
      0b00000000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b00000000,
      0b00000000,
      //0x2c ，
      0b00000000,
      0b00000000,
      0b00000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x2d －
      0b00000000,
      0b00000000,
      0b11100000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x2e ．
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01000000,
      0b00000000,
      //0x2f ／
      0b00000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      0b00000000,
      //0x30 ０
      0b11100000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b11100000,
      0b00000000,
      //0x31 １
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x32 ２
      0b11100000,
      0b00100000,
      0b11100000,
      0b10000000,
      0b11100000,
      0b00000000,
      //0x33 ３
      0b11100000,
      0b00100000,
      0b11100000,
      0b00100000,
      0b11100000,
      0b00000000,
      //0x34 ４
      0b10100000,
      0b10100000,
      0b11100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x35 ５
      0b11100000,
      0b10000000,
      0b11100000,
      0b00100000,
      0b11100000,
      0b00000000,
      //0x36 ６
      0b11100000,
      0b10000000,
      0b11100000,
      0b10100000,
      0b11100000,
      0b00000000,
      //0x37 ７
      0b11100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x38 ８
      0b11100000,
      0b10100000,
      0b11100000,
      0b10100000,
      0b11100000,
      0b00000000,
      //0x39 ９
      0b11100000,
      0b10100000,
      0b11100000,
      0b00100000,
      0b11100000,
      0b00000000,
      //0x3a ：
      0b00000000,
      0b01000000,
      0b00000000,
      0b01000000,
      0b00000000,
      0b00000000,
      //0x3b ；
      0b00000000,
      0b01000000,
      0b00000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x3c ＜
      0b00100000,
      0b01000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0x3d ＝
      0b00000000,
      0b11100000,
      0b00000000,
      0b11100000,
      0b00000000,
      0b00000000,
      //0x3e ＞
      0b10000000,
      0b01000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0x3f ？
      0b11000000,
      0b00100000,
      0b01000000,
      0b00000000,
      0b01000000,
      0b00000000,
      //0x40 ＠
      0b01000000,
      0b10100000,
      0b11100000,
      0b10000000,
      0b01100000,
      0b00000000,
      //0x41 Ａ
      0b01000000,
      0b10100000,
      0b11100000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x42 Ｂ
      0b11000000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b11000000,
      0b00000000,
      //0x43 Ｃ
      0b01100000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b01100000,
      0b00000000,
      //0x44 Ｄ
      0b11000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b11000000,
      0b00000000,
      //0x45 Ｅ
      0b11100000,
      0b10000000,
      0b11100000,
      0b10000000,
      0b11100000,
      0b00000000,
      //0x46 Ｆ
      0b11100000,
      0b10000000,
      0b11100000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x47 Ｇ
      0b01100000,
      0b10000000,
      0b10100000,
      0b10100000,
      0b01100000,
      0b00000000,
      //0x48 Ｈ
      0b10100000,
      0b10100000,
      0b11100000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x49 Ｉ
      0b11100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b11100000,
      0b00000000,
      //0x4a Ｊ
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b11000000,
      0b00000000,
      //0x4b Ｋ
      0b10100000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x4c Ｌ
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b11100000,
      0b00000000,
      //0x4d Ｍ
      0b10100000,
      0b11100000,
      0b11100000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x4e Ｎ
      0b11000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x4f Ｏ
      0b01000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b01000000,
      0b00000000,
      //0x50 Ｐ
      0b11000000,
      0b10100000,
      0b11000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x51 Ｑ
      0b01000000,
      0b10100000,
      0b10100000,
      0b11100000,
      0b01100000,
      0b00000000,
      //0x52 Ｒ
      0b11000000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x53 Ｓ
      0b01100000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b11000000,
      0b00000000,
      //0x54 Ｔ
      0b11100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x55 Ｕ
      0b10100000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b11100000,
      0b00000000,
      //0x56 Ｖ
      0b10100000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b01000000,
      0b00000000,
      //0x57 Ｗ
      0b10100000,
      0b10100000,
      0b11100000,
      0b11100000,
      0b10100000,
      0b00000000,
      //0x58 Ｘ
      0b10100000,
      0b10100000,
      0b01000000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x59 Ｙ
      0b10100000,
      0b10100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x5a Ｚ
      0b11100000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b11100000,
      0b00000000,
      //0x5b ［
      0b01100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01100000,
      0b00000000,
      //0x5c ￥
      0b00000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x5d ］
      0b11000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b11000000,
      0b00000000,
      //0x5e ＾
      0b01000000,
      0b10100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x5f ＿
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11100000,
      0b00000000,
      //0x60 ｀
      0b01000000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x61 ａ
      0b00000000,
      0b01100000,
      0b10100000,
      0b10100000,
      0b01100000,
      0b00000000,
      //0x62 ｂ
      0b10000000,
      0b11000000,
      0b10100000,
      0b10100000,
      0b11000000,
      0b00000000,
      //0x63 ｃ
      0b00000000,
      0b01100000,
      0b10000000,
      0b10000000,
      0b01100000,
      0b00000000,
      //0x64 ｄ
      0b00100000,
      0b01100000,
      0b10100000,
      0b10100000,
      0b01100000,
      0b00000000,
      //0x65 ｅ
      0b00000000,
      0b01100000,
      0b10100000,
      0b11000000,
      0b01100000,
      0b00000000,
      //0x66 ｆ
      0b01100000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x67 ｇ
      0b01100000,
      0b10100000,
      0b11100000,
      0b00100000,
      0b11000000,
      0b00000000,
      //0x68 ｈ
      0b10000000,
      0b11000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x69 ｉ
      0b01000000,
      0b00000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x6a ｊ
      0b00100000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b11000000,
      0b00000000,
      //0x6b ｋ
      0b10000000,
      0b10100000,
      0b11000000,
      0b11000000,
      0b10100000,
      0b00000000,
      //0x6c ｌ
      0b11000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x6d ｍ
      0b00000000,
      0b11000000,
      0b11100000,
      0b11100000,
      0b10100000,
      0b00000000,
      //0x6e ｎ
      0b00000000,
      0b11000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b00000000,
      //0x6f ｏ
      0b00000000,
      0b01000000,
      0b10100000,
      0b10100000,
      0b01000000,
      0b00000000,
      //0x70 ｐ
      0b00000000,
      0b11000000,
      0b10100000,
      0b11000000,
      0b10000000,
      0b00000000,
      //0x71 ｑ
      0b00000000,
      0b01100000,
      0b10100000,
      0b01100000,
      0b00100000,
      0b00000000,
      //0x72 ｒ
      0b00000000,
      0b01100000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x73 ｓ
      0b00000000,
      0b01100000,
      0b01000000,
      0b00100000,
      0b11000000,
      0b00000000,
      //0x74 ｔ
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0x75 ｕ
      0b00000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b01100000,
      0b00000000,
      //0x76 ｖ
      0b00000000,
      0b10100000,
      0b10100000,
      0b10100000,
      0b01000000,
      0b00000000,
      //0x77 ｗ
      0b00000000,
      0b10100000,
      0b11100000,
      0b11100000,
      0b01100000,
      0b00000000,
      //0x78 ｘ
      0b00000000,
      0b10100000,
      0b01000000,
      0b01000000,
      0b10100000,
      0b00000000,
      //0x79 ｙ
      0b00000000,
      0b10100000,
      0b10100000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0x7a ｚ
      0b00000000,
      0b11100000,
      0b00100000,
      0b01000000,
      0b11100000,
      0b00000000,
      //0x7b ｛
      0b00100000,
      0b01000000,
      0b11000000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0x7c ｜
      0b01000000,
      0b01000000,
      0b00000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x7d ｝
      0b10000000,
      0b01000000,
      0b01100000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0x7e ￣
      0b11100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x7f 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x80 ＼
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x81 ～
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x82 ￤
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x83 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x84 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x85 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x86 を
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x87 ぁ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x88 ぃ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x89 ぅ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8a ぇ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8b ぉ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8c ゃ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8d ゅ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8e ょ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8f っ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x90 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x91 あ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x92 い
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x93 う
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x94 え
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x95 お
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x96 か
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x97 き
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x98 く
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x99 け
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9a こ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9b さ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9c し
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9d す
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9e せ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9f そ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa0 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa1 。
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa2 「
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa3 」
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa4 、
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa5 ・
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa6 ヲ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa7 ァ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa8 ィ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa9 ゥ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xaa ェ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xab ォ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xac ャ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xad ュ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xae ョ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xaf ッ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb0 ー
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb1 ア
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb2 イ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb3 ウ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb4 エ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb5 オ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb6 カ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb7 キ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb8 ク
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb9 ケ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xba コ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xbb サ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xbc シ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xbd ス
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xbe セ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xbf ソ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc0 タ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc1 チ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc2 ツ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc3 テ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc4 ト
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc5 ナ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc6 ニ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc7 ヌ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc8 ネ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xc9 ノ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xca ハ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xcb ヒ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xcc フ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xcd ヘ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xce ホ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xcf マ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd0 ミ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd1 ム
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd2 メ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd3 モ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd4 ヤ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd5 ユ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd6 ヨ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd7 ラ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd8 リ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xd9 ル
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xda レ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xdb ロ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xdc ワ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xdd ン
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xde ゛
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xdf ゜
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe0 た
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe1 ち
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe2 つ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe3 て
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe4 と
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe5 な
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe6 に
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe7 ぬ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe8 ね
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe9 の
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xea は
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xeb ひ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xec ふ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xed へ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xee ほ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xef ま
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf0 み
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf1 む
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf2 め
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf3 も
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf4 や
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf5 ゆ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf6 よ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf7 ら
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf8 り
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf9 る
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfa れ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfb ろ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfc わ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfd ん
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfe 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xff 　
      0b11100000,
      0b11100000,
      0b11100000,
      0b11100000,
      0b11100000,
      0b00000000,
    }  //LCD4X6_FONT
*/
    //  perl ../misc/itob.pl FontPage.java LCD4X6_FONT
    public static final byte[] LCD4X6_FONT = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0@@@\0@\0\240\240\0\0\0\0\240\340\240\340\240\0`\300@`\300\0\240\300@`\240\0\300\300@\240\300\0@@\0\0\0\0 @@@ \0\200@@@\200\0\0\240@\240\0\0\0@\340@\0\0\0\0\0@@\0\0\0\340\0\0\0\0\0\0\0@\0\0 @\200\0\0\340\240\240\240\340\0@@@@@\0\340 \340\200\340\0\340 \340 \340\0\240\240\340  \0\340\200\340 \340\0\340\200\340\240\340\0\340    \0\340\240\340\240\340\0\340\240\340 \340\0\0@\0@\0\0\0@\0@@\0 @\200@ \0\0\340\0\340\0\0\200@ @\200\0\300 @\0@\0@\240\340\200`\0@\240\340\240\240\0\300\240\300\240\300\0`\200\200\200`\0\300\240\240\240\300\0\340\200\340\200\340\0\340\200\340\200\200\0`\200\240\240`\0\240\240\340\240\240\0\340@@@\340\0    \300\0\240\240\300\240\240\0\200\200\200\200\340\0\240\340\340\240\240\0\300\240\240\240\240\0@\240\240\240@\0\300\240\300\200\200\0@\240\240\340`\0\300\240\300\240\240\0`\200@ \300\0\340@@@@\0\240\240\240\240\340\0\240\240\240\240@\0\240\240\340\340\240\0\240\240@\240\240\0\240\240@@@\0\340 @\200\340\0`@@@`\0\0\200@ \0\0\300@@@\300\0@\240\0\0\0\0\0\0\0\0\340\0@ \0\0\0\0\0`\240\240`\0\200\300\240\240\300\0\0`\200\200`\0 `\240\240`\0\0`\240\300`\0`\340@@@\0`\240\340 \300\0\200\300\240\240\240\0@\0@@@\0 \0  \300\0\200\240\300\300\240\0\300@@@@\0\0\300\340\340\240\0\0\300\240\240\240\0\0@\240\240@\0\0\300\240\300\200\0\0`\240` \0\0`\200\200\200\0\0`@ \300\0@\340@@ \0\0\240\240\240`\0\0\240\240\240@\0\0\240\340\340`\0\0\240@@\240\0\0\240\240@\200\0\0\340 @\340\0 @\300@ \0@@\0@@\0\200@`@\200\0\340\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\340\340\340\340\340\0".getBytes (XEiJ.ISO_8859_1);

/*
    //  Lcd6x8フォント
    public static final byte[] LCD6X8_FONT = {
      //0x00 NL
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x01 SH
      0b01100000,
      0b10000000,
      0b01101000,
      0b00101000,
      0b11111000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x02 SX
      0b01100000,
      0b10000000,
      0b01101000,
      0b00101000,
      0b11010000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x03 EX
      0b11100000,
      0b10000000,
      0b11100000,
      0b10101000,
      0b11110000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x04 ET
      0b11100000,
      0b10000000,
      0b11111000,
      0b10010000,
      0b11110000,
      0b00010000,
      0b00010000,
      0b00000000,
      //0x05 EQ
      0b11100000,
      0b10000000,
      0b11010000,
      0b10101000,
      0b11101000,
      0b00110000,
      0b00011000,
      0b00000000,
      //0x06 AK
      0b01000000,
      0b10100000,
      0b11101000,
      0b10101000,
      0b10110000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x07 BL
      0b11000000,
      0b10100000,
      0b11100000,
      0b10100000,
      0b11100000,
      0b00100000,
      0b00111000,
      0b00000000,
      //0x08 BS
      0b11000000,
      0b10100000,
      0b11011000,
      0b10100000,
      0b11010000,
      0b00001000,
      0b00110000,
      0b00000000,
      //0x09 HT
      0b10100000,
      0b10100000,
      0b11111000,
      0b10110000,
      0b10110000,
      0b00010000,
      0b00010000,
      0b00000000,
      //0x0a LF
      0b10000000,
      0b10000000,
      0b10111000,
      0b10100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x0b VT
      0b10100000,
      0b10100000,
      0b10111000,
      0b10110000,
      0b01010000,
      0b00010000,
      0b00010000,
      0b00000000,
      //0x0c FF
      0b11100000,
      0b10000000,
      0b11111000,
      0b10100000,
      0b10111000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x0d CR
      0b01100000,
      0b10000000,
      0b10110000,
      0b10101000,
      0b01110000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x0e SO
      0b01100000,
      0b10000000,
      0b01010000,
      0b00101000,
      0b11101000,
      0b00101000,
      0b00010000,
      0b00000000,
      //0x0f SI
      0b01100000,
      0b10000000,
      0b01001000,
      0b00101000,
      0b11001000,
      0b00001000,
      0b00001000,
      0b00000000,
      //0x10 DE
      0b11000000,
      0b10100000,
      0b10111000,
      0b10100000,
      0b11110000,
      0b00100000,
      0b00111000,
      0b00000000,
      //0x11 D1
      0b11000000,
      0b10100000,
      0b10101000,
      0b10101000,
      0b11001000,
      0b00001000,
      0b00001000,
      0b00000000,
      //0x12 D2
      0b11000000,
      0b10100000,
      0b10111000,
      0b10101000,
      0b11010000,
      0b00100000,
      0b00111000,
      0b00000000,
      //0x13 D3
      0b11000000,
      0b10100000,
      0b10111000,
      0b10101000,
      0b11011000,
      0b00001000,
      0b00111000,
      0b00000000,
      //0x14 D4
      0b11000000,
      0b10100000,
      0b10101000,
      0b10101000,
      0b11011000,
      0b00001000,
      0b00001000,
      0b00000000,
      //0x15 NK
      0b11000000,
      0b10100000,
      0b10100000,
      0b10101000,
      0b10110000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x16 SN
      0b01100000,
      0b10000000,
      0b01110000,
      0b00101000,
      0b11101000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x17 EB
      0b11100000,
      0b10000000,
      0b11110000,
      0b10101000,
      0b11110000,
      0b00101000,
      0b00110000,
      0b00000000,
      //0x18 CN
      0b01100000,
      0b10000000,
      0b10110000,
      0b10101000,
      0b01101000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x19 EM
      0b11100000,
      0b10000000,
      0b11101000,
      0b10111000,
      0b11111000,
      0b00101000,
      0b00101000,
      0b00000000,
      //0x1a SB
      0b01100000,
      0b10000000,
      0b01110000,
      0b00101000,
      0b11110000,
      0b00101000,
      0b00110000,
      0b00000000,
      //0x1b EC
      0b11100000,
      0b10000000,
      0b11011000,
      0b10100000,
      0b11100000,
      0b00100000,
      0b00011000,
      0b00000000,
      //0x1c →
      0b00000000,
      0b00100000,
      0b00010000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x1d ←
      0b00000000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b01000000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x1e ↑
      0b00100000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x1f ↓
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b10101000,
      0b01110000,
      0b00100000,
      0b00000000,
      //0x20 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x21 ！
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00100000,
      0b00000000,
      //0x22 ”
      0b01010000,
      0b01010000,
      0b01010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x23 ＃
      0b01010000,
      0b01010000,
      0b11111000,
      0b01010000,
      0b11111000,
      0b01010000,
      0b01010000,
      0b00000000,
      //0x24 ＄
      0b00100000,
      0b01111000,
      0b10100000,
      0b01110000,
      0b00101000,
      0b11110000,
      0b00100000,
      0b00000000,
      //0x25 ％
      0b11000000,
      0b11001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10011000,
      0b00011000,
      0b00000000,
      //0x26 ＆
      0b01100000,
      0b10010000,
      0b10100000,
      0b01000000,
      0b10101000,
      0b10010000,
      0b01101000,
      0b00000000,
      //0x27 ’
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x28 （
      0b00010000,
      0b00100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0x29 ）
      0b01000000,
      0b00100000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x2a ＊
      0b00000000,
      0b00100000,
      0b10101000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x2b ＋
      0b00000000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x2c ，
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x2d －
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x2e ．
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      //0x2f ／
      0b00000000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      0b00000000,
      //0x30 ０
      0b01110000,
      0b10001000,
      0b10011000,
      0b10101000,
      0b11001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x31 １
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x32 ２
      0b01110000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b00000000,
      //0x33 ３
      0b11111000,
      0b00010000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x34 ４
      0b00010000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b11111000,
      0b00010000,
      0b00010000,
      0b00000000,
      //0x35 ５
      0b11111000,
      0b10000000,
      0b11110000,
      0b00001000,
      0b00001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x36 ６
      0b00110000,
      0b01000000,
      0b10000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x37 ７
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x38 ８
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x39 ９
      0b01110000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0x3a ：
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b00000000,
      //0x3b ；
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x3c ＜
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0x3d ＝
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x3e ＞
      0b01000000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x3f ？
      0b01110000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b00000000,
      //0x40 ＠
      0b01110000,
      0b10001000,
      0b00001000,
      0b01101000,
      0b10101000,
      0b10101000,
      0b01110000,
      0b00000000,
      //0x41 Ａ
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x42 Ｂ
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b00000000,
      //0x43 Ｃ
      0b01110000,
      0b10001000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x44 Ｄ
      0b11100000,
      0b10010000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10010000,
      0b11100000,
      0b00000000,
      //0x45 Ｅ
      0b11111000,
      0b10000000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0x46 Ｆ
      0b11111000,
      0b10000000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x47 Ｇ
      0b01110000,
      0b10001000,
      0b10000000,
      0b10111000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0x48 Ｈ
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x49 Ｉ
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x4a Ｊ
      0b00111000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b10010000,
      0b01100000,
      0b00000000,
      //0x4b Ｋ
      0b10001000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10010000,
      0b10001000,
      0b00000000,
      //0x4c Ｌ
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0x4d Ｍ
      0b10001000,
      0b11011000,
      0b10101000,
      0b10101000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x4e Ｎ
      0b10001000,
      0b10001000,
      0b11001000,
      0b10101000,
      0b10011000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x4f Ｏ
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x50 Ｐ
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x51 Ｑ
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10010000,
      0b01101000,
      0b00000000,
      //0x52 Ｒ
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10100000,
      0b10010000,
      0b10001000,
      0b00000000,
      //0x53 Ｓ
      0b01111000,
      0b10000000,
      0b10000000,
      0b01110000,
      0b00001000,
      0b00001000,
      0b11110000,
      0b00000000,
      //0x54 Ｔ
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x55 Ｕ
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x56 Ｖ
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00000000,
      //0x57 Ｗ
      0b10001000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b01010000,
      0b00000000,
      //0x58 Ｘ
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x59 Ｙ
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x5a Ｚ
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0x5b ［
      0b01110000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01110000,
      0b00000000,
      //0x5c ￥
      0b10001000,
      0b01010000,
      0b11111000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x5d ］
      0b01110000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b01110000,
      0b00000000,
      //0x5e ＾
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x5f ＿
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      //0x60 ｀
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x61 ａ
      0b00000000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0x62 ｂ
      0b10000000,
      0b10000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b00000000,
      //0x63 ｃ
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x64 ｄ
      0b00001000,
      0b00001000,
      0b01101000,
      0b10011000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0x65 ｅ
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      0b00000000,
      //0x66 ｆ
      0b00110000,
      0b01001000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x67 ｇ
      0b00000000,
      0b01111000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      0b00000000,
      //0x68 ｈ
      0b10000000,
      0b10000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x69 ｉ
      0b00100000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x6a ｊ
      0b00010000,
      0b00000000,
      0b00110000,
      0b00010000,
      0b00010000,
      0b10010000,
      0b01100000,
      0b00000000,
      //0x6b ｋ
      0b10000000,
      0b10000000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10010000,
      0b00000000,
      //0x6c ｌ
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x6d ｍ
      0b00000000,
      0b00000000,
      0b11010000,
      0b10101000,
      0b10101000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x6e ｎ
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x6f ｏ
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x70 ｐ
      0b00000000,
      0b00000000,
      0b11110000,
      0b10001000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x71 ｑ
      0b00000000,
      0b00000000,
      0b01101000,
      0b10011000,
      0b01111000,
      0b00001000,
      0b00001000,
      0b00000000,
      //0x72 ｒ
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x73 ｓ
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b01110000,
      0b00001000,
      0b11110000,
      0b00000000,
      //0x74 ｔ
      0b01000000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01001000,
      0b00110000,
      0b00000000,
      //0x75 ｕ
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      0b00000000,
      //0x76 ｖ
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00000000,
      //0x77 ｗ
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b01010000,
      0b00000000,
      //0x78 ｘ
      0b00000000,
      0b00000000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      //0x79 ｙ
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      0b00000000,
      //0x7a ｚ
      0b00000000,
      0b00000000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b00000000,
      //0x7b ｛
      0b00010000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00100000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0x7c ｜
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x7d ｝
      0b01000000,
      0b00100000,
      0b00100000,
      0b00010000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x7e ￣
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x7f 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x80 ＼
      0b00000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b00000000,
      0b00000000,
      //0x81 ～
      0b01101000,
      0b10010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x82 ￤
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x83 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x84 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x85 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x86 を
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x87 ぁ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x88 ぃ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x89 ぅ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8a ぇ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8b ぉ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8c ゃ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8d ゅ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8e ょ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8f っ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x90 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x91 あ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x92 い
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x93 う
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x94 え
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x95 お
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x96 か
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x97 き
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x98 く
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x99 け
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9a こ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9b さ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9c し
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9d す
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9e せ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9f そ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa0 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa1 。
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11100000,
      0b10100000,
      0b11100000,
      0b00000000,
      //0xa2 「
      0b00111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa3 」
      0b00000000,
      0b00000000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b11100000,
      0b00000000,
      //0xa4 、
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0xa5 ・
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa6 ヲ
      0b00000000,
      0b11111000,
      0b00001000,
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xa7 ァ
      0b00000000,
      0b00000000,
      0b11111000,
      0b00001000,
      0b00110000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xa8 ィ
      0b00000000,
      0b00000000,
      0b00010000,
      0b00100000,
      0b01100000,
      0b10100000,
      0b00100000,
      0b00000000,
      //0xa9 ゥ
      0b00000000,
      0b00000000,
      0b00100000,
      0b11111000,
      0b10001000,
      0b00001000,
      0b00110000,
      0b00000000,
      //0xaa ェ
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00000000,
      //0xab ォ
      0b00000000,
      0b00000000,
      0b00010000,
      0b11111000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b00000000,
      //0xac ャ
      0b00000000,
      0b00000000,
      0b01000000,
      0b11111000,
      0b01001000,
      0b01010000,
      0b01000000,
      0b00000000,
      //0xad ュ
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b00010000,
      0b00010000,
      0b11111000,
      0b00000000,
      //0xae ョ
      0b00000000,
      0b00000000,
      0b11110000,
      0b00010000,
      0b11110000,
      0b00010000,
      0b11110000,
      0b00000000,
      //0xaf ッ
      0b00000000,
      0b00000000,
      0b00000000,
      0b10101000,
      0b10101000,
      0b00001000,
      0b00110000,
      0b00000000,
      //0xb0 ー
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb1 ア
      0b11111000,
      0b00001000,
      0b00101000,
      0b00110000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xb2 イ
      0b00001000,
      0b00010000,
      0b00100000,
      0b01100000,
      0b10100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0xb3 ウ
      0b00100000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xb4 エ
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00000000,
      //0xb5 オ
      0b00010000,
      0b11111000,
      0b00010000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b00010000,
      0b00000000,
      //0xb6 カ
      0b01000000,
      0b11111000,
      0b01001000,
      0b01001000,
      0b01001000,
      0b01001000,
      0b10010000,
      0b00000000,
      //0xb7 キ
      0b00100000,
      0b11111000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0xb8 ク
      0b00000000,
      0b01111000,
      0b01001000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xb9 ケ
      0b01000000,
      0b01111000,
      0b10010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xba コ
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b00001000,
      0b00001000,
      0b11111000,
      0b00000000,
      //0xbb サ
      0b01010000,
      0b11111000,
      0b01010000,
      0b01010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xbc シ
      0b00000000,
      0b11000000,
      0b00001000,
      0b11001000,
      0b00001000,
      0b00010000,
      0b11100000,
      0b00000000,
      //0xbd ス
      0b00000000,
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      //0xbe セ
      0b01000000,
      0b11111000,
      0b01001000,
      0b01010000,
      0b01000000,
      0b01000000,
      0b00111000,
      0b00000000,
      //0xbf ソ
      0b00000000,
      0b10001000,
      0b10001000,
      0b01001000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xc0 タ
      0b00000000,
      0b01111000,
      0b01001000,
      0b10101000,
      0b00011000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xc1 チ
      0b00010000,
      0b11100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xc2 ツ
      0b00000000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xc3 テ
      0b01110000,
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xc4 ト
      0b01000000,
      0b01000000,
      0b01000000,
      0b01100000,
      0b01010000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0xc5 ナ
      0b00100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0xc6 ニ
      0b00000000,
      0b01110000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      //0xc7 ヌ
      0b00000000,
      0b11111000,
      0b00001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10000000,
      0b00000000,
      //0xc8 ネ
      0b00100000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00000000,
      //0xc9 ノ
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xca ハ
      0b00000000,
      0b00100000,
      0b00010000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0xcb ヒ
      0b10000000,
      0b10000000,
      0b11111000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b01111000,
      0b00000000,
      //0xcc フ
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xcd ヘ
      0b00000000,
      0b01000000,
      0b10100000,
      0b00010000,
      0b00001000,
      0b00001000,
      0b00000000,
      0b00000000,
      //0xce ホ
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b10101000,
      0b10101000,
      0b00100000,
      0b00000000,
      //0xcf マ
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b01010000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0xd0 ミ
      0b00000000,
      0b01110000,
      0b00000000,
      0b01110000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b00000000,
      //0xd1 ム
      0b00000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b10001000,
      0b11111000,
      0b00001000,
      0b00000000,
      //0xd2 メ
      0b00000000,
      0b00001000,
      0b00001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10000000,
      0b00000000,
      //0xd3 モ
      0b00000000,
      0b11111000,
      0b01000000,
      0b11111000,
      0b01000000,
      0b01000000,
      0b00111000,
      0b00000000,
      //0xd4 ヤ
      0b01000000,
      0b01000000,
      0b11111000,
      0b01001000,
      0b01010000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0xd5 ユ
      0b00000000,
      0b01110000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b11111000,
      0b00000000,
      //0xd6 ヨ
      0b00000000,
      0b11111000,
      0b00001000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b11111000,
      0b00000000,
      //0xd7 ラ
      0b01110000,
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xd8 リ
      0b10010000,
      0b10010000,
      0b10010000,
      0b10010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xd9 ル
      0b00000000,
      0b00100000,
      0b10100000,
      0b10100000,
      0b10101000,
      0b10101000,
      0b10110000,
      0b00000000,
      //0xda レ
      0b00000000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b00000000,
      //0xdb ロ
      0b00000000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b00000000,
      //0xdc ワ
      0b00000000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xdd ン
      0b00000000,
      0b11000000,
      0b00000000,
      0b00001000,
      0b00001000,
      0b00010000,
      0b11100000,
      0b00000000,
      //0xde ゛
      0b00100000,
      0b10010000,
      0b01000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xdf ゜
      0b11100000,
      0b10100000,
      0b11100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe0 た
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe1 ち
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe2 つ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe3 て
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe4 と
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe5 な
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe6 に
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe7 ぬ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe8 ね
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe9 の
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xea は
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xeb ひ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xec ふ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xed へ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xee ほ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xef ま
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf0 み
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf1 む
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf2 め
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf3 も
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf4 や
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf5 ゆ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf6 よ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf7 ら
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf8 り
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xf9 る
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfa れ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfb ろ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfc わ
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfd ん
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xfe 　
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xff 　
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b00000000,
    };
*/
    //  perl ../misc/itob.pl FontPage.java LCD6X8_FONT
    public static final byte[] LCD6X8_FONT = "\0\0\0\0\0\0\0\0`\200h(\370((\0`\200h(\320((\0\340\200\340\250\360((\0\340\200\370\220\360\20\20\0\340\200\320\250\3500\30\0@\240\350\250\260((\0\300\240\340\240\340 8\0\300\240\330\240\320\b0\0\240\240\370\260\260\20\20\0\200\200\270\240\370  \0\240\240\270\260P\20\20\0\340\200\370\240\270  \0`\200\260\250p((\0`\200P(\350(\20\0`\200H(\310\b\b\0\300\240\270\240\360 8\0\300\240\250\250\310\b\b\0\300\240\270\250\320 8\0\300\240\270\250\330\b8\0\300\240\250\250\330\b\b\0\300\240\240\250\260((\0`\200p(\350((\0\340\200\360\250\360(0\0`\200\260\250h((\0\340\200\350\270\370((\0`\200p(\360(0\0\340\200\330\240\340 \30\0\0 \20\370\20 \0\0\0 @\370@ \0\0 p\250    \0    \250p \0\0\0\0\0\0\0\0\0    \0\0 \0PPP\0\0\0\0\0PP\370P\370PP\0 x\240p(\360 \0\300\310\20 @\230\30\0`\220\240@\250\220h\0` @\0\0\0\0\0\20 @@@ \20\0@ \20\20\20 @\0\0 \250p\250 \0\0\0  \370  \0\0\0\0\0\0` @\0\0\0\0\370\0\0\0\0\0\0\0\0\0``\0\0\b\20 @\200\0\0p\210\230\250\310\210p\0 `    p\0p\210\b\20 @\370\0\370\20 \20\b\210p\0\0200P\220\370\20\20\0\370\200\360\b\b\210p\0000@\200\360\210\210p\0\370\b\20 @@@\0p\210\210p\210\210p\0p\210\210x\b\20`\0\0``\0``\0\0\0``\0` @\0\20 @\200@ \20\0\0\0\370\0\370\0\0\0@ \20\b\20 @\0p\210\b\20 \0 \0p\210\bh\250\250p\0p\210\210\210\370\210\210\0\360\210\210\360\210\210\360\0p\210\200\200\200\210p\0\340\220\210\210\210\220\340\0\370\200\200\360\200\200\370\0\370\200\200\360\200\200\200\0p\210\200\270\210\210x\0\210\210\210\370\210\210\210\0p     p\08\20\20\20\20\220`\0\210\220\240\300\240\220\210\0\200\200\200\200\200\200\370\0\210\330\250\250\210\210\210\0\210\210\310\250\230\210\210\0p\210\210\210\210\210p\0\360\210\210\360\200\200\200\0p\210\210\210\250\220h\0\360\210\210\360\240\220\210\0x\200\200p\b\b\360\0\370      \0\210\210\210\210\210\210p\0\210\210\210\210\210P \0\210\210\210\250\250\250P\0\210\210P P\210\210\0\210\210\210P   \0\370\b\20 @\200\370\0p@@@@@p\0\210P\370 \370  \0p\20\20\20\20\20p\0 P\210\0\0\0\0\0\0\0\0\0\0\0\370\0@ \20\0\0\0\0\0\0\0p\bx\210x\0\200\200\260\310\210\210\360\0\0\0p\200\200\210p\0\b\bh\230\210\210x\0\0\0p\210\370\200p\0000H@\340@@@\0\0x\210\210x\bp\0\200\200\260\310\210\210\210\0 \0`   p\0\20\0000\20\20\220`\0\200\200\220\240\300\240\220\0`     p\0\0\0\320\250\250\210\210\0\0\0\260\310\210\210\210\0\0\0p\210\210\210p\0\0\0\360\210\360\200\200\0\0\0h\230x\b\b\0\0\0\260\310\200\200\200\0\0\0p\200p\b\360\0@@\340@@H0\0\0\0\210\210\210\230h\0\0\0\210\210\210P \0\0\0\210\210\250\250P\0\0\0\210P P\210\0\0\0\210\210x\bp\0\0\0\370\20 @\370\0\20  @  \20\0       \0@  \20  @\0\370\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\200@ \20\b\0\0h\220\0\0\0\0\0\0   \0   \0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\340\240\340\08   \0\0\0\0\0\0\0   \340\0\0\0\0\0\200@ \0\0\0\0``\0\0\0\0\370\b\370\b\20 \0\0\0\370\b0 @\0\0\0\20 `\240 \0\0\0 \370\210\b0\0\0\0\0\370  \370\0\0\0\20\3700P\220\0\0\0@\370HP@\0\0\0\0p\20\20\370\0\0\0\360\20\360\20\360\0\0\0\0\250\250\b0\0\0\0\0\370\0\0\0\0\370\b(0  @\0\b\20 `\240  \0 \370\210\210\b\20 \0\0\370    \370\0\20\370\0200P\220\20\0@\370HHHH\220\0 \370 \370   \0\0xH\210\b\20`\0@x\220\20\20\20 \0\0\370\b\b\b\b\370\0P\370PP\20 @\0\0\300\b\310\b\20\340\0\0\370\b\20 P\210\0@\370HP@@8\0\0\210\210H\b\20`\0\0xH\250\30\20`\0\20\340 \370  @\0\0\250\250\250\b\20 \0p\0\370   @\0@@@`P@@\0  \370  @\200\0\0p\0\0\0\0\370\0\0\370\bP P\200\0 \370\20 p\250 \0\20\20\20\20\20 @\0\0 \20\210\210\210\210\0\200\200\370\200\200\200x\0\0\370\b\b\b\20`\0\0@\240\20\b\b\0\0 \370  \250\250 \0\0\370\b\bP \20\0\0p\0p\0p\b\0\0 @\200\210\370\b\0\0\b\bP P\200\0\0\370@\370@@8\0@@\370HP@@\0\0p\20\20\20\20\370\0\0\370\b\370\b\b\370\0p\0\370\b\b\20 \0\220\220\220\220\20 @\0\0 \240\240\250\250\260\0\0\200\200\210\220\240\300\0\0\370\210\210\210\210\370\0\0\370\210\210\b\20 \0\0\300\0\b\b\20\340\0 \220@\0\0\0\0\0\340\240\340\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\370\370\370\370\370\370\370\0".getBytes (XEiJ.ISO_8859_1);

  }  //class FontPage.Lcd



  //class FontPage.Smk
  //  白窓君(HD44780)のANK。16桁×32行。前半256文字が日本仕様、後半256文字が欧州仕様
  public static final class Smk extends FontPage {

    //半角フォントから作る文字
    public static final char[] HALF_BASE = (
      //日本仕様
      //123456789abcdef
      "                " +  //00
      "                " +  //01
      " !\"#$%&'()*+,-./" +  //02
      "0123456789:;<=>?" +  //03
      "@ABCDEFGHIJKLMNO" +  //04
      "PQRSTUVWXYZ[¥]^_" +  //05
      "`abcdefghijklmno" +  //06
      "pqrstuvwxyz{|}  " +  //07
      "                " +  //08
      "                " +  //09
      " ｡｢｣､･ｦｧｨｩｪｫｬｭｮｯ" +  //0a
      "ｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ" +  //0b
      "ﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ" +  //0c
      "ﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ" +  //0d
      "αäβεμδρg  j̽¢£ñö" +  //0e
      "pqθ ΩüΣπ y   ÷ █" +  //0f
      //欧州仕様
      //123456789abcdef
      "                " +  //10
      "▶◀     ↵    ≤≥  " +  //11
      " !\"#$%&'()*+,-./" +  //12
      "0123456789:;<=>?" +  //13
      "@ABCDEFGHIJKLMNO" +  //14
      "PQRSTUVWXYZ[ ]^_" +  //15
      "`abcdefghijklmno" +  //16
      "pqrstuvwxyz{|}~⌂" +  //17
      "БДЖЗИЙЛПУЦЧШЩЪЫЭ" +  //18
      "α ΓπΣσ♬τ\u237eΘΩδ ♥ε " +  //19
      " ¡¢£¤¥¦§ƒ©ª«ЮЯ® " +  //1a
      "°±²³₧µ¶·ω¹º»¼½¾¿" +  //1b
      "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏ" +  //1c
      "ÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞß" +  //1d
      "àáâãäåæçèéêëìíîï" +  //1e
      "ðñòóôõö÷øùúûüýþÿ"    //1f
      ).toCharArray ();

    //全角フォントから作る文字
    public static final char[] FULL_BASE = (
      //日本仕様
      //0 1 2 3 4 5 6 7 8 9 a b c d e f
      "　　　　　　　　　　　　　　　　" +  //00
      "　　　　　　　　　　　　　　　　" +  //01
      "　　　　　　　　　　　　　　　　" +  //02
      "　　　　　　　　　　　　　　　　" +  //03
      "　　　　　　　　　　　　　　　　" +  //04
      "　　　　　　　　　　　　　　　　" +  //05
      "　　　　　　　　　　　　　　　　" +  //06
      "　　　　　　　　　　　　　　→←" +  //07
      "　　　　　　　　　　　　　　　　" +  //08
      "　　　　　　　　　　　　　　　　" +  //09
      "　　　　　　　　　　　　　　　　" +  //0a
      "　　　　　　　　　　　　　　　　" +  //0b
      "　　　　　　　　　　　　　　　　" +  //0c
      "　　　　　　　　　　　　　　　　" +  //0d
      "　　　　　　　　√　　　　　　　" +  //0e
      "　　　∞　　　　　　千万円　　　" +  //0f
      //欧州仕様
      //0 1 2 3 4 5 6 7 8 9 a b c d e f
      "　　　　　　　　　　　　　　　　" +  //10
      "　　“”　　●　↑↓→←　　▲▼" +  //11
      "　　　　　　　　　　　　　　　　" +  //12
      "　　　　　　　　　　　　　　　　" +  //13
      "　　　　　　　　　　　　　　　　" +  //14
      "　　　　　　　　　　　　＼　　　" +  //15
      "　　　　　　　　　　　　　　　　" +  //16
      "　　　　　　　　　　　　　　　　" +  //17
      "　　　　　　　　　　　　　　　　" +  //18
      "　♪　　　　　　　　　　∞　　∩" +  //19
      "　　　　　　　　　　　　　　　‘" +  //1a
      "　　　　　　　　　　　　　　　　" +  //1b
      "　　　　　　　　　　　　　　　　" +  //1c
      "　　　　　　　　　　　　　　　　" +  //1d
      "　　　　　　　　　　　　　　　　" +  //1e
      "　　　　　　　　　　　　　　　　"    //1f
      ).toCharArray ();

    public Smk (int characterWidth, int characterHeight, String nameEn, String nameJa, String dataName, String imageName) {
      super (characterWidth, characterHeight, nameEn, nameJa, dataName, imageName,
             16, 32, 32,
             null, 0,
             new char[][] { FULL_BASE, HALF_BASE });
      fnpLoadFontDataArray (SMK6X8_FONT, 0, SMK6X8_FONT.length);
    }

    //  白窓君のフォント
    //  CGROMに合わせて上位に寄せてある。取り出すとき右に3ビットシフトする
/*
    public static final byte[] SMK6X8_FONT = {

      //日本標準フォント(日本仕様)
      //0x00
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x01
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x02
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x03
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x04
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x05
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x06
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x07
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x08
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x09
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0a
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0b
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0c
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0d
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0f
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x10
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x11
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x12
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x13
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x14
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x15
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x16
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x17
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x18
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x19
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1a
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1b
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1c
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1d
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x1f
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x20
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x21 !
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00100000,
      0b00000000,
      //0x22 "
      0b01010000,
      0b01010000,
      0b01010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x23 #
      0b01010000,
      0b01010000,
      0b11111000,
      0b01010000,
      0b11111000,
      0b01010000,
      0b01010000,
      0b00000000,
      //0x24 $
      0b00100000,
      0b01111000,
      0b10100000,
      0b01110000,
      0b00101000,
      0b11110000,
      0b00100000,
      0b00000000,
      //0x25 %
      0b11000000,
      0b11001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10011000,
      0b00011000,
      0b00000000,
      //0x26 &
      0b01100000,
      0b10010000,
      0b10100000,
      0b01000000,
      0b10101000,
      0b10010000,
      0b01101000,
      0b00000000,
      //0x27 '
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x28 (
      0b00010000,
      0b00100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0x29 )
      0b01000000,
      0b00100000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x2a *
      0b00000000,
      0b00100000,
      0b10101000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x2b +
      0b00000000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x2c ,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x2d -
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x2e .
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      //0x2f /
      0b00000000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      0b00000000,
      //0x30 0
      0b01110000,
      0b10001000,
      0b10011000,
      0b10101000,
      0b11001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x31 1
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x32 2
      0b01110000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b00000000,
      //0x33 3
      0b11111000,
      0b00010000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x34 4
      0b00010000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b11111000,
      0b00010000,
      0b00010000,
      0b00000000,
      //0x35 5
      0b11111000,
      0b10000000,
      0b11110000,
      0b00001000,
      0b00001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x36 6
      0b00110000,
      0b01000000,
      0b10000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x37 7
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x38 8
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x39 9
      0b01110000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0x3a :
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b00000000,
      //0x3b ;
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x3c <
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0x3d =
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x3e >
      0b01000000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x3f ?
      0b01110000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b00000000,
      //0x40 @
      0b01110000,
      0b10001000,
      0b00001000,
      0b01101000,
      0b10101000,
      0b10101000,
      0b01110000,
      0b00000000,
      //0x41 A
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x42 B
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b00000000,
      //0x43 C
      0b01110000,
      0b10001000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x44 D
      0b11100000,
      0b10010000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10010000,
      0b11100000,
      0b00000000,
      //0x45 E
      0b11111000,
      0b10000000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0x46 F
      0b11111000,
      0b10000000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x47 G
      0b01110000,
      0b10001000,
      0b10000000,
      0b10111000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0x48 H
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x49 I
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x4a J
      0b00111000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b10010000,
      0b01100000,
      0b00000000,
      //0x4b K
      0b10001000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10010000,
      0b10001000,
      0b00000000,
      //0x4c L
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0x4d M
      0b10001000,
      0b11011000,
      0b10101000,
      0b10101000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x4e N
      0b10001000,
      0b10001000,
      0b11001000,
      0b10101000,
      0b10011000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x4f O
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x50 P
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x51 Q
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10010000,
      0b01101000,
      0b00000000,
      //0x52 R
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10100000,
      0b10010000,
      0b10001000,
      0b00000000,
      //0x53 S
      0b01111000,
      0b10000000,
      0b10000000,
      0b01110000,
      0b00001000,
      0b00001000,
      0b11110000,
      0b00000000,
      //0x54 T
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x55 U
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x56 V
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00000000,
      //0x57 W
      0b10001000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b01010000,
      0b00000000,
      //0x58 X
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x59 Y
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x5a Z
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0x5b [
      0b01110000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01110000,
      0b00000000,
      //0x5c \\
      0b10001000,
      0b01010000,
      0b11111000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x5d ]
      0b01110000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b01110000,
      0b00000000,
      //0x5e ^
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x5f _
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      //0x60 `
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x61 a
      0b00000000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0x62 b
      0b10000000,
      0b10000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b00000000,
      //0x63 c
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x64 d
      0b00001000,
      0b00001000,
      0b01101000,
      0b10011000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0x65 e
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      0b00000000,
      //0x66 f
      0b00110000,
      0b01001000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0x67 g
      0b00000000,
      0b01111000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      0b00000000,
      //0x68 h
      0b10000000,
      0b10000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x69 i
      0b00100000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x6a j
      0b00010000,
      0b00000000,
      0b00110000,
      0b00010000,
      0b00010000,
      0b10010000,
      0b01100000,
      0b00000000,
      //0x6b k
      0b10000000,
      0b10000000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10010000,
      0b00000000,
      //0x6c l
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b00000000,
      //0x6d m
      0b00000000,
      0b00000000,
      0b11010000,
      0b10101000,
      0b10101000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x6e n
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0x6f o
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0x70 p
      0b00000000,
      0b00000000,
      0b11110000,
      0b10001000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x71 q
      0b00000000,
      0b00000000,
      0b01101000,
      0b10011000,
      0b01111000,
      0b00001000,
      0b00001000,
      0b00000000,
      //0x72 r
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b00000000,
      //0x73 s
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b01110000,
      0b00001000,
      0b11110000,
      0b00000000,
      //0x74 t
      0b01000000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01001000,
      0b00110000,
      0b00000000,
      //0x75 u
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      0b00000000,
      //0x76 v
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00000000,
      //0x77 w
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b01010000,
      0b00000000,
      //0x78 x
      0b00000000,
      0b00000000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      //0x79 y
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      0b00000000,
      //0x7a z
      0b00000000,
      0b00000000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b00000000,
      //0x7b {
      0b00010000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00100000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0x7c |
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x7d }
      0b01000000,
      0b00100000,
      0b00100000,
      0b00010000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0x7e →
      0b00000000,
      0b00100000,
      0b00010000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x7f ←
      0b00000000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b01000000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0x80
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x81
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x82
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x83
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x84
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x85
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x86
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x87
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x88
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x89
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8a
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8b
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8c
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8d
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x8f
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x90
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x91
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x92
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x93
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x94
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x95
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x96
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x97
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x98
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x99
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9a
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9b
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9c
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9d
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x9f
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa0
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa1 。
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11100000,
      0b10100000,
      0b11100000,
      0b00000000,
      //0xa2 「
      0b00111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa3 」
      0b00000000,
      0b00000000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b11100000,
      0b00000000,
      //0xa4 、
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0xa5 ・
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xa6 ヲ
      0b00000000,
      0b11111000,
      0b00001000,
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xa7 ァ
      0b00000000,
      0b00000000,
      0b11111000,
      0b00001000,
      0b00110000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xa8 ィ
      0b00000000,
      0b00000000,
      0b00010000,
      0b00100000,
      0b01100000,
      0b10100000,
      0b00100000,
      0b00000000,
      //0xa9 ゥ
      0b00000000,
      0b00000000,
      0b00100000,
      0b11111000,
      0b10001000,
      0b00001000,
      0b00110000,
      0b00000000,
      //0xaa ェ
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00000000,
      //0xab ォ
      0b00000000,
      0b00000000,
      0b00010000,
      0b11111000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b00000000,
      //0xac ャ
      0b00000000,
      0b00000000,
      0b01000000,
      0b11111000,
      0b01001000,
      0b01010000,
      0b01000000,
      0b00000000,
      //0xad ュ
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b00010000,
      0b00010000,
      0b11111000,
      0b00000000,
      //0xae ョ
      0b00000000,
      0b00000000,
      0b11110000,
      0b00010000,
      0b11110000,
      0b00010000,
      0b11110000,
      0b00000000,
      //0xaf ッ
      0b00000000,
      0b00000000,
      0b00000000,
      0b10101000,
      0b10101000,
      0b00001000,
      0b00110000,
      0b00000000,
      //0xb0 ー
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb1 ア
      0b11111000,
      0b00001000,
      0b00101000,
      0b00110000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xb2 イ
      0b00001000,
      0b00010000,
      0b00100000,
      0b01100000,
      0b10100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0xb3 ウ
      0b00100000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xb4 エ
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00000000,
      //0xb5 オ
      0b00010000,
      0b11111000,
      0b00010000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b00010000,
      0b00000000,
      //0xb6 カ
      0b01000000,
      0b11111000,
      0b01001000,
      0b01001000,
      0b01001000,
      0b01001000,
      0b10010000,
      0b00000000,
      //0xb7 キ
      0b00100000,
      0b11111000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0xb8 ク
      0b00000000,
      0b01111000,
      0b01001000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xb9 ケ
      0b01000000,
      0b01111000,
      0b10010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xba コ
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b00001000,
      0b00001000,
      0b11111000,
      0b00000000,
      //0xbb サ
      0b01010000,
      0b11111000,
      0b01010000,
      0b01010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xbc シ
      0b00000000,
      0b11000000,
      0b00001000,
      0b11001000,
      0b00001000,
      0b00010000,
      0b11100000,
      0b00000000,
      //0xbd ス
      0b00000000,
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      //0xbe セ
      0b01000000,
      0b11111000,
      0b01001000,
      0b01010000,
      0b01000000,
      0b01000000,
      0b00111000,
      0b00000000,
      //0xbf ソ
      0b00000000,
      0b10001000,
      0b10001000,
      0b01001000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xc0 タ
      0b00000000,
      0b01111000,
      0b01001000,
      0b10101000,
      0b00011000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xc1 チ
      0b00010000,
      0b11100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xc2 ツ
      0b00000000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xc3 テ
      0b01110000,
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xc4 ト
      0b01000000,
      0b01000000,
      0b01000000,
      0b01100000,
      0b01010000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0xc5 ナ
      0b00100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0xc6 ニ
      0b00000000,
      0b01110000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      //0xc7 ヌ
      0b00000000,
      0b11111000,
      0b00001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10000000,
      0b00000000,
      //0xc8 ネ
      0b00100000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00000000,
      //0xc9 ノ
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xca ハ
      0b00000000,
      0b00100000,
      0b00010000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0xcb ヒ
      0b10000000,
      0b10000000,
      0b11111000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b01111000,
      0b00000000,
      //0xcc フ
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b00001000,
      0b00010000,
      0b01100000,
      0b00000000,
      //0xcd ヘ
      0b00000000,
      0b01000000,
      0b10100000,
      0b00010000,
      0b00001000,
      0b00001000,
      0b00000000,
      0b00000000,
      //0xce ホ
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b10101000,
      0b10101000,
      0b00100000,
      0b00000000,
      //0xcf マ
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b01010000,
      0b00100000,
      0b00010000,
      0b00000000,
      //0xd0 ミ
      0b00000000,
      0b01110000,
      0b00000000,
      0b01110000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b00000000,
      //0xd1 ム
      0b00000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b10001000,
      0b11111000,
      0b00001000,
      0b00000000,
      //0xd2 メ
      0b00000000,
      0b00001000,
      0b00001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10000000,
      0b00000000,
      //0xd3 モ
      0b00000000,
      0b11111000,
      0b01000000,
      0b11111000,
      0b01000000,
      0b01000000,
      0b00111000,
      0b00000000,
      //0xd4 ヤ
      0b01000000,
      0b01000000,
      0b11111000,
      0b01001000,
      0b01010000,
      0b01000000,
      0b01000000,
      0b00000000,
      //0xd5 ユ
      0b00000000,
      0b01110000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b11111000,
      0b00000000,
      //0xd6 ヨ
      0b00000000,
      0b11111000,
      0b00001000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b11111000,
      0b00000000,
      //0xd7 ラ
      0b01110000,
      0b00000000,
      0b11111000,
      0b00001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xd8 リ
      0b10010000,
      0b10010000,
      0b10010000,
      0b10010000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      //0xd9 ル
      0b00000000,
      0b00100000,
      0b10100000,
      0b10100000,
      0b10101000,
      0b10101000,
      0b10110000,
      0b00000000,
      //0xda レ
      0b00000000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b00000000,
      //0xdb ロ
      0b00000000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b00000000,
      //0xdc ワ
      0b00000000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0xdd ン
      0b00000000,
      0b11000000,
      0b00000000,
      0b00001000,
      0b00001000,
      0b00010000,
      0b11100000,
      0b00000000,
      //0xde ゛
      0b00100000,
      0b10010000,
      0b01000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xdf ゜
      0b11100000,
      0b10100000,
      0b11100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xe0 α
      0b00000000,
      0b00000000,
      0b01001000,
      0b10101000,
      0b10010000,
      0b10010000,
      0b01101000,
      0b00000000,
      //0xe1 ä
      0b01010000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      0b00000000,
      //0xe2 β
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11110000,
      0b10001000,
      0b11110000,
      0b10000000,
      //0xe3 ε
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b01100000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0xe4 μ
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b11101000,
      0b10000000,
      //0xe5 δ
      0b00000000,
      0b00000000,
      0b01111000,
      0b10100000,
      0b10010000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0xe6 ρ
      0b00000000,
      0b00000000,
      0b00110000,
      0b01001000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10000000,
      //0xe7 g
      0b00000000,
      0b00000000,
      0b01111000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      //0xe8 √
      0b00000000,
      0b00000000,
      0b00111000,
      0b00100000,
      0b00100000,
      0b10100000,
      0b01000000,
      0b00000000,
      //0xe9
      0b00000000,
      0b00010000,
      0b11010000,
      0b00010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xea j
      0b00010000,
      0b00000000,
      0b00110000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      //0xeb
      0b00000000,
      0b10100000,
      0b01000000,
      0b10100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xec ￠
      0b00000000,
      0b00100000,
      0b01110000,
      0b10100000,
      0b10101000,
      0b01110000,
      0b00100000,
      0b00000000,
      //0xed
      0b01000000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01111000,
      0b00000000,
      //0xee
      0b01110000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0xef
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0xf0 p
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10000000,
      //0xf1 q
      0b00000000,
      0b01100000,
      0b10011000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b00001000,
      //0xf2 θ
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      //0xf3 ∞
      0b00000000,
      0b00000000,
      0b00000000,
      0b01011000,
      0b10101000,
      0b11010000,
      0b00000000,
      0b00000000,
      //0xf4 Ω
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b11011000,
      0b00000000,
      //0xf5
      0b01010000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      //0xf6 Σ
      0b11111000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b11111000,
      0b00000000,
      //0xf7 π
      0b00000000,
      0b00000000,
      0b11111000,
      0b01010000,
      0b01010000,
      0b01010000,
      0b10011000,
      0b00000000,
      //0xf8
      0b11111000,
      0b00000000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      //0xf9 y
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      //0xfa 千
      0b00000000,
      0b00001000,
      0b11110000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0xfb 万
      0b00000000,
      0b00000000,
      0b11111000,
      0b01000000,
      0b01111000,
      0b01001000,
      0b10001000,
      0b00000000,
      //0xfc 円
      0b00000000,
      0b00000000,
      0b11111000,
      0b10101000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b00000000,
      //0xfd ÷
      0b00000000,
      0b00100000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00100000,
      0b00000000,
      0b00000000,
      //0xfe
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xff
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b11111000,

      //欧州標準フォント(欧州仕様)
      //  日本標準フォントと異なり、下に寄っている
      //0x00
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x01
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x02
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x03
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x04
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x05
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x06
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x07
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x08
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x09
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0a
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0b
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0c
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0d
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x0f
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x10
      0b00000000,
      0b01000000,
      0b01100000,
      0b01110000,
      0b01111000,
      0b01110000,
      0b01100000,
      0b01000000,
      //0x11
      0b00000000,
      0b00010000,
      0b00110000,
      0b01110000,
      0b11110000,
      0b01110000,
      0b00110000,
      0b00010000,
      //0x12
      0b00000000,
      0b01001000,
      0b10010000,
      0b11011000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x13
      0b00000000,
      0b11011000,
      0b01001000,
      0b10010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x14
      0b00000000,
      0b00100000,
      0b01110000,
      0b11111000,
      0b00000000,
      0b00100000,
      0b01110000,
      0b11111000,
      //0x15
      0b00000000,
      0b11111000,
      0b01110000,
      0b00100000,
      0b00000000,
      0b11111000,
      0b01110000,
      0b00100000,
      //0x16
      0b00000000,
      0b00000000,
      0b01110000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b01110000,
      0b00000000,
      //0x17
      0b00000000,
      0b00001000,
      0b00001000,
      0b00101000,
      0b01001000,
      0b11111000,
      0b01000000,
      0b00100000,
      //0x18
      0b00000000,
      0b00100000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0x19
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b10101000,
      0b01110000,
      0b00100000,
      //0x1a
      0b00000000,
      0b00000000,
      0b00100000,
      0b00010000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b00000000,
      //0x1b
      0b00000000,
      0b00000000,
      0b00100000,
      0b01000000,
      0b11111000,
      0b01000000,
      0b00100000,
      0b00000000,
      //0x1c
      0b00000000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      0b11111000,
      //0x1d
      0b00000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b00000000,
      0b11111000,
      //0x1e
      0b00000000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b01110000,
      0b01110000,
      0b11111000,
      0b00000000,
      //0x1f
      0b00000000,
      0b00000000,
      0b11111000,
      0b01110000,
      0b01110000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x20
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x21
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00100000,
      //0x22
      0b00000000,
      0b01010000,
      0b01010000,
      0b01010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x23
      0b00000000,
      0b01010000,
      0b01010000,
      0b11111000,
      0b01010000,
      0b11111000,
      0b01010000,
      0b01010000,
      //0x24
      0b00000000,
      0b00100000,
      0b01111000,
      0b10100000,
      0b01110000,
      0b00101000,
      0b11110000,
      0b00100000,
      //0x25
      0b00000000,
      0b11000000,
      0b11001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10011000,
      0b00011000,
      //0x26
      0b00000000,
      0b01100000,
      0b10010000,
      0b10100000,
      0b01000000,
      0b10101000,
      0b10010000,
      0b01101000,
      //0x27
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x28
      0b00000000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b00100000,
      0b00010000,
      //0x29
      0b00000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00100000,
      0b01000000,
      //0x2a
      0b00000000,
      0b00000000,
      0b00100000,
      0b10101000,
      0b01110000,
      0b10101000,
      0b00100000,
      0b00000000,
      //0x2b
      0b00000000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      //0x2c
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      //0x2d
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x2e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      //0x2f
      0b00000000,
      0b00000000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b00000000,
      //0x30
      0b00000000,
      0b01110000,
      0b10001000,
      0b10011000,
      0b10101000,
      0b11001000,
      0b10001000,
      0b01110000,
      //0x31
      0b00000000,
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0x32
      0b00000000,
      0b01110000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b11111000,
      //0x33
      0b00000000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b10001000,
      0b01110000,
      //0x34
      0b00000000,
      0b00010000,
      0b00110000,
      0b01010000,
      0b10010000,
      0b11111000,
      0b00010000,
      0b00010000,
      //0x35
      0b00000000,
      0b11111000,
      0b10000000,
      0b11110000,
      0b00001000,
      0b00001000,
      0b10001000,
      0b01110000,
      //0x36
      0b00000000,
      0b00110000,
      0b01000000,
      0b10000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x37
      0b00000000,
      0b11111000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0x38
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x39
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b00010000,
      0b01100000,
      //0x3a
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      //0x3b
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b01100000,
      0b00100000,
      0b01000000,
      //0x3c
      0b00000000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00010000,
      //0x3d
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00000000,
      //0x3e
      0b00000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      //0x3f
      0b00000000,
      0b01110000,
      0b10001000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b00000000,
      0b00100000,
      //0x40
      0b00000000,
      0b01110000,
      0b10001000,
      0b00001000,
      0b01101000,
      0b10101000,
      0b10101000,
      0b01110000,
      //0x41
      0b00000000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0x42
      0b00000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      //0x43
      0b00000000,
      0b01110000,
      0b10001000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      //0x44
      0b00000000,
      0b11100000,
      0b10010000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10010000,
      0b11100000,
      //0x45
      0b00000000,
      0b11111000,
      0b10000000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b11111000,
      //0x46
      0b00000000,
      0b11111000,
      0b10000000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b10000000,
      //0x47
      0b00000000,
      0b01110000,
      0b10001000,
      0b10000000,
      0b10111000,
      0b10001000,
      0b10001000,
      0b01111000,
      //0x48
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0x49
      0b00000000,
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0x4a
      0b00000000,
      0b00111000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b10010000,
      0b01100000,
      //0x4b
      0b00000000,
      0b10001000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10010000,
      0b10001000,
      //0x4c
      0b00000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b11111000,
      //0x4d
      0b00000000,
      0b10001000,
      0b11011000,
      0b10101000,
      0b10101000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0x4e
      0b00000000,
      0b10001000,
      0b10001000,
      0b11001000,
      0b10101000,
      0b10011000,
      0b10001000,
      0b10001000,
      //0x4f
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x50
      0b00000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10000000,
      0b10000000,
      0b10000000,
      //0x51
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10010000,
      0b01101000,
      //0x52
      0b00000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      0b10100000,
      0b10010000,
      0b10001000,
      //0x53
      0b00000000,
      0b01110000,
      0b10001000,
      0b10000000,
      0b01110000,
      0b00001000,
      0b10001000,
      0b01110000,
      //0x54
      0b00000000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0x55
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x56
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      //0x57
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b01010000,
      //0x58
      0b00000000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b10001000,
      //0x59
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0x5a
      0b00000000,
      0b11111000,
      0b00001000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b11111000,
      //0x5b
      0b00000000,
      0b01110000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01000000,
      0b01110000,
      //0x5c
      0b00000000,
      0b00000000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00001000,
      0b00000000,
      //0x5d
      0b00000000,
      0b01110000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b00010000,
      0b01110000,
      //0x5e
      0b00000000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x5f
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      //0x60
      0b00000000,
      0b01000000,
      0b00100000,
      0b00010000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0x61
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0x62
      0b00000000,
      0b10000000,
      0b10000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b11110000,
      //0x63
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      //0x64
      0b00000000,
      0b00001000,
      0b00001000,
      0b01101000,
      0b10011000,
      0b10001000,
      0b10001000,
      0b01111000,
      //0x65
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      //0x66
      0b00000000,
      0b00110000,
      0b01001000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01000000,
      //0x67
      0b00000000,
      0b00000000,
      0b00000000,
      0b01111000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      //0x68
      0b00000000,
      0b10000000,
      0b10000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0x69
      0b00000000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0x6a
      0b00000000,
      0b00010000,
      0b00000000,
      0b00110000,
      0b00010000,
      0b00010000,
      0b10010000,
      0b01100000,
      //0x6b
      0b00000000,
      0b10000000,
      0b10000000,
      0b10010000,
      0b10100000,
      0b11000000,
      0b10100000,
      0b10010000,
      //0x6c
      0b00000000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0x6d
      0b00000000,
      0b00000000,
      0b00000000,
      0b11010000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b10101000,
      //0x6e
      0b00000000,
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0x6f
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x70
      0b00000000,
      0b00000000,
      0b00000000,
      0b11110000,
      0b10001000,
      0b11110000,
      0b10000000,
      0b10000000,
      //0x71
      0b00000000,
      0b00000000,
      0b00000000,
      0b01101000,
      0b10011000,
      0b01111000,
      0b00001000,
      0b00001000,
      //0x72
      0b00000000,
      0b00000000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10000000,
      0b10000000,
      0b10000000,
      //0x73
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b01110000,
      0b00001000,
      0b11110000,
      //0x74
      0b00000000,
      0b01000000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01000000,
      0b01001000,
      0b00110000,
      //0x75
      0b00000000,
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      //0x76
      0b00000000,
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      //0x77
      0b00000000,
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b01010000,
      //0x78
      0b00000000,
      0b00000000,
      0b00000000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      //0x79
      0b00000000,
      0b00000000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      //0x7a
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b00010000,
      0b00100000,
      0b01000000,
      0b11111000,
      //0x7b
      0b00000000,
      0b00010000,
      0b00100000,
      0b00100000,
      0b01000000,
      0b00100000,
      0b00100000,
      0b00010000,
      //0x7c
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0x7d
      0b00000000,
      0b01000000,
      0b00100000,
      0b00100000,
      0b00010000,
      0b00100000,
      0b00100000,
      0b01000000,
      //0x7e
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01101000,
      0b10010000,
      0b00000000,
      0b00000000,
      //0x7f
      0b00000000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b00000000,
      //0x80
      0b00000000,
      0b11111000,
      0b10001000,
      0b10000000,
      0b11110000,
      0b10001000,
      0b10001000,
      0b11110000,
      //0x81
      0b01111000,
      0b00101000,
      0b00101000,
      0b01001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0x82
      0b00000000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b01110000,
      0b10101000,
      0b10101000,
      0b10101000,
      //0x83
      0b00000000,
      0b11110000,
      0b00001000,
      0b00001000,
      0b00110000,
      0b00001000,
      0b00001000,
      0b11110000,
      //0x84
      0b00000000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b10101000,
      0b11001000,
      0b10001000,
      0b10001000,
      //0x85
      0b01010000,
      0b00100000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b10101000,
      0b11001000,
      0b10001000,
      //0x86
      0b00000000,
      0b01111000,
      0b00101000,
      0b00101000,
      0b00101000,
      0b00101000,
      0b10101000,
      0b01001000,
      //0x87
      0b00000000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0x88
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01000000,
      0b10000000,
      //0x89
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b00001000,
      //0x8a
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b00001000,
      0b00001000,
      //0x8b
      0b00000000,
      0b00000000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b11111000,
      //0x8c
      0b00000000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b10101000,
      0b11111000,
      0b00001000,
      //0x8d
      0b00000000,
      0b11000000,
      0b01000000,
      0b01000000,
      0b01110000,
      0b01001000,
      0b01001000,
      0b01110000,
      //0x8e
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b11001000,
      0b10101000,
      0b10101000,
      0b11001000,
      //0x8f
      0b00000000,
      0b01110000,
      0b10001000,
      0b00101000,
      0b01011000,
      0b00001000,
      0b10001000,
      0b01110000,
      //0x90
      0b00000000,
      0b00000000,
      0b00000000,
      0b01001000,
      0b10101000,
      0b10010000,
      0b10010000,
      0b01101000,
      //0x91
      0b00000000,
      0b00100000,
      0b00110000,
      0b00101000,
      0b00101000,
      0b00100000,
      0b11100000,
      0b11100000,
      //0x92
      0b00000000,
      0b11111000,
      0b10001000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      0b10000000,
      //0x93
      0b00000000,
      0b00000000,
      0b00000000,
      0b11111000,
      0b01010000,
      0b01010000,
      0b01010000,
      0b10011000,
      //0x94
      0b00000000,
      0b11111000,
      0b10000000,
      0b01000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b11111000,
      //0x95
      0b00000000,
      0b00000000,
      0b00000000,
      0b01111000,
      0b10010000,
      0b10010000,
      0b10010000,
      0b01100000,
      //0x96
      0b00110000,
      0b00101000,
      0b00111000,
      0b00101000,
      0b00101000,
      0b11101000,
      0b11011000,
      0b00011000,
      //0x97
      0b00000000,
      0b00000000,
      0b00001000,
      0b01110000,
      0b10100000,
      0b00100000,
      0b00100000,
      0b00010000,
      //0x98
      0b00000000,
      0b00100000,
      0b01110000,
      0b01110000,
      0b01110000,
      0b11111000,
      0b00100000,
      0b00000000,
      //0x99
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x9a
      0b00000000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01010000,
      0b11011000,
      //0x9b
      0b00000000,
      0b00110000,
      0b01001000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0x9c
      0b00000000,
      0b00000000,
      0b00000000,
      0b01011000,
      0b10101000,
      0b11010000,
      0b00000000,
      0b00000000,
      //0x9d
      0b00000000,
      0b00000000,
      0b01010000,
      0b11111000,
      0b11111000,
      0b11111000,
      0b01110000,
      0b00100000,
      //0x9e
      0b00000000,
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b01100000,
      0b10001000,
      0b01110000,
      //0x9f
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0xa0
      0b00000000,
      0b11011000,
      0b11011000,
      0b11011000,
      0b11011000,
      0b11011000,
      0b11011000,
      0b11011000,
      //0xa1
      0b00000000,
      0b00100000,
      0b00000000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0xa2
      0b00000000,
      0b00100000,
      0b01110000,
      0b10100000,
      0b10100000,
      0b10101000,
      0b01110000,
      0b00100000,
      //0xa3
      0b00000000,
      0b00110000,
      0b01000000,
      0b01000000,
      0b11100000,
      0b01000000,
      0b01001000,
      0b10110000,
      //0xa4
      0b00000000,
      0b00000000,
      0b10001000,
      0b01110000,
      0b01010000,
      0b01110000,
      0b10001000,
      0b00000000,
      //0xa5
      0b00000000,
      0b10001000,
      0b01010000,
      0b11111000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      //0xa6
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0xa7
      0b00000000,
      0b00110000,
      0b01001000,
      0b00100000,
      0b01010000,
      0b00100000,
      0b10010000,
      0b01100000,
      //0xa8
      0b00000000,
      0b00010000,
      0b00101000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b10100000,
      0b01000000,
      //0xa9
      0b00000000,
      0b11111000,
      0b10001000,
      0b10101000,
      0b10111000,
      0b10101000,
      0b10001000,
      0b11111000,
      //0xaa
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      0b00000000,
      0b11111000,
      //0xab
      0b00000000,
      0b00000000,
      0b00101000,
      0b01010000,
      0b10100000,
      0b01010000,
      0b00101000,
      0b00000000,
      //0xac
      0b00000000,
      0b10010000,
      0b10101000,
      0b10101000,
      0b11101000,
      0b10101000,
      0b10101000,
      0b10010000,
      //0xad
      0b00000000,
      0b01111000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00101000,
      0b01001000,
      0b10001000,
      //0xae
      0b00000000,
      0b11111000,
      0b10001000,
      0b10101000,
      0b10001000,
      0b10011000,
      0b10101000,
      0b11111000,
      //0xaf
      0b00000000,
      0b00100000,
      0b01000000,
      0b01100000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb0
      0b01100000,
      0b10010000,
      0b10010000,
      0b10010000,
      0b01100000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb1
      0b00000000,
      0b00100000,
      0b00100000,
      0b11111000,
      0b00100000,
      0b00100000,
      0b00000000,
      0b11111000,
      //0xb2
      0b01100000,
      0b10010000,
      0b00100000,
      0b01000000,
      0b11110000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb3
      0b11100000,
      0b00010000,
      0b01100000,
      0b00010000,
      0b11100000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xb4
      0b11100000,
      0b10010000,
      0b11100000,
      0b10000000,
      0b10010000,
      0b10111000,
      0b10010000,
      0b00011000,
      //0xb5
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b11101000,
      0b10000000,
      0b10000000,
      //0xb6
      0b00000000,
      0b01111000,
      0b10011000,
      0b10011000,
      0b01111000,
      0b00011000,
      0b00011000,
      0b00011000,
      //0xb7
      0b00000000,
      0b00000000,
      0b00000000,
      0b00000000,
      0b01100000,
      0b01100000,
      0b00000000,
      0b00000000,
      //0xb8
      0b00000000,
      0b00000000,
      0b00000000,
      0b01010000,
      0b10001000,
      0b10101000,
      0b10101000,
      0b01010000,
      //0xb9
      0b01000000,
      0b11000000,
      0b01000000,
      0b01000000,
      0b11100000,
      0b00000000,
      0b00000000,
      0b00000000,
      //0xba
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      0b00000000,
      0b11111000,
      //0xbb
      0b00000000,
      0b00000000,
      0b10100000,
      0b01010000,
      0b00101000,
      0b01010000,
      0b10100000,
      0b00000000,
      //0xbc
      0b10001000,
      0b10010000,
      0b10100000,
      0b01010000,
      0b10110000,
      0b01010000,
      0b01111000,
      0b00010000,
      //0xbd
      0b10001000,
      0b10010000,
      0b10100000,
      0b01010000,
      0b10101000,
      0b00001000,
      0b00010000,
      0b00111000,
      //0xbe
      0b11000000,
      0b01000000,
      0b11000000,
      0b01001000,
      0b11011000,
      0b00101000,
      0b00111000,
      0b00001000,
      //0xbf
      0b00000000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b01000000,
      0b10000000,
      0b10001000,
      0b01110000,
      //0xc0
      0b01000000,
      0b00100000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0xc1
      0b00010000,
      0b00100000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0xc2
      0b00100000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0xc3
      0b01101000,
      0b10010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0xc4
      0b01010000,
      0b00000000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0xc5
      0b00100000,
      0b01010000,
      0b00100000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10001000,
      0b10001000,
      //0xc6
      0b00000000,
      0b00111000,
      0b01100000,
      0b10100000,
      0b10111000,
      0b11100000,
      0b10100000,
      0b10111000,
      //0xc7
      0b01110000,
      0b10001000,
      0b10000000,
      0b10000000,
      0b10001000,
      0b01110000,
      0b00010000,
      0b00110000,
      //0xc8
      0b01000000,
      0b00100000,
      0b00000000,
      0b11111000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b11111000,
      //0xc9
      0b00010000,
      0b00100000,
      0b00000000,
      0b11111000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b11111000,
      //0xca
      0b00100000,
      0b01010000,
      0b00000000,
      0b11111000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b11111000,
      //0xcb
      0b00000000,
      0b01010000,
      0b00000000,
      0b11111000,
      0b10000000,
      0b11110000,
      0b10000000,
      0b11111000,
      //0xcc
      0b01000000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xcd
      0b00010000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xce
      0b00100000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xcf
      0b00000000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xd0
      0b00000000,
      0b01110000,
      0b01001000,
      0b01001000,
      0b11101000,
      0b01001000,
      0b01001000,
      0b01110000,
      //0xd1
      0b01101000,
      0b10010000,
      0b00000000,
      0b10001000,
      0b11001000,
      0b10101000,
      0b10011000,
      0b10001000,
      //0xd2
      0b01000000,
      0b00100000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xd3
      0b00010000,
      0b00100000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xd4
      0b00100000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xd5
      0b01101000,
      0b10010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xd6
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xd7
      0b00000000,
      0b00000000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b01010000,
      0b10001000,
      0b00000000,
      //0xd8
      0b00000000,
      0b01110000,
      0b00100000,
      0b01110000,
      0b10101000,
      0b01110000,
      0b00100000,
      0b01110000,
      //0xd9
      0b01000000,
      0b00100000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xda
      0b00010000,
      0b00100000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xdb
      0b00100000,
      0b01010000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xdc
      0b01010000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xdd
      0b00010000,
      0b00100000,
      0b10001000,
      0b01010000,
      0b00100000,
      0b00100000,
      0b00100000,
      0b00100000,
      //0xde
      0b11000000,
      0b01000000,
      0b01110000,
      0b01001000,
      0b01001000,
      0b01110000,
      0b01000000,
      0b11000000,
      //0xdf
      0b00000000,
      0b00110000,
      0b01001000,
      0b01001000,
      0b01110000,
      0b01001000,
      0b01001000,
      0b10110000,
      //0xe0
      0b01000000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0xe1
      0b00010000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0xe2
      0b00100000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0xe3
      0b01101000,
      0b10010000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0xe4
      0b00000000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0xe5
      0b00100000,
      0b01010000,
      0b00100000,
      0b01110000,
      0b00001000,
      0b01111000,
      0b10001000,
      0b01111000,
      //0xe6
      0b00000000,
      0b00000000,
      0b11010000,
      0b00101000,
      0b01111000,
      0b10100000,
      0b10101000,
      0b01010000,
      //0xe7
      0b00000000,
      0b00000000,
      0b01110000,
      0b10000000,
      0b10001000,
      0b01110000,
      0b00100000,
      0b01100000,
      //0xe8
      0b01000000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      //0xe9
      0b00010000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      //0xea
      0b00100000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      //0xeb
      0b00000000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b11111000,
      0b10000000,
      0b01110000,
      //0xec
      0b01000000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xed
      0b00010000,
      0b00100000,
      0b00000000,
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xee
      0b00100000,
      0b01010000,
      0b00000000,
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xef
      0b00000000,
      0b01010000,
      0b00000000,
      0b00100000,
      0b01100000,
      0b00100000,
      0b00100000,
      0b01110000,
      //0xf0
      0b00000000,
      0b10100000,
      0b01000000,
      0b10100000,
      0b00010000,
      0b01111000,
      0b10001000,
      0b01110000,
      //0xf1
      0b01101000,
      0b10010000,
      0b00000000,
      0b10110000,
      0b11001000,
      0b10001000,
      0b10001000,
      0b10001000,
      //0xf2
      0b01000000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xf3
      0b00010000,
      0b00100000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xf4
      0b00000000,
      0b00100000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xf5
      0b00000000,
      0b01101000,
      0b10010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xf6
      0b00000000,
      0b01010000,
      0b00000000,
      0b01110000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b01110000,
      //0xf7
      0b00000000,
      0b00000000,
      0b00100000,
      0b00000000,
      0b11111000,
      0b00000000,
      0b00100000,
      0b00000000,
      //0xf8
      0b00000000,
      0b00010000,
      0b00100000,
      0b01110000,
      0b10101000,
      0b01110000,
      0b00100000,
      0b01000000,
      //0xf9
      0b01000000,
      0b00100000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      //0xfa
      0b00010000,
      0b00100000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      //0xfb
      0b00100000,
      0b01010000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      //0xfc
      0b00000000,
      0b01010000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b10001000,
      0b10011000,
      0b01101000,
      //0xfd
      0b00000000,
      0b00010000,
      0b00100000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
      //0xfe
      0b00000000,
      0b01100000,
      0b00100000,
      0b00110000,
      0b00101000,
      0b00110000,
      0b00100000,
      0b01110000,
      //0xff
      0b00000000,
      0b01010000,
      0b00000000,
      0b10001000,
      0b10001000,
      0b01111000,
      0b00001000,
      0b01110000,
    };
*/
    //  perl ../misc/itob.pl FontPage.java SMK6X8_FONT
    public static final byte[] SMK6X8_FONT = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\20\20\20\20\0\0\20\0(((\0\0\0\0\0((|(|((\0\20<P8\24x\20\0`d\b\20 L\f\0000HP TH4\0000\20 \0\0\0\0\0\b\20   \20\b\0 \20\b\b\b\20 \0\0\20T8T\20\0\0\0\20\20|\20\20\0\0\0\0\0\0000\20 \0\0\0\0|\0\0\0\0\0\0\0\0\00000\0\0\4\b\20 @\0\08DLTdD8\0\0200\20\20\20\208\08D\4\b\20 |\0|\b\20\b\4D8\0\b\30(H|\b\b\0|@x\4\4D8\0\30 @xDD8\0|\4\b\20   \08DD8DD8\08DD<\4\b0\0\00000\00000\0\0\00000\0000\20 \0\b\20 @ \20\b\0\0\0|\0|\0\0\0 \20\b\4\b\20 \08D\4\b\20\0\20\08D\0044TT8\08DDD|DD\0xDDxDDx\08D@@@D8\0pHDDDHp\0|@@x@@|\0|@@x@@@\08D@\\DD<\0DDD|DDD\08\20\20\20\20\208\0\34\b\b\b\bH0\0DHP`PHD\0@@@@@@|\0DlTTDDD\0DDdTLDD\08DDDDD8\0xDDx@@@\08DDDTH4\0xDDxPHD\0<@@8\4\4x\0|\20\20\20\20\20\20\0DDDDDD8\0DDDDD(\20\0DDDTTT(\0DD(\20(DD\0DDD(\20\20\20\0|\4\b\20 @|\08     8\0D(|\20|\20\20\08\b\b\b\b\b8\0\20(D\0\0\0\0\0\0\0\0\0\0\0|\0 \20\b\0\0\0\0\0\0\08\4<D<\0@@XdDDx\0\0\08@@D8\0\4\0044LDD<\0\0\08D|@8\0\30$ p   \0\0<DD<\48\0@@XdDDD\0\20\0000\20\20\208\0\b\0\30\b\bH0\0@@HP`PH\0000\20\20\20\20\208\0\0\0hTTDD\0\0\0XdDDD\0\0\08DDD8\0\0\0xDx@@\0\0\0004L<\4\4\0\0\0Xd@@@\0\0\08@8\4x\0  p  $\30\0\0\0DDDL4\0\0\0DDD(\20\0\0\0DDTT(\0\0\0D(\20(D\0\0\0DD<\48\0\0\0|\b\20 |\0\b\20\20 \20\20\b\0\20\20\20\20\20\20\20\0 \20\20\b\20\20 \0\0\20\b|\b\20\0\0\0\20 | \20\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0pPp\0\34\20\20\20\0\0\0\0\0\0\0\20\20\20p\0\0\0\0\0@ \20\0\0\0\00000\0\0\0\0|\4|\4\b\20\0\0\0|\4\30\20 \0\0\0\b\0200P\20\0\0\0\20|D\4\30\0\0\0\0|\20\20|\0\0\0\b|\30(H\0\0\0 |$( \0\0\0\08\b\b|\0\0\0x\bx\bx\0\0\0\0TT\4\30\0\0\0\0|\0\0\0\0|\4\24\30\20\20 \0\4\b\0200P\20\20\0\20|DD\4\b\20\0\0|\20\20\20\20|\0\b|\b\30(H\b\0 |$$$$H\0\20|\20|\20\20\20\0\0<$D\4\b0\0 <H\b\b\b\20\0\0|\4\4\4\4|\0(|((\b\20 \0\0`\4d\4\bp\0\0|\4\b\20(D\0 |$(  \34\0\0DD$\4\b0\0\0<$T\f\b0\0\bp\20|\20\20 \0\0TTT\4\b\20\08\0|\20\20\20 \0   0(  \0\20\20|\20\20 @\0\08\0\0\0\0|\0\0|\4(\20(@\0\20|\b\208T\20\0\b\b\b\b\b\20 \0\0\20\bDDDD\0@@|@@@<\0\0|\4\4\4\b0\0\0 P\b\4\4\0\0\20|\20\20TT\20\0\0|\4\4(\20\b\0\08\08\08\4\0\0\20 @D|\4\0\0\4\4(\20(@\0\0| |  \34\0  |$(  \0\08\b\b\b\b|\0\0|\4|\4\4|\08\0|\4\4\b\20\0HHHH\b\20 \0\0\20PPTTX\0\0@@DHP`\0\0|DDDD|\0\0|DD\4\b\20\0\0`\0\4\4\bp\0\20H \0\0\0\0\0pPp\0\0\0\0\0\0\0$THH4\0(\08\4<D<\0\0\08DxDx@\0\08@0D8\0\0\0DDDLt@\0\0<PHD8\0\0\0\30$DDx@\0\0<DDD<\4\0\0\34\20\20P \0\0\bh\b\0\0\0\0\b\0\30\b\b\b\b\b\0P P\0\0\0\0\0\208PT8\20\0  p p <\08\0XdDDD\0(\08DDD8\0\0\0XdDDx@\0000LDD<\4\4\08D|DD8\0\0\0\0,Th\0\0\0\08DD(l\0(\0DDDDL4|@ \20 @|\0\0\0|(((L\0|\0D(\20(D\0\0\0DDDD<\4\0\4x\20|\20\20\0\0\0| <$D\0\0\0|T|DD\0\0\20\0|\0\20\0\0\0\0\0\0\0\0\0\0||||||||\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0 08<80 \0\b\308x8\30\b\0$Hl\0\0\0\0\0l$H\0\0\0\0\0\208|\0\208|\0|8\20\0|8\20\0\08|||8\0\0\4\4\24$| \20\0\208T\20\20\20\20\0\20\20\20\20T8\20\0\0\20\b|\b\20\0\0\0\20 | \20\0\0\b\20 \20\b\0|\0 \20\b\20 \0|\0\0\20\2088|\0\0\0|88\20\20\0\0\0\0\0\0\0\0\0\0\20\20\20\20\0\0\20\0(((\0\0\0\0\0((|(|((\0\20<P8\24x\20\0`d\b\20 L\f\0000HP TH4\0000\20 \0\0\0\0\0\b\20   \20\b\0 \20\b\b\b\20 \0\0\20T8T\20\0\0\0\20\20|\20\20\0\0\0\0\0\0000\20 \0\0\0\0|\0\0\0\0\0\0\0\0\00000\0\0\4\b\20 @\0\08DLTdD8\0\0200\20\20\20\208\08D\4\b\20 |\0|\b\20\b\4D8\0\b\30(H|\b\b\0|@x\4\4D8\0\30 @xDD8\0|D\4\b\20\20\20\08DD8DD8\08DD<\4\b0\0\00000\00000\0\0\00000\0000\20 \0\b\20 @ \20\b\0\0\0|\0|\0\0\0 \20\b\4\b\20 \08D\4\b\20\0\20\08D\0044TT8\0\20(DD|DD\0xDDxDDx\08D@@@D8\0pHDDDHp\0|@@x@@|\0|@@x@@@\08D@\\DD<\0DDD|DDD\08\20\20\20\20\208\0\34\b\b\b\bH0\0DHP`PHD\0@@@@@@|\0DlTTDDD\0DDdTLDD\08DDDDD8\0xDDx@@@\08DDDTH4\0xDDxPHD\08D@8\4D8\0|\20\20\20\20\20\20\0DDDDDD8\0DDDDD(\20\0DDDTTT(\0DD(\20(DD\0DDD(\20\20\20\0|\4\b\20 @|\08     8\0\0@ \20\b\4\0\08\b\b\b\b\b8\0\20(D\0\0\0\0\0\0\0\0\0\0\0|\0 \20\b\0\0\0\0\0\0\08\4<D<\0@@XdDDx\0\0\08@@D8\0\4\0044LDD<\0\0\08D|@8\0\30$ p   \0\0\0<D<\48\0@@XdDDD\0\20\0\0200\20\208\0\b\0\30\b\bH0\0@@HP`PH\0000\20\20\20\20\208\0\0\0hTTTT\0\0\0XdDDD\0\0\08DDD8\0\0\0xDx@@\0\0\0004L<\4\4\0\0\0Xd@@@\0\0\08@8\4x\0  p  $\30\0\0\0DDDL4\0\0\0DDD(\20\0\0\0DDTT(\0\0\0D(\20(D\0\0\0DD<\48\0\0\0|\b\20 |\0\b\20\20 \20\20\b\0\20\20\20\20\20\20\20\0 \20\20\b\20\20 \0\0\0\0004H\0\0\0\20(DDD|\0\0|D@xDDx<\24\24$D|DD\0TTT8TTT\0x\4\4\30\4\4x\0DDLTdDD(\20DDLTdD\0<\24\24\24\24T$\0|DDDDDD\0DDD(\20 @\0DDDDD|\4\0DDD<\4\4\4\0\0TTTTT|\0TTTTT|\4\0`  8$$8\0DDDdTTd\08D\24,\4D8\0\0\0$THH4\0\20\30\24\24\20pp\0|D@@@@@\0\0\0|(((L\0|@ \20 @|\0\0\0<HHH0\30\24\34\24\24tl\f\0\0\48P\20\20\b\0\20888|\20\0\08DD|DD8\0\08DDD(l\0\30$\20(DD8\0\0\0,Th\0\0\0\0(|||8\20\0\0\08@0D8\08DDDDDD\0lllllll\0\20\0\0\20\20\20\20\0\208PPT8\20\0\30  p $X\0\0D8(8D\0\0D(|\20|\20\20\0\20\20\20\0\20\20\20\0\30$\20(\20H0\0\b\24\20|\20P \0|DT\\TD|\08\4<D<\0|\0\0\24(P(\24\0\0HTTtTTH\0<DD<\24$D\0|DTDLT|\0\20 0\0\0\0\0000HHH0\0\0\0\0\20\20|\20\20\0|0H\20 x\0\0\0p\b0\bp\0\0\0pHp@H\\H\f\0DDDLt@@\0<LL<\f\f\f\0\0\0\00000\0\0\0\0\0(DTT( `  p\0\0\0\08DDD8\0|\0\0P(\24(P\0DHP(X(<\bDHP(T\4\b\34` `$l\24\34\4\0\20\0\20 @D8 \20\20(D|DD\b\20\20(D|DD\20(\08D|DD4H\08D|DD(\0\20(D|DD\20(\208D|DD\0\0340P\\pP\\8D@@D8\b\30 \20\0|@x@|\b\20\0|@x@|\20(\0|@x@|\0(\0|@x@| \20\08\20\20\208\b\20\08\20\20\208\20(\08\20\20\208\0(\08\20\20\208\08$$t$$84H\0DdTLD \208DDDD8\b\208DDDD8\20(\08DDD84H\08DDD8(\08DDDD8\0\0D(\20(D\0\08\208T8\208 \20DDDDD8\b\20DDDDD8\20(\0DDDD8(\0DDDDD8\b\20D(\20\20\20\20` 8$$8 `\0\30$$8$$X \20\08\4<D<\b\20\08\4<D<\20(\08\4<D<4H\08\4<D<\0(\08\4<D<\20(\208\4<D<\0\0h\24<PT(\0\08@D8\0200 \20\08D|@8\b\20\08D|@8\20(\08D|@8\0(\08D|@8 \20\0\0200\20\208\b\20\0\0200\20\208\20(\0\0200\20\208\0(\0\0200\20\208\0P P\b<D84H\0XdDDD \20\08DDD8\b\20\08DDD8\0\20(\08DD8\0004H\08DD8\0(\08DDD8\0\0\20\0|\0\20\0\0\b\208T8\20  \20\0DDDL4\b\20\0DDDL4\20(\0DDDL4\0(\0DDDL4\0\b\20DD<\48\0000\20\30\24\30\208\0(\0DD<\48".getBytes (XEiJ.ISO_8859_1);

  }  //class FontPage.Smk



}  //class FontPage



