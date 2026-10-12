//========================================================================================
//  Hex8Spinner.java
//    en:Eight character hexadecimal spinner
//    ja:8桁16進数スピナー
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  操作できるビットのマスクを指定する
//  選択できる値のヒントを指定する
//  ↑キーを押すか▲ボタンをクリックすると直前の小さい方の値が選択される
//  ↓キーを押すか▼ボタンをクリックすると直後の大きい方の値が選択される
//  オフセットを指定する
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.border.*;  //Border,CompoundBorder,EmptyBorder,EtchedBorder,LineBorder,TitledBorder
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument

public class Hex8Spinner extends JSpinner implements ChangeListener, DocumentListener {

  public Hex8SpinnerModel model;  //モデル
  public JTextField editor;  //エディタ

  //コンストラクタ
  @SuppressWarnings ("this-escape") public Hex8Spinner (int newValue, int newMask, boolean newReverse) {
    model = new Hex8SpinnerModel (newValue, newMask, newReverse);
    editor = new JTextField (XEiJ.fmtHex8 (newValue & newMask), 8);
    editor.setHorizontalAlignment (JTextField.RIGHT);
    //editor.setFont (LnF.lnfMonospacedFont);
    AbstractDocument document = (AbstractDocument) editor.getDocument ();
    document.setDocumentFilter (new Hex8DocumentFilter (newMask));
    setBorder (new LineBorder (new Color (LnF.lnfRGB[10]), 1));  //[this-escape]
    ComponentFactory.setFixedSize (this, 32 + (LnF.lnfFontSize * 2 / 3) * 8, LnF.lnfFontSize + 4);
    setModel (model);
    setEditor (editor);
    model.addChangeListener (this);
    document.addDocumentListener (this);
  }

  //モデルのチェンジリスナー
  //  モデルに値が設定されたときに呼び出される
  //  モデルの値をエディタに設定する
  //  エディタのドキュメントリスナーの中ではドキュメントがロックされていて書き換えることができない
  //  もともと書き換える必要がないので何もしなくてよいのだが、IllegalStateExceptionが出るのは困る
  //  ロックされているかどうか直接調べる方法が思い付かなかったのでIllegalStateExceptionを無視することにした
  //  他のチェンジリスナーがモデルから値を読み出す分には問題ない
  @Override public void stateChanged (ChangeEvent ce) {
    try {
      editor.setText (XEiJ.fmtHex8 (model.getIntValue ()));
    } catch (IllegalStateException ise) {
    }
  }

  //エディタのドキュメントリスナー
  //  エディタの値が書き換えられたときに呼び出される
  //  エディタの値をモデルに設定する
  @Override public void changedUpdate (DocumentEvent de) {
  }
  @Override public void insertUpdate (DocumentEvent de) {
    model.setIntValue ((int) Long.parseLong (editor.getText (), 16));  //0x80000000～0xffffffffは直接intにできない
  }
  @Override public void removeUpdate (DocumentEvent de) {
  }

  //値の操作
  public int getIntValue () {
    return model.getIntValue ();
  }
  public void setIntValue (int newValue) {
    model.setIntValue (newValue);
  }
  public int getAbsoluteValue () {
    return model.getAbsoluteValue ();
  }
  public void setAbsoluteValue (int newValue) {
    model.setAbsoluteValue (newValue);
  }

  //ヒントの操作
  public void setHintArray (int[] array, int count) {
    model.setHintArray (array, count);
  }
  public int getHintIndex () {
    return model.getHintIndex ();
  }
  public void setHintIndex (int index) {
    model.setHintIndex (index);
  }

  //オフセットの操作
  public void setOffset (int offset) {
    model.setOffset (offset);
  }



  //========================================================================================
  //$$XFL Hex8DocumentFilter
  //  8桁16進数ドキュメントフィルタ
  //  常に上書き
  //  削除はできない
  //  a～fをA～Fに変換する
  //  MaskFormatterだと偶数のみのような制約が作れないので自前のフィルタを作った
  public static class Hex8DocumentFilter extends DocumentFilter {
    public int mask;
    public char[] buffer;
    public Hex8DocumentFilter (int newMask) {
      mask = newMask;
      buffer = new char[8];
    }
    @Override public void insertString (DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
       replace (fb, offset, 0, string, attr);
     }
    @Override public void remove (DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
     }
    @Override public void replace (DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
       length = text.length ();
       int start = offset;
       for (int i = 0; offset < 8 && i < length; i++) {
         int x = Character.digit (text.charAt (i), 16);
         if (x >= 0) {
           buffer[offset] = XEiJ.fmtHexc (mask >>> (7 - offset) * 4 & x);
           offset++;
         }
       }
       fb.replace (start, offset - start, String.valueOf (buffer, start, offset - start), attrs);
     }
  }  //class Hex8DocumentFilter



