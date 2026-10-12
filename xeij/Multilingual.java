//========================================================================================
//  Multilingual.java
//    en:Multilingual
//    ja:多言語化
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  あらかじめ英語のテキストを設定しておく
//  言語はISO 639 alpha-2の文字列("ja"など)で指定する
//  text
//    AbstractButton
//      JButton
//        BasicArrowButton
//          MetalScrollButton
//        MetalComboBoxButton
//      JMenuItem
//        JCheckBoxMenuItem
//        JMenu
//        JRadioButtonMenuItem
//      JToggleButton
//        JCheckBox
//        JRadioButton
//    JLabel
//      BasicComboBoxRenderer
//        BasicComboBoxRenderer.UIResource
//      DefaultListCellRenderer
//        DefaultListCellRenderer.UIResource
//        MetalFileChooserUI.FileRenderer
//        MetalFileChooserUI.FilterComboBoxRenderer
//      DefaultTableCellRenderer
//        DefaultTableCellRenderer.UIResource
//      DefaultTreeCellRenderer
//    JTextComponent
//      JEditorPane
//        JTextPane
//      JTextArea
//      JTextField
//        DefaultTreeCellEditor.DefaultTextField
//        JFormattedTextField
//        JPasswordField
//  title
//    Dialog
//      FileDialog
//      JDialog
//    Frame
//      JFrame
//    TitledBorder
//  toolTipText
//    JComponent
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.border.*;  //Border,CompoundBorder,EmptyBorder,EtchedBorder,LineBorder,TitledBorder

public class Multilingual {

  //多言語化マップ
  public static HashMap<AbstractButton,HashMap<String,String>> mlnButtonTextMap;  //ボタンのテキスト
  public static HashMap<JLabel,HashMap<String,String>> mlnLabelTextMap;  //ラベルのテキスト
  public static HashMap<Dialog,HashMap<String,String>> mlnDialogTitleMap;  //ダイアログのタイトル
  public static HashMap<Frame,HashMap<String,String>> mlnFrameTitleMap;  //フレームのタイトル
  public static HashMap<JComponent,HashMap<String,String>> mlnComponentToolTipTextMap;  //コンポーネントのツールチップテキスト
  public static HashMap<SpinnerListModel,HashMap<String,java.util.List<?>>> mlnSpinnerListModelListMap;  //スピナーリストモデルのリスト
  public static HashMap<JComponent,HashMap<String,String>> mlnComponentTitledBorderMap;  //コンポーネントのタイトル付きボーダー

  //現在の言語
  public static String mlnLang;  //現在の言語
  public static boolean mlnEnglish;  //mlnLang.equals("en");
  public static boolean mlnJapanese;  //mlnLang.equals("ja");

  //mlnInit ()
  //  多言語化を初期化する
  //  コンポーネントを作る前に呼び出すこと
  public static void mlnInit () {
    //多言語化マップ
    mlnButtonTextMap = new HashMap<AbstractButton,HashMap<String,String>> ();  //ボタンのテキスト
    mlnLabelTextMap = new HashMap<JLabel,HashMap<String,String>> ();  //ラベルのテキスト
    mlnDialogTitleMap = new HashMap<Dialog,HashMap<String,String>> ();  //ダイアログのタイトル
    mlnFrameTitleMap = new HashMap<Frame,HashMap<String,String>> ();  //フレームのタイトル
    mlnComponentToolTipTextMap = new HashMap<JComponent,HashMap<String,String>> ();  //コンポーネントのツールチップテキスト
    mlnSpinnerListModelListMap = new HashMap<SpinnerListModel,HashMap<String,java.util.List<?>>> ();  //スピナーリストモデルのリスト
    mlnComponentTitledBorderMap = new HashMap<JComponent,HashMap<String,String>> ();  //コンポーネントのタイトル付きボーダー
    mlnLang = "";  //現在の言語
    String defaultLang = Locale.getDefault ().getLanguage ();  //動作環境の言語
    mlnChange (defaultLang.equals ("ja") ? "ja" : "en");
  }  //mlnInit()

