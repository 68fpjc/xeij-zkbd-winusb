//========================================================================================
//  UnderlinedLabel.java
//    en:Underlined label
//    ja:下線付きラベル
//  Copyright (C) 2003-2019 Makoto Kamada
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

public class UnderlinedLabel extends JLabel {
  private Stroke stroke;
  public UnderlinedLabel (String text) {
    super (text);
    stroke = new BasicStroke ();
  }
  @Override public void paintComponent (Graphics g) {
    super.paintComponent (g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor (getForeground ());
    g2.setStroke (stroke);
    g2.drawLine (0, getHeight () - 2, getWidth () - 1, getHeight () - 2);  //下線を描き足す。位置はてきとう
  }
}  //class UnderlinedLabel