  //========================================================================================
  //$$XSM Hex8SpinnerModel
  //  8桁16進数スピナーモデル
  public static class Hex8SpinnerModel extends AbstractSpinnerModel {

    public int value;  //現在の値(相対値)
    public int mask;  //マスク
    public boolean reverse;  //逆回転
    public int[] hintArray;  //ヒント(絶対値)の配列
    public int hintCount;  //ヒント(絶対値)の数。負数=ヒント(絶対値)が設定されていない
    public int hintIndex;  //ヒント(絶対値)のインデックス。負数=現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にない
    public int offset;  //オフセット(絶対値-相対値)

    //コンストラクタ
    public Hex8SpinnerModel (int newValue, int newMask, boolean newReverse) {
      value = newValue & newMask;
      mask = newMask;
      reverse = newReverse;
      hintArray = new int[0];
      hintCount = 0;
      hintIndex = -1;
      offset = 0;
    }

    //value = getNextValue ()
    //  直前の小さい方の値(相対値)を返す
    //  ↑キーを押すか▲ボタンをクリックしたときに呼び出される
    //  ノーマル
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にないとき
    //      マスクが0のビットを避けてインクリメントした値(相対値)を返す
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にあるとき
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の末尾のとき
    //        現在の値(相対値)を返す
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の末尾でないとき
    //        ヒント(絶対値)の配列にある直後の値(絶対値)-オフセット(絶対値-相対値)を返す
    //  リバース
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にないとき
    //      マスクが0のビットを避けてデクリメントした値(相対値)を返す
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にあるとき
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の先頭のとき
    //        現在の値(相対値)を返す
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の先頭でないとき
    //        ヒント(絶対値)の配列にある直前の値(絶対値)-オフセット(絶対値-相対値)を返す
    @Override public Object getNextValue () {
      if (reverse) {
        return (hintCount < 0 || hintIndex < 0 ? (value & mask) - 1 & mask :  //マスクが0のビットを避けてデクリメントした値(相対値)
                hintIndex == 0 ? value :  //現在の値(相対値)
                hintArray[hintIndex - 1] - offset);  //ヒント(絶対値)の配列にある直前の値(絶対値)-オフセット(絶対値-相対値)
      } else {
        return (hintCount < 0 || hintIndex < 0 ? (value | ~mask) + 1 & mask :  //マスクが0のビットを避けてインクリメントした値(相対値)
                hintIndex == hintCount - 1 ? value :  //現在の値(相対値)
                hintArray[hintIndex + 1] - offset);  //ヒント(絶対値)の配列にある直後の値(絶対値)-オフセット(絶対値-相対値)
      }
    }
    //value = getPreviousValue ()
    //  直後の大きい方の値(相対値)を返す
    //  ↓キーを押すか▼ボタンをクリックしたときに呼び出される
    //  ノーマル
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にないとき
    //      マスクが0のビットを避けてデクリメントした値(相対値)を返す
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にあるとき
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の先頭のとき
    //        現在の値(相対値)を返す
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の先頭でないとき
    //        ヒント(絶対値)の配列にある直前の値(絶対値)-オフセット(絶対値-相対値)を返す
    //  リバース
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にないとき
    //      マスクが0のビットを避けてインクリメントした値(相対値)を返す
    //    現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にあるとき
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の末尾のとき
    //        現在の値(相対値)を返す
    //      現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列の末尾でないとき
    //        ヒント(絶対値)の配列にある直後の値(絶対値)-オフセット(絶対値-相対値)を返す
    @Override public Object getPreviousValue () {
      if (reverse) {
        return (hintCount < 0 || hintIndex < 0 ? (value | ~mask) + 1 & mask :  //マスクが0のビットを避けてインクリメントした値(相対値)
                hintIndex == hintCount - 1 ? value :  //現在の値(相対値)
                hintArray[hintIndex + 1] - offset);  //ヒント(絶対値)の配列にある直後の値(絶対値)-オフセット(絶対値-相対値)
      } else {
        return (hintCount < 0 || hintIndex < 0 ? (value & mask) - 1 & mask :  //マスクが0のビットを避けてデクリメントした値(相対値)
                hintIndex == 0 ? value :  //現在の値(相対値)
                hintArray[hintIndex - 1] - offset);  //ヒント(絶対値)の配列にある直前の値(絶対値)-オフセット(絶対値-相対値)
      }
    }
    //value = getValue ()
    //  現在の値(相対値)を文字列で返す
    @Override public Object getValue () {
      return XEiJ.fmtHex8 (getIntValue ());
    }
    //setValue (newValue)
    //  現在の値(相対値)を文字列で設定する
    @Override public void setValue (Object newValue) {
      setIntValue (newValue instanceof Integer ? ((Integer) newValue).intValue () :
                   newValue instanceof String ? (int) Long.parseLong ((String) newValue, 16) : 0);  //0x80000000～0xffffffffは直接intにできない
    }