  //mlnChange (lang)
  //  多言語化されたコンポーネントの言語を切り替える
  //  コンポーネントが指定された言語のテキストを持たないときは英語になる
  public static void mlnChange (String lang) {
    if (!mlnLang.equals (lang)) {  //現在の言語と異なるとき
      //現在の言語
      if (lang.equals ("ja")) {
        mlnLang = "ja";
        mlnEnglish = false;
        mlnJapanese = true;
        System.out.println ("日本語が選択されました");
      } else {
        mlnLang = "en";
        mlnEnglish = true;
        mlnJapanese = false;
        System.out.println ("English is selected");
      }
      //ボタンのテキストの言語を切り替える
      for (Map.Entry<AbstractButton,HashMap<String,String>> entry : mlnButtonTextMap.entrySet ()) {
        mlnChangeButtonText (entry.getKey (), entry.getValue (), lang);
      }
      //ラベルのテキストの言語を切り替える
      for (Map.Entry<JLabel,HashMap<String,String>> entry : mlnLabelTextMap.entrySet ()) {
        mlnChangeLabelText (entry.getKey (), entry.getValue (), lang);
      }
      //ダイアログのタイトルの言語を切り替える
      for (Map.Entry<Dialog,HashMap<String,String>> entry : mlnDialogTitleMap.entrySet ()) {
        mlnChangeDialogTitle (entry.getKey (), entry.getValue (), lang);
      }
      //フレームのタイトルの言語を切り替える
      for (Map.Entry<Frame,HashMap<String,String>> entry : mlnFrameTitleMap.entrySet ()) {
        mlnChangeFrameTitle (entry.getKey (), entry.getValue (), lang);
      }
      //コンポーネントのツールチップテキストの言語を切り替える
      for (Map.Entry<JComponent,HashMap<String,String>> entry : mlnComponentToolTipTextMap.entrySet ()) {
        mlnChangeComponentToolTipText (entry.getKey (), entry.getValue (), lang);
      }
      //スピナーリストモデルのリストの言語を切り替える
      for (Map.Entry<SpinnerListModel,HashMap<String,java.util.List<?>>> entry : mlnSpinnerListModelListMap.entrySet ()) {
        mlnChangeSpinnerListModelList (entry.getKey (), entry.getValue (), lang);
      }
      //コンポーネントのタイトル付きボーダーの言語を切り替える
      for (Map.Entry<JComponent,HashMap<String,String>> entry : mlnComponentTitledBorderMap.entrySet ()) {
        mlnChangeComponentTitledBorder (entry.getKey (), entry.getValue (), lang);
      }
    }
  }  //mlnChange(String)

  //mlnChangeButtonText (button, map, lang)
  //  ボタンのテキストの言語を切り替える
  public static void mlnChangeButtonText (AbstractButton button, HashMap<String,String> map, String lang) {
    button.setText (map.containsKey (lang) ? map.get (lang) : map.get ("en"));
  }  //mlnChangeButtonText(AbstractButton,HashMap<String,String>,String)

  //mlnChangeLabelText (label, map, lang)
  //  ラベルのテキストの言語を切り替える
  public static void mlnChangeLabelText (JLabel label, HashMap<String,String> map, String lang) {
    label.setText (map.containsKey (lang) ? map.get (lang) : map.get ("en"));
  }  //mlnChangeLabelText(JLabel,HashMap<String,String>,String)

  //mlnChangeDialogTitle (dialog, map, lang)
  //  ダイアログのタイトルの言語を切り替える
  public static void mlnChangeDialogTitle (Dialog dialog, HashMap<String,String> map, String lang) {
    dialog.setTitle (map.containsKey (lang) ? map.get (lang) : map.get ("en"));
  }  //mlnChangeDialogTitle(Dialog,HashMap<String,String>,String)

  //mlnChangeFrameTitle (frame, map, lang)
  //  フレームのタイトルの言語を切り替える
  public static void mlnChangeFrameTitle (Frame frame, HashMap<String,String> map, String lang) {
    frame.setTitle (map.containsKey (lang) ? map.get (lang) : map.get ("en"));
  }  //mlnChangeFrameTitle(Frame,HashMap<String,String>,String)

