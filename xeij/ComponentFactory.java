//========================================================================================
//  ComponentFactory.java
//    en:Component factory -- It creates and changes components.
//    ja:コンポーネントファクトリー -- コンポーネントの作成と変更を行います。
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,InterruptedException,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.net.*;  //MalformedURLException,URI,URL
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,LinkedList,TimeZone,Timer,TimerTask,TreeMap
import java.util.regex.*;  //Matcher,Pattern
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.border.*;  //Border,CompoundBorder,EmptyBorder,EtchedBorder,LineBorder,TitledBorder
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.plaf.metal.*;  //MetalLookAndFeel,MetalTheme,OceanTheme
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument

public class ComponentFactory {


  //--------------------------------------------------------------------------------
  //コンポーネントにリスナーを追加する

  //button = addListener (button, listener)
  //  ボタンにアクションリスナーを追加する
  public static <T extends AbstractButton> T addListener (T button, ActionListener listener) {
    if (listener != null) {
      button.addActionListener (listener);
    }
    return button;
  }  //addListener(T extends AbstractButton,ActionListener)

  //comboBox = addListener (comboBox, listener)
  //  コンボボックスにアクションリスナーを追加する
  public static <T extends JComboBox<String>> T addListener (T comboBox, ActionListener listener) {
    if (listener != null) {
      comboBox.addActionListener (listener);
    }
    return comboBox;
  }  //addListener(T extends JComboBox,ActionListener)

  //slider = addListener (slider, listener)
  //  スライダーにチェンジリスナーを追加する
  public static <T extends JSlider> T addListener (T slider, ChangeListener listener) {
    if (listener != null) {
      slider.addChangeListener (listener);
    }
    return slider;
  }  //addListener(T extends JSlider,ActionListener)

  //spinner = addListener (spinner, listener)
  //  スピナーにチェンジリスナーを追加する
  public static <T extends JSpinner> T addListener (T spinner, ChangeListener listener) {
    if (listener != null) {
      spinner.addChangeListener (listener);
    }
    return spinner;
  }  //addListener(T extends JSpinner,ChangeListener)

  //scrollList = addListener (scrollList, listener)
  //  スクロールリストにリストセレクションリスナーを追加する
  public static <T extends ScrollList> T addListener (T scrollList, ListSelectionListener listener) {
    if (listener != null) {
      scrollList.addListSelectionListener (listener);
    }
    return scrollList;
  }  //addListener(T extends ScrollList,ListSelectionListener)

  //component = addListener (component, listener)
  //  コンポーネントにフォーカスリスナーを追加する
  public static <T extends Component> T addListener (T component, FocusListener listener) {
    if (listener != null) {
      component.addFocusListener (listener);
    }
    return component;
  }  //addListener(T extends Component,FocusListener)

  //component = addListener (component, listener)
  //  コンポーネントにキーリスナーを追加する
  public static <T extends Component> T addListener (T component, KeyListener listener) {
    if (listener != null) {
      component.addKeyListener (listener);
    }
    return component;
  }  //addListener(T extends Component,KeyListener)

  //component = addListener (component, listener)
  //  コンポーネントにコンポーネントリスナーを追加する
  public static <T extends Component> T addListener (T component, ComponentListener listener) {
    if (listener != null) {
      component.addComponentListener (listener);
    }
    return component;
  }  //addListener(T extends Component,ComponentListener)

  //component = addListener (component, listener)
  //  コンポーネントにマウスリスナー、マウスモーションリスナー、マウスホイールリスナーを追加する
  public static <T extends Component> T addListener (T component, MouseAdapter listener) {
    if (listener != null) {
      component.addMouseListener (listener);
      component.addMouseMotionListener (listener);
      component.addMouseWheelListener (listener);
    }
    return component;
  }  //addListener(T extends Component,MouseAdapter)

  //component = addListener (component, listener)
  //  コンポーネントにマウスリスナーを追加する
  public static <T extends Component> T addListener (T component, MouseListener listener) {
    if (listener != null) {
      component.addMouseListener (listener);
    }
    return component;
  }  //addListener(T extends Component,MouseListener)

  //component = addListener (component, listener)
  //  コンポーネントにマウスモーションリスナーを追加する
  public static <T extends Component> T addListener (T component, MouseMotionListener listener) {
    if (listener != null) {
      component.addMouseMotionListener (listener);
    }
    return component;
  }  //addListener(T extends Component,MouseMotionListener)

  //component = addListener (component, listener)
  //  コンポーネントにマウスホイールリスナーを追加する
  public static <T extends Component> T addListener (T component, MouseWheelListener listener) {
    if (listener != null) {
      component.addMouseWheelListener (listener);
    }
    return component;
  }  //addListener(T extends Component,MouseWheelListener)

  //window = addListener (window, listener)
  //  ウインドウにウインドウリスナー、ウインドウステートリスナー、ウインドウフォーカスリスナーを追加する
  public static <T extends Window> T addListener (T window, WindowAdapter listener) {
    if (listener != null) {
      window.addWindowListener (listener);
      window.addWindowStateListener (listener);
      window.addWindowFocusListener (listener);
    }
    return window;
  }  //addListener(T extends Window,WindowAdapter)

  //window = addListener (window, listener)
  //  ウインドウにウインドウリスナーを追加する
  public static <T extends Window> T addListener (T window, WindowListener listener) {
    if (listener != null) {
      window.addWindowListener (listener);
    }
    return window;
  }  //addListener(T extends Window,WindowListener)

  //window = addListener (window, listener)
  //  ウインドウにウインドウステートリスナーを追加する
  public static <T extends Window> T addListener (T window, WindowStateListener listener) {
    if (listener != null) {
      window.addWindowStateListener (listener);
    }
    return window;
  }  //addListener(T extends Window,WindowStateListener)

  //window = addListener (window, listener)
  //  ウインドウにウインドウフォーカスリスナーを追加する
  public static <T extends Window> T addListener (T window, WindowFocusListener listener) {
    if (listener != null) {
      window.addWindowFocusListener (listener);
    }
    return window;
  }  //addListener(T extends Window,WindowFocusListener)

  //textComponent = addListener (textComponent, listener)
  //  テキストコンポーネントにキャレットリスナーを追加する
  public static <T extends JTextComponent> T addListener (T textComponent, CaretListener listener) {
    if (listener != null) {
      textComponent.addCaretListener (listener);
    }
    return textComponent;
  }  //addListener(T extends JTextComponent,CaretListener)


  //--------------------------------------------------------------------------------
  //コンポーネントに属性を追加する
  //  ジェネリクスを用いてパラメータのコンポーネントをクラスを変えずにそのまま返すメソッドを定義する
  //  コンポーネントのインスタンスメソッドがコンポーネント自身を返してくれると、
  //  メソッドチェーンが組めて括弧をネストする必要がなくなりコードが読みやすくなるのだが、
  //  元のクラスのまま利用できなければ既存のメソッドの返却値をいちいちキャストしなければならず、
  //  結局括弧が増えることになる

  //component = setToolTipText (component, toolTipText)
  public static <T extends JComponent> T setToolTipText (T component, String toolTipText) {
    component.setToolTipText (toolTipText);
    return component;
  }  //setToolTipText(T extends JComponent,String)

  //component = setName (component, name)
  public static <T extends Component> T setName (T component, String name) {
    component.setName (name);
    return component;
  }  //setName(T extends Component,String)

  //component = setVisible (component, visible)
  public static <T extends Component> T setVisible (T component, boolean visible) {
    component.setVisible (visible);
    return component;
  }  //setVisible(T extends Component,boolean)

  //component = setColor (component, foreground, background)
  public static <T extends Component> T setColor (T component, Color foreground, Color background) {
    component.setBackground (background);
    component.setForeground (foreground);
    return component;
  }  //setColor(T extends Component,Color,Color)

  //component = setFont (component, font)
  public static <T extends Component> T setFont (T component, Font font) {
    component.setFont (font);
    return component;
  }  //setFont(T extends Component,Font)

  //component = bold (component)
  //component = italic (component)
  //component = boldItalic (component)
  public static <T extends Component> T bold (T component) {
    return setFont (component, component.getFont ().deriveFont (Font.BOLD));
  }  //bold(T extends Component)
  public static <T extends Component> T italic (T component) {
    return setFont (component, component.getFont ().deriveFont (Font.ITALIC));
  }  //italic(T extends Component)
  public static <T extends Component> T boldItalic (T component) {
    return setFont (component, component.getFont ().deriveFont (Font.BOLD | Font.ITALIC));
  }  //boldItalic(T extends Component)