    //value = getIntValue ()
    //  現在の値(相対値)を整数で返す
    public int getIntValue () {
      return value;
    }
    //setIntValue (newValue)
    //  現在の値(相対値)を整数で設定する
    //  値はマスクされる
    //  現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にあるときはヒント(絶対値)のインデックスも設定する
    //  現在の値(相対値)が変化したときはチェンジリスナーを呼び出す
    public void setIntValue (int newValue) {
      newValue &= mask;
      if (value != newValue) {  //値が変化する
        value = newValue;
        hintIndex = Arrays.binarySearch (hintArray, 0, hintCount, value + offset);  //ヒント(絶対値)のインデックス。現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にないときは負数
        fireStateChanged ();
      }
    }
    //value = getAbsoluteValue ()
    //  現在の値(相対値)+オフセット(絶対値-相対値)を整数で返す
    public int getAbsoluteValue () {
      return getIntValue () + offset;
    }
    //setAbsoluteValue (newValue)
    //  現在の値(相対値)+オフセット(絶対値-相対値)を整数で設定する
    public void setAbsoluteValue (int newValue) {
      setIntValue (newValue - offset);
    }

    //setHintArray (array, count)
    //  ヒント(絶対値)の配列を設定する
    //  現在の値(相対値)は操作しない
    //  配列はコピーされるので、設定後に元の配列に加えられた変更は反映されない
    //  配列はマスク済みで昇順にソートされていなければならない
    //  配列に同じ値が複数あってはならない
    public void setHintArray (int[] array, int count) {
      if (hintArray == null || hintArray.length < count) {  //ヒント(絶対値)の配列が確保されていないか現在の配列に収まらないとき
        hintArray = new int[count];  //ヒント(絶対値)の配列を作り直す
      }
      System.arraycopy (array, 0, hintArray, 0, count);  //ヒント(絶対値)の配列をコピーする
      hintCount = count;
      hintIndex = Arrays.binarySearch (hintArray, 0, hintCount, value + offset);  //ヒント(絶対値)のインデックス。現在の値(相対値)+オフセット(絶対値-相対値)がヒント(絶対値)の配列にないときは負数
    }

    //index = getHintIndex ()
    //  ヒント(絶対値)のインデックスを返す
    public int getHintIndex () {
      return hintIndex;
    }
    //setHingIndex (index)
    //  ヒント(絶対値)のインデックスを設定する
    public void setHintIndex (int index) {
      if (0 <= index && index < hintCount &&
          hintIndex != index) {
        hintIndex = index;
        int newValue = hintArray[index] - offset;
        if (value != newValue) {  //値が変化する
          value = newValue;
          fireStateChanged ();
        }
      }
    }

    //setOffset (newOffset)
    //  オフセット(絶対値-相対値)を設定する
    //  オフセット(絶対値-相対値)はマスクされる
    //  現在の値(相対値)が変化したときはチェンジリスナーを呼び出す
    public void setOffset (int newOffset) {
      newOffset &= mask;
      if (offset != newOffset) {  //値が変化する
        value += offset - newOffset;
        offset = newOffset;
        fireStateChanged ();
      }
    }

  }  //class Hex8SpinnerModel



}  //class Hex8Spinner