  //mlnChangeComponentToolTipText (component, map, lang)
  //  コンポーネントのツールチップテキストの言語を切り替える
  public static void mlnChangeComponentToolTipText (JComponent component, HashMap<String,String> map, String lang) {
    component.setToolTipText (map.containsKey (lang) ? map.get (lang) : map.get ("en"));
  }  //mlnChangeComponentToolTipText(JComponent,HashMap<String,String>,String)

  //mlnChangeSpinnerListModelList (model, map, lang)
  //  スピナーリストモデルのリストの言語を切り替える
  public static void mlnChangeSpinnerListModelList (SpinnerListModel model, HashMap<String,java.util.List<?>> map, String lang) {
    int index = model.getList ().indexOf (model.getValue ());  //選択されている値のインデックス
    java.util.List<?> list = map.containsKey (lang) ? map.get (lang) : map.get ("en");
    model.setList (list);  //チェンジイベントが発生する
    model.setValue (list.get (index));  //同じインデックスで選択し直す
  }  //mlnChangeSpinnerListModelList(SpinnerListModel,HashMap<String,java.util.List<?>>,String)

  //mlnChangeComponentTitledBorder (component, map, lang)
  //  コンポーネントのタイトル付きボーダーの言語を切り替える
  public static void mlnChangeComponentTitledBorder (JComponent component, HashMap<String,String> map, String lang) {
    TitledBorder titledBorder = (TitledBorder) component.getBorder ();
    titledBorder.setTitle (map.containsKey (lang) ? map.get (lang) : map.get ("en"));
    component.repaint ();  //TitledBorder.setTitle()だけだとrepaintされず次にウインドウに触れた瞬間に言語が変化するという不自然な動作になる
  }  //mlnChangeComponentTitledBorder(JComponent,HashMap<String,String>,String)

  //button = mlnText (button, lang, text, ...)
  //  ボタンに多言語のテキストを設定する
  //  アクションコマンドを英語のテキストとして用いる
  //  必要ならばテキストにニモニックを加える
  public static <T extends AbstractButton> T mlnText (T button, String... langText) {
    String enText = button.getActionCommand ();
    int mnemonic = button.getMnemonic ();
    String mnemonicText = KeyEvent.getKeyText (mnemonic);
    String mnemonicLowerCaseText = mnemonicText.toLowerCase ();
    HashMap<String,String> map = new HashMap<String,String> ();
    for (int i = langText.length > 0 && langText[0].equals ("en") ? 0 : -2; i < langText.length; i += 2) {
      String lang = i < 0 ? "en" : langText[i];
      String text = i < 0 ? enText : langText[i + 1];
      map.put (
        lang,
        mnemonic == KeyEvent.VK_UNDEFINED ||  //ニモニックがない、または
        text.toLowerCase ().indexOf (mnemonicLowerCaseText) >= 0 ? text :  //ニモニックがテキストに含まれるとき
        text + "(" + mnemonicText + ")");  //ニモニックがある、かつ、ニモニックがテキストに含まれないとき
    }
    mlnButtonTextMap.put (button, map);
    mlnChangeButtonText (button, map, mlnLang);  //現在の言語を設定する
    return button;
  }  //mlnText(T extends AbstractButton,String...)

  //label = mlnText (label, lang, text, ...)
  //  ラベルに多言語のテキストを設定する
  public static <T extends JLabel> T mlnText (T label, String... langText) {
    String enText = label.getText ();
    HashMap<String,String> map = new HashMap<String,String> ();
    for (int i = langText.length > 0 && langText[0].equals ("en") ? 0 : -2; i < langText.length; i += 2) {
      String lang = i < 0 ? "en" : langText[i];
      String text = i < 0 ? enText : langText[i + 1];
      map.put (lang, text);
    }
    mlnLabelTextMap.put (label, map);
    mlnChangeLabelText (label, map, mlnLang);  //現在の言語を設定する
    return label;
  }  //mlnText(T extends JLabel,String...)

