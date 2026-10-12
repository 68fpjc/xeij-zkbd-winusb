//========================================================================================
//  DecimalSpinner.java
//    en:Decimal spinner
//    ja:10進数スピナー
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.border.*;  //Border,CompoundBorder,EmptyBorder,EtchedBorder,LineBorder,TitledBorder

public class DecimalSpinner extends NumberSpinner {

  protected int minimum;  //最小値。model.getMinimum()はintではない
  protected int maximum;  //最大値
  protected int option;  //オプション。多数のスピナーをリスナーの中で効率よく見分けたいときに番号などを入れる

  //DecimalSpinner (value, minimum, maximum, stepSize)
  //DecimalSpinner (value, minimum, maximum, stepSize, option)
  //  コンストラクタ
  public DecimalSpinner (int value, int minimum, int maximum, int stepSize) {
    this (value, minimum, maximum, stepSize, 0);
  }  //new DecimalSpinner(int,int,int,int)
  @SuppressWarnings ("this-escape") public DecimalSpinner (int value, int minimum, int maximum, int stepSize, int option) {
    super (new SpinnerNumberModel (value, minimum, maximum, stepSize));  //minimum<=value<=maximumでなければここでIllegalArgumentExceptionが発生する
    this.minimum = minimum;
    this.maximum = maximum;
    this.option = option;
    this.setBorder (new LineBorder (new Color (LnF.lnfRGB[10]), 1));  //[this-escape]
    int digits = String.valueOf (maximum).length ();  //maximumの桁数
    ComponentFactory.setFixedSize (this, 24 + (LnF.lnfFontSize * 2 / 3) * digits, LnF.lnfFontSize + 4);
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) this.getEditor ();
    editor.getFormat ().setGroupingUsed (false);  //3桁毎にグループ化しない
    JTextField textField = editor.getTextField ();
    textField.setText (String.valueOf (value));  //初回は3桁毎にグループ化されているので上書きする
    textField.setHorizontalAlignment (JTextField.CENTER);  //中央寄せ
    //textField.setFont (LnF.lnfMonospacedFont);
  }  //new DecimalSpinner(int,int,int,int,int)

  //value = spinner.getIntValue ()
  //  値を読み出す
  //  範囲外の値が返ることはない
  public int getIntValue () {
    return this.model.getNumber ().intValue ();
  }  //getIntValue()

  //spinner.setIntValue (value)
  //  値を書き込む
  //  範囲外の値は設定できない
  //  チェンジリスナーが呼び出される
  public void setIntValue (int value) {
    if (value < minimum || maximum < value) {
      throw new IllegalArgumentException ();
    }
    this.model.setValue (Integer.valueOf (value));
  }  //spinner.setIntValue(int)

  //option = spinner.getOption ()
  //  オプションを読み出す
  public int getOption () {
    return this.option;
  }  //spinner.getOption()

  //spinner.setOption(option)
  //  オプションを書き込む
  public void setOption (int option) {
    this.option = option;
  }  //spinner.setOption(int)

}  //class DecimalSpinner



