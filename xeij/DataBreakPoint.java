//========================================================================================
//  DataBreakPoint.java
//    en:Data break point -- It stops the MPU when a data that meets the specified condition is read or written.
//    ja:データブレークポイント -- 指定された条件に合うデータが読み書きされたらMPUを止めます。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  機能
//    指定されたアドレスから指定された条件に合う値が読み出されたら停止する
//    指定されたアドレスに指定された条件に合う値が書き込まれたら停止する
//  情報
//    udm     ユーザデータメモリマップ
//    sdm     スーパーバイザデータメモリマップ
//    mdm     現在のデータメモリマップ
//    データブレークポイント
//      有効フラグ
//      アドレス
//      サイズ
//      マスク
//      下限
//      上限
//      回数
//      閾値
//    最後にヒットしたリードデータブレークポイント
//    最後にヒットしたライトデータブレークポイント
//  条件
//    有効である
//    アドレスが一致している
//    サイズが一致している
//      命令のオペレーションサイズではなくバスアクセスのサイズなので、ワードとロングは分割されている場合がある
//    下限<=上限のとき
//      下限<=マスクされたデータ&&マスクされたデータ<=上限
//    上限<下限のとき
//      マスクされたデータ<=上限||下限<=マスクされたデータ
//    比較は常に符号なしで行われる
//  動作
//    データブレークポイントがあるページから読み出されたとき
//      元のページから読み出す
//        ここでバスエラーが発生して戻ってこなかったときは何もしない
//      リードデータブレークポイントの条件をテストする
//    データブレークポイントがあるページに書き込まれたとき
//      ライトデータブレークポイントの条件をテストする
//      元のページに書き込む
//        ここでバスエラーが発生して戻ってこなかったときは何もしない
//    条件が成立したブレークポイントがあるとき
//      回数を更新する
//      回数が閾値以上のとき
//        コアを停止させる
//        実行中の命令は最後まで実行させなければならない
//        エラーをthrowするのではなくループカウンタをオーバーフローさせた上でタスクを停止させる
//        実行中の命令が終わるまでに複数のデータブレークポイントがヒットする可能性があることに注意する
//  メモリマップ
//    現在はumとsmが大元のメモリマップになっている
//    グラフィックス画面、スーパーバイザ領域設定、RTC、拡張SCSI、CGROMなどはメモリマップを動的に組み替えている
//    メモリマップの組み換えの競合を避けるため、um,smをコピーしてudm,sdmを作る
//    um,smへの変更はデータブレークポイントがないページに限ってudm,sdmにもコピーされる
//    データブレークポイントが設定されるとudm,sdmが変更される
//    データブレークポイントが解除されるとum,smを用いてudm,sdmが復元される
//    mmの代わりにmdmを使う
//      ユーザモードに切り替えるとき
//        mdm = udm;
//      スーパーバイザモードに切り替えるとき
//        mdm = sdm;
//    モード切り替えでmmとmdmの両方を更新するのは負荷が増えるのでmmを廃止する
//      残骸が残っているとデバッグの邪魔になるので一旦削除して完全に移行できたか確認した方がよい
//    MPUのDMAのリードとライトはすべてmdmを経由して行う
//      DMAによるライトも検出できるようにする
//    ピークはリードを経由せずum,smに直接アクセスしなければならない
//      そうしないとメモリダンプでも停止してしまうことになる
//      mmを廃止するのでumとsmのどちらかを使うか毎回選ばなければならない
//  課題
//    MOVEMでメインメモリを特別扱いにする処理が重くなる
//    ディスクアクセスをどうするか
//      セクタ単位のディスクアクセスは途中で止めることができない
//      1つの命令が多数のデータにアクセスしているだけと考えれば問題ない?
//    ページを跨ぐロングワードのデータブレークポイントは設置が困難
//    4の倍数でないアドレスのロングワードのアクセスを常に分割してしまうとロングワードのブレークポイントを設置できる場所が制限される
//    アドレスは一致しているが設定されたサイズと異なるサイズでアクセスされたらどうするか
//      バイトサイズで待っていたらワードサイズでアクセスされたとき
//        データは揃っているのだから上位バイトか下位バイトのどちらかだけテストすればよい
//      ワードサイズで待っていたらバイトサイズでアクセスされたとき
//        ワードサイズのデータの範囲の条件が下限==0x0000&&上限==0xffffまたは下限==上限のとき
//          バイトサイズのデータの範囲の条件も同じなので範囲の条件を満たすかどうか判断できる
//        その他
//          データが不足しているので範囲の条件を満たすかどうか判断できない
//    もっと複雑な条件を与えたい
//      条件を式で与える
//      元の値よりも小さい値が書き込まれたら停止する
//        slt(d,rws(a))
//      アドレスがマッチすることが少なければ条件が複雑でも全体のパフォーマンスにそれほど影響はないはず
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class DataBreakPoint {

  public static final boolean DBP_ON = true;  //true=データブレークポイントの機能を用いる

  //サイズ
  public static final int DBP_BYTE  = 0;
  public static final int DBP_WORD  = 1;
  public static final int DBP_LONG  = 2;
  public static final java.util.List<String> DBP_SIZE_LIST_EN = Arrays.asList ("Byte", "Word", "Long");
  public static final java.util.List<String> DBP_SIZE_LIST_JA = Arrays.asList ("バイト", "ワード", "ロング");

  //メモリマップ
  public static final MemoryMappedDevice[] dbpUserMap = DBP_ON ? new MemoryMappedDevice[XEiJ.BUS_PAGE_COUNT] : null;  //ユーザデータメモリマップ
  public static final MemoryMappedDevice[] dbpSuperMap = DBP_ON ? new MemoryMappedDevice[XEiJ.BUS_PAGE_COUNT] : null;  //スーパーバイザデータメモリマップ
  public static MemoryMappedDevice[] dbpMemoryMap;  //現在のデータメモリマップ。srS==0?udm:sdm

  //リスト
  public static ArrayList<DataBreakRecord> dbpList;  //リスト

  //ウインドウ
  public static int dbpLock;  //スピナーのチェンジイベントを抑制するためのフラグ
  public static JFrame dbpFrame;  //フレーム
  public static GridLayout dbpListGridLayout;  //リストパネルのグリッドレイアウト
  public static JPanel dbpListPanel;  //リストパネル

  //dbpInit ()
  //  データブレークポイントを初期化する
  public static void dbpInit () {

    Arrays.fill (dbpUserMap, MemoryMappedDevice.MMD_NUL);
    Arrays.fill (dbpSuperMap, MemoryMappedDevice.MMD_NUL);
    dbpMemoryMap = dbpSuperMap;

    //リスト
    dbpList = new ArrayList<DataBreakRecord> ();

  }  //dbpInit()

  //dbpStart ()
  public static void dbpStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DBP_FRAME_KEY)) {
      dbpOpen ();
    }
  }  //dbpStart()

  //dbpOpen ()
  //  データブレークポイントウインドウを開く
  public static void dbpOpen () {
    if (dbpFrame == null) {
      dbpMakeFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_DBP_VISIBLE_MASK;
    XEiJ.pnlExitFullScreen (false);
    dbpFrame.setVisible (true);
  }  //dbpOpen()

  //dbpMakeFrame ()
  //  データブレークポイントウインドウを作る
  //  ここでは開かない
  public static void dbpMakeFrame () {
    dbpLock = 0;
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Add":  //追加
          new DataBreakRecord (0x00000000, DBP_BYTE, 0xff, 0, 0xff, true, true, false, 0, 0);
          break;
        }
      }
    };
    //ウインドウ
    dbpListGridLayout = new GridLayout (1, 9);
    dbpListPanel = ComponentFactory.addComponents (
      new JPanel (dbpListGridLayout),
      Multilingual.mlnText (ComponentFactory.createLabel ("Enabled"), "ja", "有効"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Address"), "ja", "アドレス"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Size"), "ja", "サイズ"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Mask"), "ja", "マスク"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Lower"), "ja", "下限"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Upper"), "ja", "上限"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Read"), "ja", "リード"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Write"), "ja", "ライト"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Count"), "ja", "回数"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Threshold"), "ja", "閾値"),
      Multilingual.mlnText (ComponentFactory.createLabel ("Remove"), "ja", "削除")
      );
    dbpFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_DBP_FRAME_KEY,
        "Data break point",
        null,
        ComponentFactory.setPreferredSize (
          ComponentFactory.createBorderPanel (
            //CENTER
            ComponentFactory.createScrollPane (dbpListPanel),
            //NORTH
            null,
            //WEST
            null,
            //SOUTH
            ComponentFactory.createFlowPanel (
              FlowLayout.CENTER,
              Multilingual.mlnText (ComponentFactory.createButton ("Add", listener), "ja", "追加")
              )
            //EAST
            ),
          750, 100)
        ),
      "ja", "データブレークポイント");
    //  ウインドウリスナー
    ComponentFactory.addListener (
      dbpFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_DBP_VISIBLE_MASK;
        }
      });
  }  //dbpMakeFrame()

  //dbpBreak (type, address, data, written)
  //  条件に合うブレークポイントを探して見つかったら回数を増やす。閾値に達したらコアを停止させる
  //  バイトアクセスしたときバイトサイズのブレークポイントがマッチする
  //  ワードアクセスしたときバイトサイズとワードサイズのブレークポイントがマッチする
  //  ロングワードアクセスしたときバイトサイズとワードサイズとロングワードサイズのブレークポイントがマッチする
  public static void dbpBreak (int size, int address, int data, boolean written) {
    if (size == DBP_BYTE) {  //バイトアクセス
      for (DataBreakRecord r : dbpList) {
        if (r.dbrEnabled && (written ? r.dbrWrite : r.dbrRead)) {  //ブレークポイントが有効かつリード/ライトが一致
          if (r.dbrSize == DBP_BYTE) {  //ブレークポイントがバイトサイズ
            if (address == r.dbrAddress) {  //バイトアクセスの1バイト目にバイトサイズのブレークポイントがある
              int masked = data & r.dbrMask;  //マスクされたバイトデータの1バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達している
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //データ
            }  //アドレス
          }  //ブレークポイントのサイズ
        }  //ブレークポイントが有効
      }  //for r
    } else if (size == DBP_WORD) {  //ワードアクセス
      for (DataBreakRecord r : dbpList) {
        if (r.dbrEnabled && (written ? r.dbrWrite : r.dbrRead)) {  //ブレークポイントが有効かつリード/ライトが一致
          if (r.dbrSize == DBP_BYTE) {  //ブレークポイントがバイトサイズ
            if (address == r.dbrAddress) {  //ワードアクセスの1バイト目にバイトサイズのブレークポイントがある
              int masked = data >>> 8 & r.dbrMask;  //マスクされたワードデータの1バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            } else if (address + 1 == r.dbrAddress) {  //ワードアクセスの2バイト目にバイトサイズのブレークポイントがある
              int masked = data & r.dbrMask;  //マスクされたワードデータの2バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            }  //アドレス
          } else if (r.dbrSize == DBP_WORD) {  //ブレークポイントがワードサイズ
            if (address == r.dbrAddress) {  //ワードアクセスの1ワード目にワードサイズのブレークポイントがある
              int masked = data & r.dbrMask;  //マスクされたワードデータの1ワード目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            }  //アドレス
          }  //ブレークポイントのサイズ
        }  //ブレークポイントが有効
      }  //for r
    } else if (size == DBP_LONG) {  //ロングワードアクセス
      for (DataBreakRecord r : dbpList) {
        if (r.dbrEnabled && (written ? r.dbrWrite : r.dbrRead)) {  //ブレークポイントが有効かつリード/ライトが一致
          if (r.dbrSize == DBP_BYTE) {  //ブレークポイントがバイトサイズ
            if (address == r.dbrAddress) {  //ロングワードアクセスの1バイト目にバイトサイズのブレークポイントがある
              int masked = data >>> 24 & r.dbrMask;  //マスクされたロングワードデータの1バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            } else if (address + 1 == r.dbrAddress) {  //ロングワードアクセスの2バイト目にバイトサイズのブレークポイントがある
              int masked = data >>> 16 & r.dbrMask;  //マスクされたロングワードデータの2バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            } else if (address + 2 == r.dbrAddress) {  //ロングワードアクセスの3バイト目にバイトサイズのブレークポイントがある
              int masked = data >>> 8 & r.dbrMask;  //マスクされたロングワードデータの3バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            } else if (address + 3 == r.dbrAddress) {  //ロングワードアクセスの4バイト目にバイトサイズのブレークポイントがある
              int masked = data & r.dbrMask;  //マスクされたロングワードデータの4バイト目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            }  //アドレス
          } else if (r.dbrSize == DBP_WORD) {  //ブレークポイントがワードサイズ
            if (address == r.dbrAddress) {  //ロングワードアクセスの1ワード目にワードサイズのブレークポイントがある
              int masked = data >> 16 & r.dbrMask;  //マスクされたロングワードデータの1ワード目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            } else if (address + 2 == r.dbrAddress) {  //ロングワードアクセスの2ワード目にワードサイズのブレークポイントがある
              int masked = data & r.dbrMask;  //マスクされたロングワードデータの2ワード目
              if (r.dbrLower <= r.dbrUpper ?
                  r.dbrLower <= masked && masked <= r.dbrUpper :
                  masked <= r.dbrUpper || r.dbrLower <= masked) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            }  //アドレス
          } else if (r.dbrSize == DBP_LONG) {  //ブレークポイントがロングワードサイズ
            if (address == r.dbrAddress) {  //ロングワードアクセスの1ロングワード目にロングワードサイズのブレークポイントがある
              int masked = data & r.dbrMask;  //マスクされたロングワードデータの1ロングワード目
              if (Integer.compareUnsigned (r.dbrLower, r.dbrUpper) <= 0 ?
                  Integer.compareUnsigned (r.dbrLower, masked) <= 0 && Integer.compareUnsigned (masked, r.dbrUpper) <= 0 :
                  Integer.compareUnsigned (masked, r.dbrUpper) <= 0 || Integer.compareUnsigned (r.dbrLower, masked) <= 0) {  //データが範囲内
                r.dbrSetCount (r.dbrCount + 1);  //回数を増やす
                if (r.dbrThreshold <= r.dbrCount) {  //閾値に達しているとき
                  XEiJ.mpuStop (null);  //コアを停止させる
                }
              }  //条件
            }  //アドレス
          }  //ブレークポイントのサイズ
        }  //ブレークポイントが有効
      }  //for r
    }  //アクセスサイズ
  }  //dbpBreak(int,int,int)



  public static class DataBreakRecord implements Comparable<DataBreakRecord> {

    //設定
    public int dbrAddress;  //アドレス
    public int dbrSize;  //サイズ。DBP_BYTE=バイト,DBP_WORD=ワード,DBP_LONG=ロング
    public int dbrMask;  //マスク。バイトのときは0xff、ワードのときは0xffffを超えないこと
    public int dbrLower;  //下限。符号なし比較。下限の方が大きいときは範囲外を意味する
    public int dbrUpper;  //上限。符号なし比較。lower<=upper?lower<=data&&data<=upper:data<=upper||lower<=data
    public boolean dbrRead;  //リード
    public boolean dbrWrite;  //ライト

    //状態
    public boolean dbrEnabled;  //true=有効
    public int dbrCount;  //回数
    public int dbrThreshold;  //閾値

    //パネル
    public Box dbrEnabledBox;
    public JCheckBox dbrEnabledCheckBox;
    public Hex8Spinner dbrAddressSpinner;
    public JSpinner dbrSizeSpinner;
    public JLabel[] dbrSizeLabels;
    public Hex8Spinner dbrMaskSpinner;
    public Hex8Spinner dbrLowerSpinner;
    public Hex8Spinner dbrUpperSpinner;
    public JCheckBox dbrReadCheckBox;
    public JCheckBox dbrWriteCheckBox;
    public JSpinner dbrCountSpinner;
    public SpinnerNumberModel dbrCountModel;
    public JSpinner dbrThresholdSpinner;
    public SpinnerNumberModel dbrThresholdModel;
    public Box dbrRemoveBox;
    public JButton dbrRemoveButton;

    //コンパレータ
    @Override public int compareTo (DataBreakRecord r) {
      int t = Integer.compareUnsigned (dbrAddress, r.dbrAddress);
      if (t == 0) {
        t = Integer.compareUnsigned (dbrSize, r.dbrSize);
        if (t == 0) {
          t = Integer.compareUnsigned (dbrMask, r.dbrMask);
          if (t == 0) {
            t = Integer.compareUnsigned (dbrLower, r.dbrLower);
            if (t == 0) {
              t = Integer.compareUnsigned (dbrUpper, r.dbrUpper);
            }
          }
        }
      }
      return t;
    }

    //new DataBreakRecord (address, size, mask, lower, upper, read, write, enabled, count, threshold)
    //  コンストラクタ
    @SuppressWarnings ("this-escape") public DataBreakRecord (int address, int size, int mask, int lower, int upper, boolean read, boolean write,
                                                              boolean enabled, int count, int threshold) {

      //サイズによるマスク
      mask &= size == DBP_BYTE ? 0xff : size == DBP_WORD ? 0xffff : 0xffffffff;
      lower &= mask;
      upper &= mask;

      //設定
      dbrAddress = address;  //アドレス
      dbrSize = size;  //サイズ
      dbrMask = mask;  //マスク
      dbrLower = lower;  //下限
      dbrUpper = upper;  //上限
      dbrRead = read;  //リード
      dbrWrite = write;  //ライト

      //状態
      dbrEnabled = enabled;  //true=有効
      dbrCount = count;  //回数
      dbrThreshold = threshold;  //閾値

      //アクションリスナー
      ActionListener listener = new ActionListener () {
        @Override public void actionPerformed (ActionEvent ae) {
          Object source = ae.getSource ();
          switch (ae.getActionCommand ()) {
          case "Enabled":  //有効
            dbrSetEnabled (((JCheckBox) source).isSelected ());
            break;
          case "Read":  //リード
            dbrRead = ((JCheckBox) source).isSelected ();
            break;
          case "Write":  //ライト
            dbrWrite = ((JCheckBox) source).isSelected ();
            break;
          case "Remove":  //削除
            dbrRemove ();
            break;
          }
        }
      };

      //有効
      dbrEnabledBox = ComponentFactory.createGlueBox (
        dbrEnabledCheckBox = Multilingual.mlnText (ComponentFactory.createCheckBox (dbrEnabled, "Enabled", listener), "ja", "有効")
        );

      //アドレス
      dbrAddressSpinner = ComponentFactory.createHex8Spinner (dbrAddress, 0xffffffff, false, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (dbpLock == 0) {
            dbrSetAddress (((Hex8Spinner) ce.getSource ()).getIntValue ());  //構築中はdbrAddressSpinnerを参照できないことに注意
          }
        }
      });

      //サイズ
      dbrSizeSpinner = Multilingual.mlnList (
        ComponentFactory.createListSpinner (
          DBP_SIZE_LIST_EN,
          DBP_SIZE_LIST_EN.get (dbrSize),
          new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              if (dbpLock == 0) {
                SpinnerListModel model = (SpinnerListModel) ((JSpinner) ce.getSource ()).getModel ();  //構築中はdbrSizeSpinnerを参照できないことに注意
                dbrSetSize (model.getList ().indexOf (model.getValue ()));
              }
            }
          }),
        "ja", DBP_SIZE_LIST_JA);

      //マスク
      dbrMaskSpinner = ComponentFactory.createHex8Spinner (dbrMask, -1, false, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (dbpLock == 0) {
            dbrSetMask (((Hex8Spinner) ce.getSource ()).getIntValue ());  //構築中はdbrMaskSpinnerを参照できないことに注意
          }
        }
      });

      //下限
      dbrLowerSpinner = ComponentFactory.createHex8Spinner (dbrLower, -1, false, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (dbpLock == 0) {
            dbrSetLower (((Hex8Spinner) ce.getSource ()).getIntValue ());  //構築中はdbrLowerSpinnerを参照できないことに注意
          }
        }
      });

      //上限
      dbrUpperSpinner = ComponentFactory.createHex8Spinner (dbrUpper, -1, false, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (dbpLock == 0) {
            dbrSetUpper (((Hex8Spinner) ce.getSource ()).getIntValue ());  //構築中はdbrUpperSpinnerを参照できないことに注意
          }
        }
      });

      //リード
      dbrReadCheckBox = Multilingual.mlnText (ComponentFactory.createCheckBox (dbrRead, "Read", listener), "ja", "リード");

      //ライト
      dbrWriteCheckBox = Multilingual.mlnText (ComponentFactory.createCheckBox (dbrWrite, "Write", listener), "ja", "ライト");

      //回数
      dbrCountSpinner = ComponentFactory.createNumberSpinner (
        dbrCountModel = new SpinnerNumberModel (dbrCount, 0, 99999999, 1),
        8,
        new ChangeListener () {
          @Override public void stateChanged (ChangeEvent ce) {
            if (dbpLock == 0) {
              dbrSetCount (dbrCountModel.getNumber ().intValue ());
            }
          }
        });

      //閾値
      dbrThresholdSpinner = ComponentFactory.createNumberSpinner (
        dbrThresholdModel = new SpinnerNumberModel (dbrThreshold, 0, 99999999, 1),
        8,
        new ChangeListener () {
          @Override public void stateChanged (ChangeEvent ce) {
            if (dbpLock == 0) {
              dbrSetThreshold (dbrThresholdModel.getNumber ().intValue ());
            }
          }
        });

      //削除
      dbrRemoveBox = ComponentFactory.createGlueBox (
        dbrRemoveButton = Multilingual.mlnText (ComponentFactory.createButton ("Remove", listener), "ja", "削除")
        );

      //データブレークポイントのリストに加える
      dbpList.add (this);  //[this-escape]

      //パネルに加える
      dbpListGridLayout.setRows (dbpListGridLayout.getRows () + 1);
      ComponentFactory.addComponents (
        dbpListPanel,
        dbrEnabledBox,  //有効
        dbrAddressSpinner,  //アドレス
        dbrSizeSpinner,  //サイズ
        dbrMaskSpinner,  //マスク
        dbrLowerSpinner,  //下限
        dbrUpperSpinner,  //上限
        dbrReadCheckBox,  //リード
        dbrWriteCheckBox,  //ライト
        dbrCountSpinner,  //回数
        dbrThresholdSpinner,  //閾値
        dbrRemoveBox
        );
      dbpListPanel.validate ();
      dbpListPanel.repaint ();

      //このデータブレークポイントが有効かどうかでページのデータブレークポイントを有効にするかどうか決める
      dbrSetEnabled (dbrEnabled);

    }  //new DataBreakRecord(int,int,int,int,int,boolean,int,int)

    //dbr.dbrRemove ()
    //  データブレークポイントを取り除く
    public void dbrRemove () {

      //このデータブレークポイントを無効にして必要ならばページのデータブレークポイントを無効にする
      dbrSetEnabled (false);

      //パネルから取り除く
      ComponentFactory.removeComponents (
        dbpListPanel,
        dbrEnabledBox,  //有効
        dbrAddressSpinner,  //アドレス
        dbrSizeSpinner,  //サイズ
        dbrMaskSpinner,  //マスク
        dbrLowerSpinner,  //下限
        dbrUpperSpinner,  //上限
        dbrReadCheckBox,  //リード
        dbrWriteCheckBox,  //ライト
        dbrCountSpinner,  //回数
        dbrThresholdSpinner,  //閾値
        dbrRemoveBox
        );
      dbpListGridLayout.setRows (dbpListGridLayout.getRows () - 1);
      dbpListPanel.validate ();
      dbpListPanel.repaint ();

      //リストから取り除く
      dbpList.remove (this);

    }  //dbr.dbrRemove()

    //dbr.dbrSetAddress (address)
    //  アドレスを設定する
    public void dbrSetAddress (int address) {
      if (dbrAddress != address) {
        dbrAddress = address;
        if (dbrAddressSpinner != null) {
          dbpLock++;
          dbrAddressSpinner.setIntValue (address);
          dbpLock--;
        }
      }
    }  //dbr.dbrSetAddress(int)

    //dbr.dbrSetSize (size)
    //  サイズを設定する
    public void dbrSetSize (int size) {
      if (dbrSize != size) {
        dbrSize = size;
        if (dbrSizeSpinner != null) {
          dbpLock++;
          dbrSizeSpinner.setValue (((SpinnerListModel) dbrSizeSpinner.getModel ()).getList ().get (size));
          dbpLock--;
        }
        dbrSetMask (dbrMask & (dbrSize == DBP_BYTE ? 0xff : dbrSize == DBP_WORD ? 0xffff : 0xffffffff));
      }
    }  //dbr.dbrSetSize(int)

    //dbr.dbrSetMask (mask)
    //  マスクを設定する
    public void dbrSetMask (int mask) {
      mask &= dbrSize == DBP_BYTE ? 0xff : dbrSize == DBP_WORD ? 0xffff : 0xffffffff;
      if (dbrMask != mask) {
        dbrMask = mask;
        if (dbrMaskSpinner != null) {
          dbpLock++;
          dbrMaskSpinner.setIntValue (mask);
          dbpLock--;
        }
        dbrSetLower (dbrLower);
        dbrSetUpper (dbrUpper);
      }
    }  //dbr.dbrSetMask(int)

    //dbr.dbrSetLower (lower)
    //  下限を設定する
    public void dbrSetLower (int lower) {
      lower &= dbrMask;
      if (dbrLower != lower) {
        dbrLower = lower;
        if (dbrLowerSpinner != null) {
          dbpLock++;
          dbrLowerSpinner.setIntValue (lower);
          dbpLock--;
        }
      }
    }  //dbr.dbrSetLower(int)

    //dbr.dbrSetUpper (upper)
    //  上限を設定する
    public void dbrSetUpper (int upper) {
      upper &= dbrMask;
      if (dbrUpper != upper) {
        dbrUpper = upper;
        if (dbrUpperSpinner != null) {
          dbpLock++;
          dbrUpperSpinner.setIntValue (upper);
          dbpLock--;
        }
      }
    }  //dbr.dbrSetUpper(int)

    //dbr.dbrSetEnabled (enabled)
    //  有効/無効を設定する
    public void dbrSetEnabled (boolean enabled) {
      if (dbrEnabled != enabled) {
        dbrEnabled = enabled;
        dbrEnabledCheckBox.setSelected (enabled);
      }
      int p = dbrAddress >>> XEiJ.BUS_PAGE_BITS;  //ページ番号
      if (dbrEnabled) {  //有効
        //ページのデータブレークポイントを有効にする
        dbpUserMap[p] = dbpSuperMap[p] = MemoryMappedDevice.MMD_DBP;
      } else {  //無効
        //同じページに他にデータブレークポイントがなければページのデータブレークポイントを無効にする
        boolean exists = false;
        for (DataBreakRecord r : dbpList) {
          if (r.dbrAddress >>> XEiJ.BUS_PAGE_BITS == p && r != this) {  //同じページに他のデータブレークポイントがある
            exists = true;
            break;
          }
        }
        dbpUserMap[p] = exists ? MemoryMappedDevice.MMD_DBP : XEiJ.busUserMap[p];
        dbpSuperMap[p] = exists ? MemoryMappedDevice.MMD_DBP : XEiJ.busSuperMap[p];
      }
    }  //dbr.dbrSetEnabled(boolean)

    //dbr.dbrSetCount (count)
    //  回数を設定する
    public void dbrSetCount (int count) {
      if (dbrCount != count) {
        dbrCount = count;
        if (dbrCountSpinner != null) {
          dbpLock++;
          dbrCountSpinner.setValue (Integer.valueOf (count));
          dbpLock--;
        }
      }
    }  //dbr.dbrSetCount(int)

    //dbr.dbrSetThreshold (threshold)
    //  閾値を設定する
    public void dbrSetThreshold (int threshold) {
      if (dbrThreshold != threshold) {
        dbrThreshold = threshold;
        if (dbrThresholdSpinner != null) {
          dbpLock++;
          dbrThresholdSpinner.setValue (Integer.valueOf (threshold));
          dbpLock--;
        }
      }
    }  //dbr.dbrSetThreshold(int)

  }  //class DataBreakRecord



}  //class DataBreakPoint