  //component = pointSize (component)
  public static <T extends Component> T pointSize (T component, int size) {
    return setFont (component, component.getFont ().deriveFont ((float) size));
  }

  //component = setEnabled (component, enabled)
  //  コンポーネントが有効かどうか指定する
  public static <T extends Component> T setEnabled (T component, boolean enabled) {
    component.setEnabled (enabled);
    return component;
  }  //setEnabled(T extends Component,boolean)

  //component = setMaximumSize (component, width, height)
  //  コンポーネントの最大サイズを指定する
  public static <T extends Component> T setMaximumSize (T component, int width, int height) {
    component.setMaximumSize (new Dimension (width, height));
    return component;
  }  //setMaximumSize(T extends Component,int,int)

  //component = setMinimumSize (component, width, height)
  //  コンポーネントの最小サイズを指定する
  public static <T extends Component> T setMinimumSize (T component, int width, int height) {
    component.setMinimumSize (new Dimension (width, height));
    return component;
  }  //setMinimumSize(T extends Component,int,int)

  //component = setPreferredSize (component, width, height)
  //  コンポーネントの推奨サイズを指定する
  public static <T extends Component> T setPreferredSize (T component, int width, int height) {
    component.setPreferredSize (new Dimension (width, height));
    return component;
  }  //setPreferredSize(T extends Component,int,int)

  //component = setFixedSize (component, width, height)
  //  コンポーネントの固定サイズを指定する
  public static <T extends Component> T setFixedSize (T component, int width, int height) {
    Dimension d = new Dimension (width, height);
    component.setMinimumSize (d);
    component.setMaximumSize (d);
    component.setPreferredSize (d);
    return component;
  }  //setFixedSize(T extends Component,int,int)

  //component = setEmptyBorder (component, top, left, bottom, right)
  //  コンポーネントに透過ボーダーを付ける
  public static <T extends JComponent> T setEmptyBorder (T component, int top, int left, int bottom, int right) {
    component.setBorder (new EmptyBorder (top, left, bottom, right));
    return component;
  }  //setEmptyBorder(T extends JComponent,int,int,int,int)

  //component = setLineBorder (component)
  //  コンポーネントにラインボーダーを付ける
  public static <T extends JComponent> T setLineBorder (T component) {
    component.setBorder (new LineBorder (MetalLookAndFeel.getSeparatorForeground (), 1));  //primary1
    return component;
  }  //setLineBorder(T extends JComponent)

  //component = setTitledLineBorder (component, title)
  //  コンポーネントにタイトル付きラインボーダーを付ける
  public static <T extends JComponent> T setTitledLineBorder (T component, String title) {
    component.setBorder (new TitledBorder (new LineBorder (MetalLookAndFeel.getSeparatorForeground (), 1), title));  //primary1
    return component;
  }  //setTitledLineBorder(T extends JComponent,String)

  //component = setEtchedBorder (component, title)
  //  コンポーネントにエッチングボーダーを付ける
  public static <T extends JComponent> T setEtchedBorder (T component) {
    component.setBorder (new EtchedBorder (new Color (LnF.lnfRGB[10]), new Color (LnF.lnfRGB[14])));
    return component;
  }  //setEtchedBorder(T extends JComponent)

  //component = setTitledEtchedBorder (component, title)
  //  コンポーネントにタイトル付きエッチングボーダーを付ける
  public static <T extends JComponent> T setTitledEtchedBorder (T component, String title) {
    component.setBorder (new TitledBorder (new EtchedBorder (), title));
    return component;
  }  //setTitledEtchedBorder(T extends JComponent,String)

  //parent = addComponents (parent, component, ...)
  //  コンポーネントにコンポーネントを追加する
  public static <T extends JComponent> T addComponents (T parent, Component... components) {
    for (Component component : components) {
      if (component != null) {
        parent.add (component);
      }
    }
    return parent;
  }  //addComponents(T extends JComponent,Component...)

  //parent = removeComponents (parent, child, ...)
  //  コンポーネントからコンポーネントを取り除く
  public static <T extends JComponent> T removeComponents (T parent, Component... components) {
    for (Component component : components) {
      if (component != null) {
        parent.remove (component);
      }
    }
    return parent;
  }  //removeComponents(T extends JComponent,Component...)

  //ボタン

  //button = setText (button, text)
  public static <T extends AbstractButton> T setText (T button, String text) {
    button.setText (text);
    return button;
  }  //setText(T extends AbstractButton,String)

  //button = setHorizontalAlignment (button, alignment)
  //  SwingConstants.RIGHT
  //  SwingConstants.LEFT
  //  SwingConstants.CENTER (デフォルト)
  //  SwingConstants.LEADING
  //  SwingConstants.TRAILING
  public static <T extends AbstractButton> T setHorizontalAlignment (T button, int alignment) {
    button.setHorizontalAlignment (alignment);
    return button;
  }  //setHorizontalAlignment(T extends AbstractButton,int)

  //button = setVerticalAlignment (button, alignment)
  //  SwingConstants.CENTER (デフォルト)
  //  SwingConstants.TOP
  //  SwingConstants.BOTTOM
  public static <T extends AbstractButton> T setVerticalAlignment (T button, int alignment) {
    button.setVerticalAlignment (alignment);
    return button;
  }  //setVerticalAlignment(T extends AbstractButton,int)

  //テキストフィールド

  //button = setHorizontalAlignment (textField, alignment)
  //  JTextField.RIGHT
  //  JTextField.LEFT (デフォルト)
  //  JTextField.CENTER
  //  JTextField.LEADING
  //  JTextField.TRAILING
  public static <T extends JTextField> T setHorizontalAlignment (T textField, int alignment) {
    textField.setHorizontalAlignment (alignment);
    return textField;
  }  //setHorizontalAlignment(T extends JTextField,int)

  //テキストコンポーネント

  //component = setEditable (component, enabled)
  //  コンポーネントが編集可能かどうか指定する
  public static <T extends JTextComponent> T setEditable (T component, boolean enabled) {
    component.setEditable (enabled);
    return component;
  }  //setEditable(T extends JTextComponent,boolean)


  //--------------------------------------------------------------------------------
  //フレームとダイアログを作る
  //  ウインドウリスナー
  //    ウインドウを開いたとき  activated,opened
  //    フォーカスを失ったとき  deactivated
  //    フォーカスを取得したとき  activated
  //    ウインドウをアイコン化したとき  iconified,[deactivated]
  //    ウインドウを元のサイズに戻したとき  deiconified,activated
  //    ウインドウを閉じたとき  closing,[deactivated],closed

