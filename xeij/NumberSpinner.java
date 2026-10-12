//========================================================================================
//  NumberSpinner.java
//    en:NumberSpinner -- It is a number spinner that invokes the change listener immediately when you change a number without hitting the Enter key.
//    ja:ナンバースピナー -- Enterキーを押さなくても数字を書き換えるとすぐにチェンジリスナーが呼び出されるナンバースピナーです。
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.text.*;  //DecimalFormat,NumberFormat,ParseException
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument

public class NumberSpinner extends JSpinner implements ChangeListener, DocumentListener {

  protected SpinnerNumberModel model;
  protected DecimalFormat format;
  protected JTextField textField;

  //  ドキュメントリスナーの中でドキュメントを更新しようとするとAbstractDocumentのwriteLock()でIllegalStateExceptionが出てしまう
  //  AbstractDocumentのnumWritersを直接読み出す方法が見当たらないのでドキュメントリスナーの中かどうかを自前のフラグで判断する
  //  ドキュメントリスナーの中ではドキュメントを更新できないのだからドキュメントリスナーが二重に呼び出されることはないはず
  //  従ってフラグは二値で足りる
  private boolean writeLocked;  //true=ドキュメントリスナーの中なのでドキュメントを更新できない

  //コンストラクタ
  @SuppressWarnings ("this-escape") public NumberSpinner (SpinnerNumberModel model) {
    super (model);
    this.model = model;
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) getEditor ();  //[this-escape]
    format = editor.getFormat ();
    //format.setGroupingUsed (false);  //コンマを書かない
    textField = editor.getTextField ();
    writeLocked = false;
    removeChangeListener (editor);  //デフォルトのチェンジリスナーはドキュメントリスナーの中から呼び出すとIllegalStateExceptionが出てしまうので取り除く
    addChangeListener (this);  //モデルのチェンジリスナーを設定する
    ((AbstractDocument) textField.getDocument ()).addDocumentListener (this);  //エディタのドキュメントリスナーを設定する
  }

  //モデルのチェンジリスナー
  @Override public void stateChanged (ChangeEvent ce) {
    if (!writeLocked) {
      Number modelNumber = model.getNumber ();
      textField.setText (modelNumber instanceof Double || modelNumber instanceof Float ?
                         format.format (modelNumber.doubleValue ()) :
                         format.format (modelNumber.longValue ()));
    }
  }

  //エディタのドキュメントリスナー
  @Override public void changedUpdate (DocumentEvent de) {
  }
  @Override public void insertUpdate (DocumentEvent de) {
    textChanged ();
  }
  @Override public void removeUpdate (DocumentEvent de) {
    textChanged ();
  }
  protected void textChanged () {
    writeLocked = true;  //モデルのチェンジリスナーにテキストを更新させない
    Number modelNumber = model.getNumber ();  //モデルの値
    try {
      Number editorNumber = format.parse (textField.getText ());  //エディタの値
      //SpinnerNumberModelのsetValue()は範囲外の値でも受け付けてしまうので範囲内であることを確認してから設定する
      if (modelNumber instanceof Double || modelNumber instanceof Float) {
        double modelValue = modelNumber.doubleValue ();  //モデルの値
        double editorValue = editorNumber.doubleValue ();  //エディタの値
        if (modelValue != editorValue &&  //モデルの値とエディタの値が異なり、
            ((Number) model.getMinimum ()).doubleValue () <= editorValue &&
            editorValue <= ((Number) model.getMaximum ()).doubleValue ()) {  //エディタの値がモデルの範囲内のとき
          model.setValue (editorNumber);  //エディタの値をモデルに設定してチェンジリスナーを呼ばせる
        }
      } else {
        long modelValue = modelNumber.longValue ();  //モデルの値
        long editorValue = editorNumber.longValue ();  //エディタの値
        if (modelValue != editorValue &&  //モデルの値とエディタの値が異なり、
            ((Number) model.getMinimum ()).longValue () <= editorValue &&
            editorValue <= ((Number) model.getMaximum ()).longValue ()) {  //エディタの値がモデルの範囲内のとき
          model.setValue (editorNumber);  //エディタの値をモデルに設定してチェンジリスナーを呼ばせる
        }
      }
    } catch (ParseException pe) {
    }
    writeLocked = false;
  }

}  //class NumberSpinner