  //dialog = mlnTitle (dialog, lang, text, ...)
  //  ダイアログに多言語のタイトルを設定する
  public static <T extends Dialog> T mlnTitle (T dialog, String... langText) {
    String enText = dialog.getTitle ();
    HashMap<String,String> map = new HashMap<String,String> ();
    for (int i = langText.length > 0 && langText[0].equals ("en") ? 0 : -2; i < langText.length; i += 2) {
      String lang = i < 0 ? "en" : langText[i];
      String text = i < 0 ? enText : langText[i + 1];
      map.put (lang, text);
    }
    mlnDialogTitleMap.put (dialog, map);
    mlnChangeDialogTitle (dialog, map, mlnLang);  //現在の言語を設定する
    return dialog;
  }  //mlnTitle(T extends Dialog,String...)

  //frame = mlnTitle (frame, lang, text, ...)
  //  フレームに多言語のタイトルを設定する
  public static <T extends Frame> T mlnTitle (T frame, String... langText) {
    String enText = frame.getTitle ();
    HashMap<String,String> map = new HashMap<String,String> ();
    for (int i = langText.length > 0 && langText[0].equals ("en") ? 0 : -2; i < langText.length; i += 2) {
      String lang = i < 0 ? "en" : langText[i];
      String text = i < 0 ? enText : langText[i + 1];
      map.put (lang, text);
    }
    mlnFrameTitleMap.put (frame, map);
    mlnChangeFrameTitle (frame, map, mlnLang);  //現在の言語を設定する
    return frame;
  }  //mlnTitle(T extends Frame,String...)

  //component = mlnToolTipText (component, lang, text, ...)
  //  コンポーネントに多言語のツールチップテキストを設定する
  public static <T extends JComponent> T mlnToolTipText (T component, String... langText) {
    String enText = component.getToolTipText ();
    HashMap<String,String> map = new HashMap<String,String> ();
    for (int i = langText.length > 0 && langText[0].equals ("en") ? 0 : -2; i < langText.length; i += 2) {
      String lang = i < 0 ? "en" : langText[i];
      String text = i < 0 ? enText : langText[i + 1];
      map.put (lang, text);
    }
    mlnComponentToolTipTextMap.put (component, map);
    mlnChangeComponentToolTipText (component, map, mlnLang);  //現在の言語を設定する
    return component;
  }  //mlnToolTipText(T extends JComponent,String...)

  //spinner = mlnList (spinner, lang, list, ...)
  //mode = mlnList (model, lang, list, ...)
  //  スピナーリストモデルに多言語のリストを設定する
  public static JSpinner mlnList (JSpinner spinner, Object... langList) {
    mlnList ((SpinnerListModel) spinner.getModel (), langList);
    return spinner;
  }  //mlnList(JSpinner,Object...)
  public static SpinnerListModel mlnList (SpinnerListModel model, Object... langList) {
    java.util.List<?> enList = model.getList ();
    HashMap<String,java.util.List<?>> map = new HashMap<String,java.util.List<?>> ();
    for (int i = langList.length > 0 && ((String) langList[0]).equals ("en") ? 0 : -2; i < langList.length; i += 2) {
      String lang = i < 0 ? "en" : (String) langList[i];
      java.util.List<?> list = i < 0 ? enList : (java.util.List<?>) langList[i + 1];
      map.put (lang, list);
    }
    mlnSpinnerListModelListMap.put (model, map);
    mlnChangeSpinnerListModelList (model, map, mlnLang);  //現在の言語を設定する
    return model;
  }  //mlnList(SpinnerListModel,Object...)

  //component = mlnTitledBorder (component, lang, text, ...)
  //  コンポーネントのタイトル付きボーダーに多言語のタイトルを設定する
  public static <T extends JComponent> T mlnTitledBorder (T component, String... langText) {
    TitledBorder titledBorder = (TitledBorder) component.getBorder ();
    String enText = titledBorder.getTitle ();
    HashMap<String,String> map = new HashMap<String,String> ();
    for (int i = langText.length > 0 && langText[0].equals ("en") ? 0 : -2; i < langText.length; i += 2) {
      String lang = i < 0 ? "en" : langText[i];
      String text = i < 0 ? enText : langText[i + 1];
      map.put (lang, text);
    }
    mlnComponentTitledBorderMap.put (component, map);
    mlnChangeComponentTitledBorder (component, map, mlnLang);  //現在の言語を設定する
    return component;
  }  //mlnTitledBorder(T extends JComponent,String...)

}  //class Multilingual



