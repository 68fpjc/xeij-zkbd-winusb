//========================================================================================
//  ScrollList.java
//    en:Scroll list -- It is a modified JScrollPage that has a JList as the view.
//    ja:スクロールリスト -- JListをビューに持つJScrollPaneです。
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class ScrollList extends JScrollPane {

  private DefaultListModel<String> listModel;
  private JList<String> list;

  @SuppressWarnings ("this-escape") public ScrollList (DefaultListModel<String> listModel) {
    super ();
    this.listModel = listModel;
    list = new JList<String> (listModel);
    setViewportView (list);  //[this-escape]
  }

  public Color getBackground () {
    return list == null ? null : list.getBackground ();
  }
  public void setBackground (Color background) {
    super.setBackground (background);
    if (list != null) {
      list.setBackground (background);
    }
  }

  public Color getForeground () {
    return list == null ? null : list.getForeground ();
  }
  public void setForeground (Color foreground) {
    super.setForeground (foreground);
    if (list != null) {
      list.setForeground (foreground);
    }
  }

  public Font getFont () {
    return list == null ? null : list.getFont ();
  }
  public void setFont (Font font) {
    super.setFont (font);
    if (list != null) {
      list.setFont (font);
    }
  }

  public int getVisibleRowCount () {
    return list == null ? 8 : list.getVisibleRowCount ();
  }
  public void setVisibleRowCount (int visibleRowCount) {
    if (list != null) {
      list.setVisibleRowCount (visibleRowCount);
    }
  }

  public int getSelectedIndex () {
    return list == null ? -1 : list.getSelectedIndex ();
  }
  public void setSelectedIndex (int selectedIndex) {
    if (list != null) {
      list.setSelectedIndex (selectedIndex);
    }
  }

  public int[] getSelectedIndices () {
    return list == null ? new int[0] : list.getSelectedIndices ();
  }
  public void setSelectedIndices (int selectedIndices[]) {
    if (list != null) {
      list.setSelectedIndices (selectedIndices);
    }
  }

  public int getSelectionMode () {
    return list == null ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : list.getSelectionMode ();
  }
  public void setSelectionMode (int selectionMode) {
    if (list != null) {
      list.setSelectionMode (selectionMode);
    }
  }

  public void addListSelectionListener (ListSelectionListener listener) {
    list.addListSelectionListener (listener);
  }
  public ListSelectionListener[] getListSelectionListeners () {
    return list.getListSelectionListeners ();
  }
  public void removeListSelectionListener (ListSelectionListener listener) {
    list.removeListSelectionListener (listener);
  }

  public void setTexts (String[] texts) {
    listModel.clear ();
    for (String text : texts) {
      listModel.addElement (text);
    }
  }
  public void addElement (String element) {
    listModel.addElement (element);
  }
  public void clear () {
    listModel.clear ();
  }

}  //class ScrollList



