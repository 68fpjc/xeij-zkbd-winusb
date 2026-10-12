//========================================================================================
//  ReverseLongModel.java
//    en:Reverse long model -- It is a modified SpinnerNumberModel that has a Long value and reversely spins.
//    ja:リバースロングモデル -- SpinnerNumberModelの値をLongにして回転方向を逆にしたスピナーモデルです。
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class ReverseLongModel extends SpinnerNumberModel {
  public ReverseLongModel (long value, long minimum, long maximum, long stepSize) {
    super (Long.valueOf (value), Long.valueOf (minimum), Long.valueOf (maximum), Long.valueOf (stepSize));
  }
  @Override public Object getNextValue () {
    return super.getPreviousValue ();
  }
  @Override public Object getPreviousValue () {
    return super.getNextValue ();
  }
}  //class ReverseLongModel