  //frame = createFrame (title, mnbMenuBar, component)
  //  フレームを作る
  //  すぐに開く
  //  デフォルトのクローズボタンの動作はEXIT_ON_CLOSE
  //  クローズボタンがクリックされたとき後始末を行なってから終了するとき
  //    frame = createFrame (title, mnbMenuBar, component);
  //    frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
  //    frame.addWindowListener (new WindowAdapter () {
  //      @Override public void windowClosed (WindowEvent we) {
  //        後始末;
  //        System.exit (0);
  //      }
  //    });
  public static JFrame createFrame (String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new JFrame (title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    //frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    frame.setVisible (true);
    return frame;
  }  //createFrame(String,JMenuBar,JComponent)

  public static JFrame createSubFrame (String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new JFrame (title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.HIDE_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    return frame;
  }  //createSubFrame(String,JMenuBar,JComponent)

  public static JFrame createRestorableFrame (String key, String title, JMenuBar mnbMenuBar, JComponent component) {
    JFrame frame = new RestorableFrame (key, title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    //frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.pack ();
    frame.setVisible (true);
    return frame;
  }  //createRestorableFrame(String,String,JMenuBar,JComponent)

  public static JFrame createRestorableSubFrame (String key, String title, JMenuBar mnbMenuBar, JComponent component) {
    return createRestorableSubFrame (key, title, mnbMenuBar, component, true);
  }
  public static JFrame createRestorableSubFrame (String key, String title, JMenuBar mnbMenuBar, JComponent component, boolean resizable) {
    JFrame frame = new RestorableFrame (key, title);
    frame.setUndecorated (true);  //ウインドウの枠を消す
    frame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    //frame.setLocationByPlatform (true);
    frame.setDefaultCloseOperation (JFrame.HIDE_ON_CLOSE);
    if (mnbMenuBar != null) {
      frame.setJMenuBar (mnbMenuBar);
    }
    //frame.getContentPane ().add (component, BorderLayout.CENTER);
    component.setOpaque (true);
    frame.setContentPane (component);
    frame.setResizable (resizable);
    frame.pack ();
    return frame;
  }  //createRestorableSubFrame(String,String,JMenuBar,JComponent)

  //dialog = createModalDialog (owner, title, component)
  //dialog = createModelessDialog (owner, title, component)
  //dialog = createDialog (owner, title, component, modal)
  //  ダイアログを作る(ownerを指定する)
  //  まだ開かない
  //  デフォルトのクローズボタンの動作はHIDE_ON_CLOSE
  //  閉じてもdialog.setVisible(true)で再表示できる
  public static JDialog createModalDialog (Frame owner, String title, JComponent component) {
    return createDialog (owner, title, true, component);
  }  //createModalDialog(Frame,String,JComponent)
  public static JDialog createModelessDialog (Frame owner, String title, JComponent component) {
    return createDialog (owner, title, false, component);
  }  //createModelessDialog(Frame,String,JComponent)
  public static JDialog createDialog (Frame owner, String title, boolean modal, JComponent component) {
    JDialog dialog = new JDialog (owner, title, modal);
    dialog.setUndecorated (true);  //ウインドウの枠を消す
    dialog.getRootPane ().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    dialog.setAlwaysOnTop (modal);  //モーダルのときは常に手前に表示、モードレスのときは奥に移動できる
    dialog.setLocationByPlatform (true);
    dialog.setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
    dialog.getContentPane ().add (component, BorderLayout.CENTER);
    dialog.pack ();
    dialog.setVisible (false);
    return dialog;
  }  //createDialog(Frame,String,boolean,JComponent)


  //--------------------------------------------------------------------------------
  //パネルを作る

  //box = createHorizontalBox (component, ...)
  //  コンポーネントを横に並べるボックスを作る
  public static Box createHorizontalBox (Component... components) {
    return addComponents (Box.createHorizontalBox (), components);
  }  //createHorizontalBox(Component...)

  //box = createVerticalBox (component, ...)
  //  コンポーネントを縦に並べるボックスを作る
  public static Box createVerticalBox (Component... components) {
    return addComponents (Box.createVerticalBox (), components);
  }  //createVerticalBox(Component...)

  //box = createGlueBox (component)
  //box = createGlueBox (orientation, component)
  //  コンポーネントを引き伸ばさず指定された方向に寄せて表示する
  //  component自身がmaximumSizeを持っていること
  //  componentがBorderLayoutでCENTERがmaximumSizeを持っているのではダメ
  //  orientation
  //    SwingConstants.NORTH_WEST SwingConstants.NORTH  SwingConstants.NORTH_EAST
  //    SwingConstants.WEST       SwingConstants.CENTER SwingConstants.EAST
  //    SwingConstants.SOUTH_WEST SwingConstants.SOUTH  SwingConstants.SOUTH_EAST
  public static Box createGlueBox (JComponent component) {
    return createGlueBox (SwingConstants.CENTER, component);
  }  //createGlueBox(JComponent)
  public static Box createGlueBox (int orientation, JComponent component) {
    Box box = (orientation == SwingConstants.NORTH_WEST ||
               orientation == SwingConstants.WEST ||
               orientation == SwingConstants.SOUTH_WEST ?
               createHorizontalBox (component, Box.createHorizontalGlue ()) :
               orientation == SwingConstants.NORTH_EAST ||
               orientation == SwingConstants.EAST ||
               orientation == SwingConstants.SOUTH_EAST ?
               createHorizontalBox (Box.createHorizontalGlue (), component) :
               createHorizontalBox (Box.createHorizontalGlue (), component, Box.createHorizontalGlue ()));
    return (orientation == SwingConstants.NORTH_WEST ||
            orientation == SwingConstants.NORTH ||
            orientation == SwingConstants.NORTH_EAST ?
            createVerticalBox (box, Box.createVerticalGlue ()) :
            orientation == SwingConstants.SOUTH_WEST ||
            orientation == SwingConstants.SOUTH ||
            orientation == SwingConstants.SOUTH_EAST ?
            createVerticalBox (Box.createVerticalGlue (), box) :
            createVerticalBox (Box.createVerticalGlue (), box, Box.createVerticalGlue ()));
  }  //createGlueBox(int,JComponent)

  //panel = createFlowPanel (component, ...)
  //panel = createFlowPanel (align, component, ...)
  //panel = createFlowPanel (hgap ,vgap, component, ...)
  //panel = createFlowPanel (align, hgap, vgap, component, ...)
  //  FlowLayoutのパネルを作る
  //  align
  //    FlowLayout.CENTER  中央揃え
  //    FlowLayout.LEFT    左揃え
  //    FlowLayout.RIGHT   右揃え
  public static JPanel createFlowPanel (Component... components) {
    return createFlowPanel (FlowLayout.LEFT, 0, 0, components);
  }  //createFlowPanel(Component...)
  public static JPanel createFlowPanel (int align, Component... components) {
    return createFlowPanel (align, 0, 0, components);
  }  //createFlowPanel(int,Component...)
  public static JPanel createFlowPanel (int hgap, int vgap, Component... components) {
    return createFlowPanel (FlowLayout.LEFT, hgap, vgap, components);
  }  //createFlowPanel(int,int,Component...)
  public static JPanel createFlowPanel (int align, int hgap, int vgap, Component... components) {
    JPanel panel = new JPanel (new FlowLayout (align, hgap, vgap));
    panel.setOpaque (true);
    return addComponents (panel, components);
  }  //createFlowPanel(int,int,int,Component...)

  //panel = createBorderPanel (component, ...)
  //panel = createBorderPanel (hgap, vgap, component, ...)
  //  BorderLayoutのパネルを作る
  //  コンポーネントをCENTER,NORTH,WEST,SOUTH,EASTの順序で指定する
  //  末尾のコンポーネントを省略するか途中のコンポーネントにnullを指定するとその部分は設定されない
  public static JPanel createBorderPanel (JComponent... components) {
    return createBorderPanel (0, 0, components);
  }  //createBorderPanel(JComponent...)
  public static JPanel createBorderPanel (int hgap, int vgap, JComponent... components) {
    JPanel panel = new JPanel (new BorderLayout (hgap, vgap));
    panel.setOpaque (true);
    if (components.length >= 1) {
      if (components[0] != null) {
        panel.add (components[0], BorderLayout.CENTER);
      }
      if (components.length >= 2) {
        if (components[1] != null) {
          panel.add (components[1], BorderLayout.NORTH);
        }
        if (components.length >= 3) {
          if (components[2] != null) {
            panel.add (components[2], BorderLayout.WEST);
          }
          if (components.length >= 4) {
            if (components[3] != null) {
              panel.add (components[3], BorderLayout.SOUTH);
            }
            if (components.length >= 5 && components[4] != null) {
              panel.add (components[4], BorderLayout.EAST);
            }
          }
        }
      }
    }
    return panel;
  }  //createBorderPanel(int,int,JComponent...)

  //panel = createGridPanel (colCount, rowCount, gridStyles, colStyless, rowStyless, cellStyless, objectArray, ...)
  //  GridBagLayoutのパネルを作る
  //    colCount          列数
  //    rowCount          行数
  //    gridStyles        すべてのセルの共通のスタイル
  //    colStyles         列毎の共通のスタイル。列の区切りは";"。スタイルの区切りは","
  //    rowStyles         行毎の共通のスタイル。行の区切りは";"。スタイルの区切りは","
  //    cellStyles        個々のセルのスタイル。セルの区切りは";"。スタイルの区切りは","。上または左のセルが重なっているセルは含まない
  //                      colSpan        列数
  //                      rowSpan        行数
  //                      width          幅
  //                      height         高さ
  //                      widen          幅をいっぱいまで伸ばす
  //                      lengthen       高さをいっぱいまで伸ばす
  //                      center         左右に寄せない
  //                      left           左に寄せる
  //                      right          右に寄せる
  //                      middle         上下に寄せない
  //                      top            上に寄せる
  //                      bottom         下に寄せる
  //                      paddingTop     上端のパディング
  //                      paddingRight   右端のパディング
  //                      paddingBottom  下端のパディング
  //                      paddingLeft    左端のパディング
  //                      bold           ボールド
  //                      italic         イタリック
  //    objectArray, ...  セルに表示するオブジェクトの配列。長さが列数×行数よりも少ないときは上または左のセルが重なっているセルが詰められていると判断される。このときcellStylesも詰められたインデックスで参照される
  //  gridStyles;colStyles;rowStyles;cellStylesの順序で個々のセルに対応するスタイルが連結される
  //  同時に指定できないスタイルは後から指定した方が優先される
  public static JPanel createGridPanel (int colCount, int rowCount, String gridStyles, String colStyless, String rowStyless, String cellStyless, Object... objectArray) {
    String[] colStylesArray = (colStyless != null ? colStyless : "").split (";");
    String[] rowStylesArray = (rowStyless != null ? rowStyless : "").split (";");
    String[] cellStylesArray = (cellStyless != null ? cellStyless : "").split (";");
    int cellCount = colCount * rowCount;
    //Component[] componentArray = new Component[cellCount];  //セルのオブジェクト。上または左のセルが重なっているセルは含まない
    boolean[] cellFilledArray = new boolean[cellCount];  //セルが充填済みかどうか。colCount*rowCountのセルをすべて含む
    GridBagLayout gridbag = new GridBagLayout ();
    JPanel panel = new JPanel (gridbag);
    GridBagConstraints c = new GridBagConstraints ();
    int objectIndex = 0;  //objectArrayとcellStylesArrayの詰められたインデックス
    boolean objectClosed = objectArray.length < cellCount;  //objectArrayとcellStylesArrayが詰められている(上または左のセルが重なっているセルを含まない)。objectArray[objectClosed?objectIndex:cellIndex]
    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
      for (int colIndex = 0; colIndex < colCount; colIndex++) {
        int cellIndex = colIndex + colCount * rowIndex;  //セルのインデックス。colCount*rowCountのセルをすべて含む
        if (cellFilledArray[cellIndex]) {  //充填済み
          continue;
        }
        int colSpan = 1;
        int rowSpan = 1;
        int width = 1;
        int height = 1;
        int fill = 0;  //1=widen,2=lengthen
        int anchor = 0;  //1=left,2=right,4=top,8=bottom
        int paddingTop = 0;
        int paddingRight = 0;
        int paddingBottom = 0;
        int paddingLeft = 0;
        int fontStyle = 0;  //1=bold,2=italic
        for (String style : ((gridStyles != null ? gridStyles : "") + "," +
                             (colIndex < colStylesArray.length ? colStylesArray[colIndex] : "") + "," +
                             (rowIndex < rowStylesArray.length ? rowStylesArray[rowIndex] : "") + "," +
                             ((objectClosed ? objectIndex : cellIndex) < cellStylesArray.length ? cellStylesArray[objectClosed ? objectIndex : cellIndex] : "")).split (",")) {
          String[] keyValue = style.split ("=");
          String key = keyValue.length < 1 ? "" : keyValue[0].trim ();
          int value = keyValue.length < 2 ? 1 : Integer.parseInt (keyValue[1]);
          switch (key) {
          case "colSpan":  //列数
            colSpan = value;
            break;
          case "rowSpan":  //行数
            rowSpan = value;
            break;
          case "width":  //幅
            width = value;
            break;
          case "height":  //高さ
            height = value;
            break;
          case "widen":  //幅をいっぱいまで伸ばす
            fill |= 1;
            break;
          case "lengthen":  //高さをいっぱいまで伸ばす
            fill |= 2;
            break;
          case "center":  //左右に寄せない
            anchor &= ~0b0011;
            break;
          case "left":  //左に寄せる
            anchor = anchor & ~0b0011 | 0b0001;
            break;
          case "right":  //右に寄せる
            anchor = anchor & ~0b0011 | 0b0010;
            break;
          case "middle":  //上下に寄せない
            anchor &= ~0b1100;
            break;
          case "top":  //上に寄せる
            anchor = anchor & ~0b1100 | 0b0100;
            break;
          case "bottom":  //下に寄せる
            anchor = anchor & ~0b1100 | 0b1000;
            break;
          case "paddingTop":  //上端のパディング
            paddingTop = value;
            break;
          case "paddingRight":  //右端のパディング
            paddingRight = value;
            break;
          case "paddingBottom":  //下端のパディング
            paddingBottom = value;
            break;
          case "paddingLeft":  //左端のパディング
            paddingLeft = value;
            break;
          case "bold":  //ボールド
            fontStyle |= 1;
            break;
          case "italic":  //イタリック
            fontStyle |= 2;
            break;
          }
        }
        c.gridx = colIndex;
        c.gridy = rowIndex;
        c.gridwidth = colSpan;
        c.gridheight = rowSpan;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = (fill == 1 ? GridBagConstraints.HORIZONTAL :
                  fill == 2 ? GridBagConstraints.VERTICAL :
                  fill == 3 ? GridBagConstraints.BOTH :
                  GridBagConstraints.NONE);
        c.anchor = (anchor == 0b0001 ? GridBagConstraints.WEST :
                    anchor == 0b0010 ? GridBagConstraints.EAST :
                    anchor == 0b0100 ? GridBagConstraints.NORTH :
                    anchor == 0b1000 ? GridBagConstraints.SOUTH :
                    anchor == 0b0101 ? GridBagConstraints.NORTHWEST :
                    anchor == 0b0110 ? GridBagConstraints.NORTHEAST :
                    anchor == 0b1001 ? GridBagConstraints.SOUTHWEST :
                    anchor == 0b1010 ? GridBagConstraints.SOUTHEAST :
                    GridBagConstraints.CENTER);
        c.insets = new Insets (paddingTop, paddingLeft, paddingBottom, paddingRight);
        Object object = (objectClosed ? objectIndex : cellIndex) < objectArray.length ? objectArray[objectClosed ? objectIndex : cellIndex] : null;
        Component component;
        if (object == null) {
          component = new JPanel ();
        } else if (object instanceof String) {
          String string = (String) object;
          component = string.startsWith ("http://") || string.startsWith ("https://") ? createAnchor (string, string) : createLabel ((String) object);
        } else if (object instanceof Component) {
          component = (Component) object;
        } else {
          component = new JPanel ();
        }
        if (component instanceof JLabel) {
          JLabel label = (JLabel) component;
          if (fontStyle == 1) {
            bold (label);
          } else if (fontStyle == 2) {
            italic (label);
          } else if (fontStyle == 3) {
            boldItalic (label);
          }
        }

        component.setMinimumSize (new Dimension (width, height));
        if (width > 1 || height > 1) {
          component.setPreferredSize (new Dimension (width, height));
        }
        gridbag.setConstraints (component, c);
        panel.add (component);
        //componentArray[objectIndex] = component;
        for (int y = 0; y < rowSpan; y++) {
          for (int x = 0; x < colSpan; x++) {
            cellFilledArray[(colIndex + x) + colCount * (rowIndex + y)] = true;
          }
        }

        objectIndex++;
      }  //for colIndex
    }  //for rowIndex
    return panel;
  }  //createGridPanel(int,int,String,String,String,String,Object...)

  //scrollPane = createScrollPane (view)
  //scrollPane = createScrollPane (view, vsbPolicy, hsbPolicy)
  //  スクロールペインを作る
  //  推奨サイズが必要なので通常はsetPreferredSize (createScrollPane (view), width, height)の形式で作る
  //  vsbPolicy
  //    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
  //    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  //    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
  //  hsbPolicy
  //    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
  //    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  //    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
  public static JScrollPane createScrollPane (Component view) {
    return createScrollPane (view,
                             ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }  //createScrollPane(Component)
  public static JScrollPane createScrollPane (Component view, int vsbPolicy, int hsbPolicy) {
    return new JScrollPane (view, vsbPolicy, hsbPolicy);
  }  //createScrollPane(Component,int,int)

  //splitPane = createHorizontalSplitPane (component, ...)
  //splitPane = createVerticalSplitPane (component, ...)
  //splitPane = createSplitPane (orientation, component, ...)
  //  スプリットペインを作る
  //  orientation
  //    JSplitPane.HORIZONTAL_SPLIT
  //    JSplitPane.VERTICAL_SPLIT
  public static JSplitPane createHorizontalSplitPane (Component... components) {
    return createSplitPane (JSplitPane.HORIZONTAL_SPLIT, components);
  }  //createHorizontalSplitPane(Component...)
  public static JSplitPane createVerticalSplitPane (Component... components) {
    return createSplitPane (JSplitPane.VERTICAL_SPLIT, components);
  }  //createVerticalSplitPane(Component...)
  public static JSplitPane createSplitPane (int orientation, Component... components) {
    JSplitPane splitPane = new JSplitPane (orientation, true, components[0], components[1]);
    for (int i = 2; i < components.length; i++) {
      splitPane = new JSplitPane (orientation, true, splitPane, components[i]);  //((0,1),2)...
    }
    return splitPane;
  }  //createSplitPane(int,Component...)


  //--------------------------------------------------------------------------------
  //セパレータを作る

  //separator = createHorizontalSeparator ()
  //separator = createVerticalSeparator ()
  //  セパレータを作る
  public static JSeparator createHorizontalSeparator () {
    return new JSeparator (SwingConstants.HORIZONTAL);
  }  //createHorizontalSeparator()
  public static JSeparator createVerticalSeparator () {
    return new JSeparator (SwingConstants.VERTICAL);
  }  //createVerticalSeparator()


  //--------------------------------------------------------------------------------
  //ラベルを作る

  //label = createLabel (enText)
  //label = createLabel (enText, alignment)
  //  ラベルを作る
  public static JLabel createLabel (String enText) {
    return createLabel (enText, SwingConstants.CENTER);
  }  //createLabel(String)
  public static JLabel createLabel (String enText, int alignment) {
    JLabel label = new JLabel (enText);
    label.setForeground (MetalLookAndFeel.getBlack ());  //black
    if (alignment == SwingConstants.NORTH_WEST ||
        alignment == SwingConstants.NORTH ||
        alignment == SwingConstants.NORTH_EAST ||
        alignment == SwingConstants.TOP) {
      label.setVerticalAlignment (SwingConstants.TOP);
    } else if (alignment == SwingConstants.SOUTH_WEST ||
        alignment == SwingConstants.SOUTH ||
        alignment == SwingConstants.SOUTH_EAST ||
        alignment == SwingConstants.BOTTOM) {
      label.setVerticalAlignment (SwingConstants.BOTTOM);
    } else if (alignment == SwingConstants.CENTER) {
      label.setVerticalAlignment (SwingConstants.CENTER);
    }
    if (alignment == SwingConstants.NORTH_WEST ||
        alignment == SwingConstants.WEST ||
        alignment == SwingConstants.SOUTH_WEST ||
        alignment == SwingConstants.LEFT) {
      label.setHorizontalAlignment (SwingConstants.LEFT);
    } else if (alignment == SwingConstants.NORTH_EAST ||
        alignment == SwingConstants.EAST ||
        alignment == SwingConstants.SOUTH_EAST ||
        alignment == SwingConstants.RIGHT) {
      label.setHorizontalAlignment (SwingConstants.RIGHT);
    } else if (alignment == SwingConstants.CENTER) {
      label.setHorizontalAlignment (SwingConstants.CENTER);
    }
    return label;
  }  //createLabel(String,int)

  //label = createIconLabel (image)
  //  アイコンラベルを作る
  public static JLabel createIconLabel (Image image) {
    JLabel label = new JLabel (new ImageIcon (image));
    label.setBorder (new EmptyBorder (1, 1, 1, 1));  //アイコンボタンと同じサイズにする
    return label;
  }  //createIconLabel(Image)

  public static JLabel createChapterLabel (String enText) {
    return createLargerLabel (enText, 20);
  }
  public static JLabel createSectionLabel (String enText) {
    return createLargerLabel (enText, 18);
  }
  public static JLabel createSubsectionLabel (String enText) {
    return createLargerLabel (enText, 16);
  }
  public static JLabel createLargerLabel (String enText, int size) {
    return setEmptyBorder (
      bold (pointSize (
        createLabel (enText),
        LnF.lnfFontSize * size / 14)),
      size / 2, 0, size / 2, 0);  //top,left,bottom,right
  }


  //--------------------------------------------------------------------------------
  //アンカーを作る
  //  下線付きラベル
  //  マウスカーソルは手の形
  //  クリックされたらあらかじめ設定されたURIをブラウザに渡す

  //label = createAnchor (enText, uri)
  //  アンカーを作る
  public static boolean isObsoleteURI (String uri) {
    return uri.startsWith ("http://www.nifty.ne.jp/forum/");  //"fsharp/"。リンク先が存在しないURI
  }  //isObsoleteURI(String)
  public static JLabel createAnchor (String enText, String uri) {
    JLabel label = new UnderlinedLabel (enText);  //下線付きラベル
    label.setForeground (MetalLookAndFeel.getBlack ());  //black
    if (uri != null) {
      if (isObsoleteURI (uri)) {
        uri = "https://web.archive.org/web/" + "*" + "/" + uri;
      }
      label.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));  //マウスカーソルは手の形
      label.setToolTipText (uri);
      label.addMouseListener (new AnchorAdapter (uri));  //クリックされたらあらかじめ設定されたURIをブラウザに渡す
    }
    return label;
  }  //createAnchor(String,String)


  //--------------------------------------------------------------------------------
  //テキストフィールドを作る

  //textField = createTextField (text, columns)
  //  テキストフィールドを作る
  public static JTextField createTextField (String text, int columns) {
    JTextField textField = new JTextField (text, columns);
    textField.setForeground (new Color (LnF.lnfRGB[14]));
    textField.setSelectionColor (new Color (LnF.lnfRGB[7]));  //選択領域の背景の色
    //textField.setSelectedTextColor (new Color (LnF.lnfRGB[14]));  //選択領域のテキストの色
    return textField;
  }  //createTextField(String,int)

  //textField = createNumberField (text, columns)
  //  数値入力用のテキストフィールドを作る
  public static JTextField createNumberField (String text, int columns) {
    return setHorizontalAlignment (
      setFixedSize (
        //setFont (
        //  new JTextField (text),  //columnを指定すると幅を調節できなくなる
        //  LnF.lnfMonospacedFont),
        //10 + (LnF.lnfFontSize / 2) * columns, LnF.lnfFontSize + 4
        new JTextField (text),  //columnを指定すると幅を調節できなくなる
        10 + (LnF.lnfFontSize * 2 / 3) * columns, LnF.lnfFontSize + 4
        ),
      JTextField.RIGHT);
  }  //createNumberField(int,int)


  //--------------------------------------------------------------------------------
  //スクロールテキストエリアを作る

  //scrollTextArea = createScrollTextArea (text, width, height)
  //scrollTextArea = createScrollTextArea (text, width, height, editable)
  //  スクロールテキストエリアを作る
  public static ScrollTextArea createScrollTextArea (String text, int width, int height) {
    return createScrollTextArea (text, width, height, false);
  }  //createScrollTextArea(String,int,int)
  public static ScrollTextArea createScrollTextArea (String text, int width, int height, boolean editable) {
    ScrollTextArea scrollTextArea = setPreferredSize (
      setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
      width, height);
    setEmptyBorder (scrollTextArea, 0, 0, 0, 0);
    scrollTextArea.setMargin (new Insets (2, 4, 2, 4));  //グリッドを更新させるためJTextAreaではなくScrollTextAreaに設定する必要がある
    JTextArea textArea = scrollTextArea.getTextArea ();
    textArea.setEditable (editable);
    scrollTextArea.setText (text);
    scrollTextArea.setCaretPosition (0);
    return scrollTextArea;
  }  //createScrollTextArea(String,int,int,boolean)


  //--------------------------------------------------------------------------------
  //スクロールテキストペインを作る

  //scrollTextPane = createScrollTextPane (text, width, height)
  //  スクロールテキストペインを作る
  //  http://～の部分がハイパーリンクになる
  //    許諾条件.txtの中に"(http://www.nifty.ne.jp/forum/fsharp/)"という部分がある
  //    ')'はURIに使える文字なので正しい方法では分離することができない
  //    ここではhttp://の直前に'('があるときは')'をURIに使えない文字とみなすことにする
  public static JScrollPane createScrollTextPane (String text, int width, int height) {
    JTextPane textPane = new JTextPane ();
    StyledDocument document = textPane.getStyledDocument ();
    Style defaultStyle = document.addStyle ("default", StyleContext.getDefaultStyleContext ().getStyle (StyleContext.DEFAULT_STYLE));
    int anchorNumber = 0;
    //  http://user:passwd@host:port/path?query#hash → http://host/path?query
    //Matcher matcher = Pattern.compile ("\\bhttps?://[-.0-9A-Za-z]*(?:/(?:[!$&-;=?-Z_a-z~]|%[0-9A-Fa-f]{2})*)?").matcher (text);
    Matcher matcher = Pattern.compile ("\\b" +
                                       "(?:" +
                                       "(?<!\\()https?://[-.0-9A-Za-z]*(?:/(?:[!$&-;=?-Z_a-z~]|%[0-9A-Fa-f]{2})*)?" +
                                       "|" +
                                       "(?<=\\()https?://[-.0-9A-Za-z]*(?:/(?:[!$&-(*-;=?-Z_a-z~]|%[0-9A-Fa-f]{2})*)?" +
                                       ")").matcher (text);
    try {
      int start = 0;
      while (matcher.find ()) {
        int end = matcher.start ();  //ハイパーリンクの開始位置
        if (start < end) {
          document.insertString (document.getLength (), text.substring (start, end), defaultStyle);  //ハイパーリンクの手前のテキスト
        }
        String anchorHref = matcher.group ();  //ハイパーリンク
        Style anchorStyle = document.addStyle ("anchor" + anchorNumber++, defaultStyle);
        JLabel anchorLabel = createAnchor (anchorHref, anchorHref);
        Dimension anchorSize = anchorLabel.getPreferredSize ();
        anchorLabel.setAlignmentY ((float) anchorLabel.getBaseline (anchorSize.width, anchorSize.height) / (float) anchorSize.height);  //JLabelのベースラインをテキストに合わせる
        StyleConstants.setComponent (anchorStyle, anchorLabel);
        document.insertString (document.getLength (), anchorHref, anchorStyle);
        start = matcher.end ();  //ハイパーリンクの終了位置
      }
      document.insertString (document.getLength (), text.substring (start), defaultStyle);  //残りのテキスト
    } catch (BadLocationException ble) {
    }
    textPane.setMargin (new Insets (2, 4, 2, 4));
    textPane.setEditable (false);
    textPane.setCaretPosition (0);
    JScrollPane scrollPane = new JScrollPane (textPane);
    scrollPane.setPreferredSize (new Dimension (width, height));
    return scrollPane;
  }  //createScrollTextPane(String,int,int)


  //--------------------------------------------------------------------------------
  //ボタンを作る

  //button = createButton (enText, listener)
  //button = createButton (enText, mnemonic, listener)
  //  テキストのボタンを作る
  public static JButton createButton (String enText, ActionListener listener) {
    return createButton (enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createButton(String,ActionListener)
  public static JButton createButton (String enText, int mnemonic, ActionListener listener) {
    JButton button = new JButton ();
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createButton(String,int,ActionListener)

  //button = createButton (image, enText, listener)
  //button = createButton (image, enText, mnemonic, listener)
  //button = createButton (image, disabledImage, enText, listener)
  //button = createButton (image, disabledImage, enText, mnemonic, listener)
  //  アイコンとテキストのボタンを作る
  public static JButton createButton (Image image, String enText, ActionListener listener) {
    return createButton (image, null, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createButton(Image,String,ActionListener)
  public static JButton createButton (Image image, String enText, int mnemonic, ActionListener listener) {
    return createButton (image, null, enText, mnemonic, listener);
  }  //createButton(Image,String,int,ActionListener)
  public static JButton createButton (Image image, Image disabledImage, String enText, ActionListener listener) {
    return createButton (image, disabledImage, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createButton(Image,Image,String,ActionListener)
  public static JButton createButton (Image image, Image disabledImage, String enText, int mnemonic, ActionListener listener) {
    JButton button = new JButton (new ImageIcon (image));
    if (disabledImage != null) {
      button.setDisabledIcon (new ImageIcon (disabledImage));
    }
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    button.setIconTextGap (3);
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createButton(Image,Image,String,int,ActionListener)

  //button = createIconButton (icon, enToolTipText, listener)
  //button = createIconButton (icon, disabledIcon, enToolTipText, listener)
  //button = createImageButton (image, enToolTipText, listener)
  //button = createImageButton (image, disabledImage, enToolTipText, listener)
  //  アイコンのみのボタンを作る
  //  ツールチップテキストをそのままアクションコマンドにする
  public static JButton createIconButton (ImageIcon icon, String enToolTipText, ActionListener listener) {
    return createIconButton (icon, null, enToolTipText, listener);
  }  //createIconButton(ImageIcon,String,ActionListener)
  public static JButton createIconButton (ImageIcon icon, ImageIcon disabledIcon, String enToolTipText, ActionListener listener) {
    JButton button = new JButton (icon);
    if (disabledIcon != null) {
      button.setDisabledIcon (disabledIcon);
    }
    //button.setContentAreaFilled (false);
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    //button.setBorderPainted (false);
    //button.setMargin (new Insets (0, 0, 0, 0));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addListener (button, listener);
  }  //createIconButton(ImageIcon,ImageIcon,String,ActionListener)
  public static JButton createImageButton (Image image, String enToolTipText, ActionListener listener) {
    return createImageButton (image, null, enToolTipText, listener);
  }  //createImageButton(Image,String,ActionListener)
  public static JButton createImageButton (Image image, Image disabledImage, String enToolTipText, ActionListener listener) {
    JButton button = new JButton (new ImageIcon (image));
    if (disabledImage != null) {
      button.setDisabledIcon (new ImageIcon (disabledImage));
    }
    //button.setContentAreaFilled (false);
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    //button.setBorderPainted (false);
    //button.setMargin (new Insets (0, 0, 0, 0));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addListener (button, listener);
  }  //createImageButton(Image,Image,String,ActionListener)

  //button = createCheckBox (selected, enText, listener)
  //button = createCheckBox (selected, enText, mnemonic, listener)
  //  チェックボックスを作る
  public static JCheckBox createCheckBox (boolean selected, String enText, ActionListener listener) {
    return createCheckBox (selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createCheckBox(boolean,String,ActionListener)
  public static JCheckBox createCheckBox (boolean selected, String enText, int mnemonic, ActionListener listener) {
    JCheckBox button = new JCheckBox ();
    button.setBorder (new EmptyBorder (0, 0, 0, 0));
    button.setSelected (selected);
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createCheckBox(boolean,String,int,ActionListener)

  //button = createIconCheckBox (selected, image, selectedImage, enToolTipText, listener)
  //button = createIconCheckBox (selected, image, selectedImage, disabledImage, disabledSelectedImage, enToolTipText, listener)
  //  アイコンチェックボックスを作る
  //  ツールチップテキストをそのままアクションコマンドにする
  public static JCheckBox createIconCheckBox (boolean selected, Image image, Image selectedImage, String enToolTipText, ActionListener listener) {
    return createIconCheckBox (selected, image, selectedImage, null, null, enToolTipText, listener);
  }  //createIconCheckBox(boolean,Image,Image,String,ActionListener)
  public static JCheckBox createIconCheckBox (boolean selected, Image image, Image selectedImage, Image disabledImage, Image disabledSelectedImage, String enToolTipText, ActionListener listener) {
    JCheckBox button = new JCheckBox (new ImageIcon (image));
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    button.setSelected (selected);
    button.setSelectedIcon (new ImageIcon (selectedImage));
    if (disabledImage != null) {
      button.setDisabledIcon (new ImageIcon (disabledImage));
    }
    if (disabledSelectedImage != null) {
      button.setDisabledSelectedIcon (new ImageIcon (disabledSelectedImage));
    }
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addListener (button, listener);
  }  //createIconCheckBox(boolean,Image,Image,Image,Image,String,ActionListener)

  //radioButton = createRadioButton (group, selected, enText, listener)
  //radioButton = createRadioButton (group, selected, enText, mnemonic, listener)
  //  ラジオボタンを作る
  public static JRadioButton createRadioButton (ButtonGroup group, boolean selected, String enText, ActionListener listener) {
    return createRadioButton (group, selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createRadioButton(ButtonGroup,boolean,String,ActionListener)
  public static JRadioButton createRadioButton (ButtonGroup group, boolean selected, String enText, int mnemonic, ActionListener listener) {
    JRadioButton button = new JRadioButton ();
    button.setBorder (new EmptyBorder (0, 0, 0, 0));
    group.add (button);
    button.setSelected (selected);
    return setButtonCommons (button, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createRadioButton(ButtonGroup,boolean,String,int,ActionListener)

  //button = createIconRadioButton (group, selected, image, selectedImage, enToolTipText, listener)
  //  アイコンラジオボタンを作る
  //  ツールチップテキストをそのままアクションコマンドにする
  public static JRadioButton createIconRadioButton (ButtonGroup group, boolean selected, Image image, Image selectedImage, String enToolTipText, ActionListener listener) {
    JRadioButton button = new JRadioButton (new ImageIcon (image));
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    group.add (button);
    button.setSelected (selected);
    button.setSelectedIcon (new ImageIcon (selectedImage));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    return addListener (button, listener);
  }  //createIconRadioButton(ButtonGroup,boolean,Image,Image,String,int,ActionListener)


  //--------------------------------------------------------------------------------
  //アイコン回転式ボタンを作る

  //button = createRotaryButton (enToolTipText, listener, index, image, ...)
  public static RotaryButton createRotaryButton (String enToolTipText, ActionListener listener, int index, Image... images) {
    Icon[] icons = new ImageIcon[images.length];
    for (int i = 0; i < images.length; i++) {
      icons[i] = new ImageIcon (images[i]);
    }
    RotaryButton button = new RotaryButton (index, icons);
    button.setBorder (new EmptyBorder (1, 1, 1, 1));
    if (enToolTipText != null) {
      button.setToolTipText (enToolTipText);
      button.setActionCommand (enToolTipText);
    }
    if (listener != null) {
      button.addActionListener (listener);
    }
    return button;
  }  //createRotaryButton


  //--------------------------------------------------------------------------------
  //スライダーを作る

  //slider = createHorizontalSlider (min, max, value, major, minor, texts, listener)
  //  ラベルのテキストを指定してスライダーを作る
  public static JSlider createHorizontalSlider (int min, int max, int value, int major, int minor, String[] texts, ChangeListener listener) {
    JSlider slider = createHorizontalSlider (min, max, value, major, minor, listener);
    Hashtable<Integer,JComponent> table = new Hashtable<Integer,JComponent> ();
    for (int i = min; i <= max; i++) {
      if (i % major == 0 && texts[i - min] != null) {  //メジャー目盛りの位置だけ書く
        table.put (i, createLabel (texts[i - min]));
      }
    }
    slider.setLabelTable (table);
    return slider;
  }  //createHorizontalSlider(int,int,int,int,int,String[],ChangeListener)

  //slider = createHorizontalSlider (min, max, value, major, minor, listener)
  //  スライダーを作る
  public static JSlider createHorizontalSlider (int min, int max, int value, int major, int minor, ChangeListener listener) {
    JSlider slider = new JSlider (SwingConstants.HORIZONTAL, min, max, value);
    if (major != 0) {
      slider.setLabelTable (slider.createStandardLabels (major));
      slider.setPaintLabels (true);
      slider.setMajorTickSpacing (major);
      if (minor != 0) {
        slider.setMinorTickSpacing (minor);
      }
      slider.setPaintTicks (true);
      slider.setSnapToTicks (true);
    }
    return addListener (slider, listener);
  }  //createHorizontalSlider(int,int,int,int,int,ChangeListener)


  //--------------------------------------------------------------------------------
  //メニューを作る

  //menuBar = createMenuBar (component, ...)
  //  メニューバーを作る
  //  メニューアイテムを並べる
  //  nullは何も表示しない
  //  Box.createHorizontalGlue()を追加すると残りのメニューを右に寄せることができる
  public static JMenuBar createMenuBar (Component... components) {
    JMenuBar bar = new JMenuBar ();
    for (Component component : components) {
      if (component != null) {
        bar.add (component);
      }
    }
    return bar;
  }  //createMenuBar(Component...)

  //menu = createMenu (enText, item, ...)
  //menu = createMenu (enText, mnemonic, item, ...)
  //  メニューを作る
  //  メニューアイテムを並べる
  //  nullは何も表示しない
  //  セパレータを入れるときはcreateHorizontalSeparator()を使う
  //  JSeparatorを受け付けるためJMenuItem...ではなくJComponent...にする
  public static JMenu createMenu (String enText, JComponent... items) {
    return createMenu (enText, KeyEvent.VK_UNDEFINED, items);
  }  //createMenu(String,JComponent...)
  public static JMenu createMenu (String enText, int mnemonic, JComponent... items) {
    JMenu menu = new JMenu ();
    for (JComponent item : items) {
      if (item != null) {
        menu.add (item);
      }
    }
    //menu.setAccelerator()は実行時エラーになる
    //  java.lang.Error: setAccelerator() is not defined for JMenu.  Use setMnemonic() instead.
    return setButtonCommons (menu, enText, mnemonic, null);  //ボタンの共通の設定
  }  //createMenu(String,int,JComponent...)

  //popupMenu = createPopupMenu (item, ...)
  //  ポップアップメニューを作る
  //  メニューアイテムを並べる
  //  nullは何も表示しない
  //  セパレータを入れるときはcreateHorizontalSeparator()を使う
  //  JSeparatorを受け付けるためJMenuItem...ではなくJComponent...にする
  public static JPopupMenu createPopupMenu (JComponent... items) {
    JPopupMenu popupMenu = new JPopupMenu ();
    for (JComponent item : items) {
      if (item != null) {
        popupMenu.add (item);
      }
    }
    return popupMenu;
  }  //createPopupMenu(JComponent...)

  //item = createMenuItem (enText, listener)
  //item = createMenuItem (enText, mnemonic, listener)
  //  メニューアイテムを作る
  public static JMenuItem createMenuItem (String enText, ActionListener listener) {
    return createMenuItem (enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createMenuItem(String,ActionListener)
  public static JMenuItem createMenuItem (String enText, int mnemonic, ActionListener listener) {
    return createMenuItem (enText, mnemonic, 0, listener);
  }  //createMenuItem(String,int,ActionListener)
  public static JMenuItem createMenuItem (String enText, int mnemonic, int modifiers, ActionListener listener) {
    JMenuItem item = new JMenuItem ();
    if (modifiers != 0) {
      item.setAccelerator (KeyStroke.getKeyStroke (mnemonic, modifiers));
      mnemonic = KeyEvent.VK_UNDEFINED;
    }
    return setButtonCommons (item, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createMenuItem(String,int,int,ActionListener)

  //item = createCheckBoxMenuItem (selected, enText, listener)
  //item = createCheckBoxMenuItem (selected, enText, mnemonic, listener)
  //  チェックボックスメニューアイテムを作る
  public static JCheckBoxMenuItem createCheckBoxMenuItem (boolean selected, String enText, ActionListener listener) {
    return createCheckBoxMenuItem (selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createCheckBoxMenuItem(boolean,String,ActionListener)
  public static JCheckBoxMenuItem createCheckBoxMenuItem (boolean selected, String enText, int mnemonic, ActionListener listener) {
    return createCheckBoxMenuItem (selected, enText, mnemonic, 0, listener);
  }  //createCheckBoxMenuItem(boolean,String,int,ActionListener)
  public static JCheckBoxMenuItem createCheckBoxMenuItem (boolean selected, String enText, int mnemonic, int modifiers, ActionListener listener) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem ();
    item.setSelected (selected);
    if (modifiers != 0) {
      item.setAccelerator (KeyStroke.getKeyStroke (mnemonic, modifiers));
      mnemonic = KeyEvent.VK_UNDEFINED;
    }
    return setButtonCommons (item, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createCheckBoxMenuItem(boolean,String,int,int,ActionListener)

  //item = createRadioButtonMenuItem (group, selected, enText, listener)
  //item = createRadioButtonMenuItem (group, selected, enText, mnemonic, listener)
  //  ラジオボタンメニューアイテムを作る
  public static JRadioButtonMenuItem createRadioButtonMenuItem (ButtonGroup group, boolean selected, String enText, ActionListener listener) {
    return createRadioButtonMenuItem (group, selected, enText, KeyEvent.VK_UNDEFINED, listener);
  }  //createRadioButtonMenuItem(ButtonGroup,boolean,String,ActionListener)
  public static JRadioButtonMenuItem createRadioButtonMenuItem (ButtonGroup group, boolean selected, String enText, int mnemonic, ActionListener listener) {
    return createRadioButtonMenuItem (group, selected, enText, mnemonic, 0, listener);
  }  //createRadioButtonMenuItem(ButtonGroup,boolean,String,int,ActionListener)
  public static JRadioButtonMenuItem createRadioButtonMenuItem (ButtonGroup group, boolean selected, String enText, int mnemonic, int modifiers, ActionListener listener) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem ();
    group.add (item);
    item.setSelected (selected);
    if (modifiers != 0) {
      item.setAccelerator (KeyStroke.getKeyStroke (mnemonic, modifiers));
      mnemonic = KeyEvent.VK_UNDEFINED;
    }
    return setButtonCommons (item, enText, mnemonic, listener);  //ボタンの共通の設定
  }  //createRadioButtonMenuItem(ButtonGroup,boolean,String,int,int,ActionListener)

  //setButtonCommons (button, enText, mnemonic, listener)
  //  ボタンの共通の設定
  //  ニモニックを含まないテキストをそのままアクションコマンドにする
  //  Multilingual.mlnTextがアクションコマンドを英語のテキストとして使うのでアクションリスナーを省略してもアクションコマンドは設定される
  //  ニモニックはKeyEvent.VK_～で指定する。英数字は大文字のcharで指定しても問題ない
  //  Multilingual.mlnTextがニモニックの有無をgetMnemonicで確認するのでニモニックがKeyEvent.VK_UNDEFINEDのときもそのままニモニックとして設定される
  public static <T extends AbstractButton> T setButtonCommons (T button, String enText, int mnemonic, ActionListener listener) {
    button.setMnemonic (mnemonic);
    if (mnemonic == KeyEvent.VK_UNDEFINED) {  //ニモニックがないとき
      button.setText (enText);
    } else {  //ニモニックがあるとき
      //テキストにニモニックの大文字と小文字が両方含まれているとき、大文字と小文字が一致するほうにマークを付ける
      String mnemonicText = KeyEvent.getKeyText (mnemonic);
      int index = enText.indexOf (mnemonicText);  //大文字と小文字を区別して検索する
      if (index < 0) {
        index = enText.toLowerCase ().indexOf (mnemonicText.toLowerCase ());  //大文字と小文字を区別せずに検索する
      }
      if (index >= 0) {  //ニモニックがテキストに含まれているとき
        button.setText (enText);
        button.setDisplayedMnemonicIndex (index);
      } else {  //ニモニックがテキストに含まれていないとき
        button.setText (enText + "(" + mnemonicText + ")");
        button.setDisplayedMnemonicIndex (enText.length () + 1);
      }
    }
    button.setActionCommand (enText);
    return addListener (button, listener);
  }  //setButtonCommons(T extends AbstractButton,String,int,ActionListener)


  //--------------------------------------------------------------------------------
  //スクロールリストを作る

  //list = createScrollList (texts, visibleRowCount, selectedIndex, listener)
  //list = createScrollList (texts, visibleRowCount, selectedIndex, selectionMode, listener)
  //  selectionMode
  //    ListSelectionModel.SINGLE_SELECTION
  //    ListSelectionModel.SINGLE_INTERVAL_SELECTION
  //    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  public static ScrollList createScrollList (String[] texts, int visibleRowCount, int selectedIndex, ListSelectionListener listener) {
    return createScrollList (texts, visibleRowCount, selectedIndex, ListSelectionModel.SINGLE_SELECTION, listener);
  }  //createScrollList(String[],int,int)
  public static ScrollList createScrollList (String[] texts, int visibleRowCount, int selectedIndex, int selectionMode, ListSelectionListener listener) {
    DefaultListModel<String> listModel = new DefaultListModel<String> ();
    for (String text : texts) {
      listModel.addElement (text);
    }
    ScrollList list = new ScrollList (listModel);
    list.setVisibleRowCount (visibleRowCount);
    list.setSelectionMode (selectionMode);
    list.setSelectedIndex (selectedIndex);
    return addListener (list, listener);
  }  //createScrollList(String[],int,int,int)


  //--------------------------------------------------------------------------------
  //コンボボックスを作る

  //comboBox = createComboBox (selectedIndex, enToolTipText, listener, text, ...)
  //comboBox = createComboBox (selectedIndex, enToolTipText, listener, columns, text, ...)
  //  コンボボックスを作る
  public static JComboBox<String> createComboBox (int selectedIndex, String enToolTipText, ActionListener listener, String... texts) {
    int columns = 0;
    for (String text : texts) {
      int length = text.length ();
      if (columns < length) {
        columns = length;
      }
    }
    return createComboBox (selectedIndex, enToolTipText, listener, columns, texts);
  }
  public static JComboBox<String> createComboBox (int selectedIndex, String enToolTipText, ActionListener listener, int columns, String... texts) {
    JComboBox<String> comboBox = new JComboBox<String> (texts);
    setEmptyBorder (comboBox, 0, 0, 0, 0);
    comboBox.setEditable (false);
    comboBox.setSelectedIndex (selectedIndex);
    //comboBox.setMaximumRowCount (5);
    setFixedSize (comboBox, 30 + (LnF.lnfFontSize * 2 / 3) * columns, LnF.lnfFontSize + 4);
    if (enToolTipText != null) {
      comboBox.setToolTipText (enToolTipText);
      comboBox.setActionCommand (enToolTipText);
    }
    return addListener (comboBox, listener);
  }  //createComboBox(int,String,ActionListener,int,String...)


  //--------------------------------------------------------------------------------
  //スピナーを作る

  //spinner = createNumberSpinner (model, digits, listener)
  //  ナンバースピナーを作る
  public static NumberSpinner createNumberSpinner (SpinnerNumberModel model, int digits, ChangeListener listener) {
    NumberSpinner spinner = new NumberSpinner (model);
    spinner.setBorder (new LineBorder (MetalLookAndFeel.getBlack (), 1));  //black
    setFixedSize (spinner, 24 + (LnF.lnfFontSize * 2 / 3) * digits, LnF.lnfFontSize + 4);
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor ();
    editor.getFormat ().setGroupingUsed (false);  //3桁毎にグループ化しない
    JTextField textField = editor.getTextField ();
    textField.setHorizontalAlignment (JTextField.RIGHT);  //右寄せ
    //textField.setFont (LnF.lnfMonospacedFont);
    return addListener (spinner, listener);
  }  //createNumberSpinner(SpinnerNumberModel,int,ChangeListener)

  //spinner = createDecimalSpinner (value, minimum, maximum, stepSize)
  //spinner = createDecimalSpinner (value, minimum, maximum, stepSize, option)
  //spinner = createDecimalSpinner (value, minimum, maximum, stepSize, option, listener)
  //  10進数スピナーを作る
  public static DecimalSpinner createDecimalSpinner (int value, int minimum, int maximum, int stepSize) {
    return createDecimalSpinner (value, minimum, maximum, stepSize, 0, null);
  }  //createDecimalSpinner(int,int,int,int)
  public static DecimalSpinner createDecimalSpinner (int value, int minimum, int maximum, int stepSize, int option) {
    return createDecimalSpinner (value, minimum, maximum, stepSize, option, null);
  }  //createDecimalSpinner(int,int,int,int,int)
  public static DecimalSpinner createDecimalSpinner (int value, int minimum, int maximum, int stepSize, int option, ChangeListener listener) {
    return addListener (new DecimalSpinner (value, minimum, maximum, stepSize, option), listener);
  }  //createDecimalSpinner(int,int,int,int,int,ChangeListener)

  //spinner = createHex8Spinner (value, mask, reverse, listener)
  //   8桁16進数スピナーを作る
  public static Hex8Spinner createHex8Spinner (int value, int mask, boolean reverse, ChangeListener listener) {
    return addListener (new Hex8Spinner (value, mask, reverse), listener);
  }  //createHex8Spinner(int,int,boolean,ChangeListener)

  //spinner = createListSpinner (list, value, listener)
  //  リストスピナーを作る
  public static JSpinner createListSpinner (java.util.List<?> list, Object value, ChangeListener listener) {
    SpinnerListModel model = new SpinnerListModel (list);
    JSpinner spinner = new JSpinner (model);
    spinner.setBorder (new LineBorder (MetalLookAndFeel.getSeparatorForeground (), 1));  //primary1
    int digits = 0;
    for (Object t : list) {
      digits = Math.max (digits, String.valueOf (t).length ());
    }
    setFixedSize (spinner, 24 + (LnF.lnfFontSize * 2 / 3) * digits, LnF.lnfFontSize + 4);
    JSpinner.ListEditor editor = (JSpinner.ListEditor) spinner.getEditor ();
    JTextField textField = editor.getTextField ();
    textField.setHorizontalAlignment (JTextField.RIGHT);  //右寄せ
    //textField.setFont (LnF.lnfMonospacedFont);
    model.setValue (value);  //初期設定ではリスナーを呼び出さない
    return addListener (spinner, listener);
  }  //createListSpinner (java.util.List<?>,Object,ChangeListener)

  //spinner = createStringSpinner (array, index, listener)
  //  文字列スピナーを作る
  public static JSpinner createStringSpinner (String[] array, int index, ChangeListener listener) {
    ArrayList<String> list = new ArrayList<String> ();
    for (String string : array) {
      list.add (string);
    }
    return createListSpinner (list, array[index], listener);
  }  //createStringSpinner


  //--------------------------------------------------------------------------------
  //  デバッグ

  //printAncestorClass (object)
  //  オブジェクトのクラス、親クラス、親の親クラス…を表示する
  public static void printAncestorClass (Object object) {
    System.out.print (object);
    if (object != null) {
      Class<? extends Object> c = object.getClass ();
      for (c = c.getSuperclass (); c != null; c = c.getSuperclass ()) {
        System.out.print (" < " + c.getName ());
      }
    }
    System.out.println ();
  }

  //printComponentTree (component)
  //printComponentTree (component, prefix)
  //  コンポーネントのツリーを表示する
  public static void printComponentTree (Component component) {
    printComponentTree (component, "0.");
  }
  public static void printComponentTree (Component component, String prefix) {
    System.out.print (prefix + " ");
    printAncestorClass (component);
    if (component instanceof Container) {
      Container container = (Container) component;
      int n = container.getComponentCount ();
      for (int i = 0; i < n; i++) {
        printComponentTree (container.getComponent (i), "  " + prefix + i + ".");
      }
    }
  }


}  //class ComponentFactory



