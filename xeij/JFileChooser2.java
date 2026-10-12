//========================================================================================
//  JFileChooser2.java
//    en:JFileChooser2 -- Modified JFileChooser
//    ja:JFileChooser2 -- JFileChooserの改造
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//JFileChooserの改造
//  履歴
//    ファイルチューザーのファイル名のテキストフィールドをコンボボックスに変更する
//    コンボボックスのリストに最近使ったファイルを表示して簡単に選択できるようにする
//  バグ対策
//    ファイルチューザーのテキストフィールドに入力されたファイル名を確実に取り出せるようにする
//    JFileChooserのgetSelectedFile()の説明には
//      Returns the selected file. This can be set either by the programmer via setSelectedFile or by a user action,
//      such as either typing the filename into the UI or selecting the file from a list in the UI.
//    と書かれており、テキストフィールドに入力されたファイル名もgetSelectedFile()で取り出せることになっている
//    しかし、実際にはsetSelectedFile()で設定したかファイルの一覧をクリックして選択したファイル名しか取り出すことができない
//    これでは新しいファイルを作れないだけでなく、
//    ファイルの一覧をクリックしてテキストフィールドに既存のファイル名を表示させた後にそれを書き換えて新規のファイル名を入力すると、
//    入力した新規のファイル名ではなくクリックした既存のファイル名が返るため、既存のファイルを破壊してしまう可能性がある
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import java.util.regex.*;  //Matcher,Pattern
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument

public class JFileChooser2 extends JFileChooser {

  private static final boolean DEBUG = false;

  //履歴
  public static final int MAXIMUM_HISTORY_COUNT = 100;  //履歴の長さの上限
  public static final int MAXIMUM_ROW_COUNT = 10;  //コンボボックスの行数の上限
  protected ArrayList<File[]> history;  //履歴。消去するとnew File[0]だけになる。空にはならない

  //コンポーネント
  protected JTextField formerTextField;  //元のテキストフィールド
  protected JComboBox<String> comboBox;  //コンボボックス
  protected JTextField comboBoxTextField;  //コンボボックスのテキストフィールド
  protected int ignoreItemEvent;  //コンボボックスのアイテムイベントを0=処理する,0以外=処理しない

  //テキストフィールドのファイル名の正規表現
  //  ファイル名をそのまま書くまたは"～"で囲んで書く
  //  ' 'または','で区切って複数のファイル名を書ける
  //  ' 'または','を含むファイル名は"～"で囲めば書ける
  //  '"'を含むファイル名は書けない
  //  0文字のファイル名は書けない
  protected static final Pattern NAME_PATTERN = Pattern.compile ("\\s*+(?:,\\s*+)*+(?:([^\",]++)|\"([^\"]++)\"?+)");

  //コンストラクタ
  public JFileChooser2 () {
    this (new File[0]);
  }
  public JFileChooser2 (File file) {
    this (new File[] { file });
  }
  @SuppressWarnings ("this-escape") public JFileChooser2 (File[] files) {
    history = new ArrayList<File[]> ();
    history.add (files);
    formerTextField = null;
    comboBox = null;
    comboBoxTextField = null;
    ignoreItemEvent = 0;
    //元のテキストフィールドを求める
    //  ファイルチューザーの構造が異なると失敗する可能性がある
    if (false) {
      ComponentFactory.printComponentTree (this);  //[this-escape]
      //  0.3. javax.swing.JPanel
      //    0.3.0. javax.swing.JPanel
      //      0.3.0.0. javax.swing.plaf.metal.MetalFileChooserUI$AlignedLabel < javax.swing.JLabel
      //      0.3.0.1. javax.swing.plaf.metal.MetalFileChooserUI$3 < javax.swing.JTextField
    }
    JPanel fileNamePanel;
    try {
      fileNamePanel = (JPanel) ((JPanel) getComponent (3)).getComponent (0);  //ファイル名のパネル
      formerTextField = (JTextField) fileNamePanel.getComponent (1);  //元のテキストフィールド
    } catch (Exception e) {
      e.printStackTrace ();
      return;
    }
    //コンボボックスを作る
    //  元のテキストフィールドはJTextFieldの名無しサブクラスなので直接置き換えるのは難しい
    //  元のテキストフィールドを削除してコンボボックスを追加する
    //  元のテキストフィールドのテキストが更新されたらコンボボックスにコピーする
    comboBox = new JComboBox<String> (new String[0]);  //コンボボックス
    comboBox.setEditable (true);  //編集可能
    comboBox.setMaximumRowCount (MAXIMUM_ROW_COUNT);  //最大行数
    //コンボボックスのテキストフィールドを求める
    comboBoxTextField = (JTextField) comboBox.getEditor ().getEditorComponent ();  //コンボボックスのテキストフィールド
    comboBoxTextField.setColumns (formerTextField.getColumns ());  //桁数をコピーする
    comboBoxTextField.setText (formerTextField.getText ());  //元のテキストフィールドのテキストをコンボボックスにコピーする
    //元のテキストフィールドを削除する
    fileNamePanel.remove (formerTextField);
    //コンボボックスを追加する
    fileNamePanel.add (comboBox);
    //再配置する
    fileNamePanel.validate ();
    //元のテキストフィールドにドキュメントリスナーを設定する
    ((AbstractDocument) formerTextField.getDocument ()).addDocumentListener (new DocumentListener () {
      @Override public void changedUpdate (DocumentEvent de) {
        if (DEBUG) {
          System.out.println ("formerTextField.changedUpdate");
          System.out.println ("  getText()=\"" + formerTextField.getText () + "\"");
        }
      }
      @Override public void insertUpdate (DocumentEvent de) {
        if (DEBUG) {
          System.out.println ("formerTextField.insertUpdate");
          System.out.println ("  getText()=\"" + formerTextField.getText () + "\"");
        }
        if (ignoreItemEvent == 0) {
          ignoreItemEvent++;
          comboBoxTextField.setText (formerTextField.getText ());  //元のテキストフィールドのテキストをコンボボックスにコピーする
          ignoreItemEvent--;
        }
      }
      @Override public void removeUpdate (DocumentEvent de) {
        if (DEBUG) {
          System.out.println ("formerTextField.removeUpdate");
          System.out.println ("  getText()=\"" + formerTextField.getText () + "\"");
        }
        if (ignoreItemEvent == 0) {
          ignoreItemEvent++;
          comboBoxTextField.setText (formerTextField.getText ());  //元のテキストフィールドのテキストをコンボボックスにコピーする
          ignoreItemEvent--;
        }
      }
    });
    //コンボボックスのテキストフィールドにドキュメントリスナーを設定する
    ((AbstractDocument) comboBoxTextField.getDocument ()).addDocumentListener (new DocumentListener () {
      @Override public void changedUpdate (DocumentEvent de) {
        if (DEBUG) {
          System.out.println ("comboBoxTextField.changedUpdate");
          System.out.println ("  getText()=\"" + formerTextField.getText () + "\"");
        }
      }
      @Override public void insertUpdate (DocumentEvent de) {
        if (DEBUG) {
          System.out.println ("comboBoxTextField.insertUpdate");
          System.out.println ("  getText()=\"" + formerTextField.getText () + "\"");
        }
        if (ignoreItemEvent == 0) {
          ignoreItemEvent++;
          setSelectedFiles (namesToFiles (comboBoxTextField.getText ()));  //コンボボックスのテキストのファイルを選択する
          ignoreItemEvent--;
        }
      }
      @Override public void removeUpdate (DocumentEvent de) {
        if (DEBUG) {
          System.out.println ("comboBoxTextField.removeUpdate");
          System.out.println ("  getText()=\"" + formerTextField.getText () + "\"");
        }
        if (ignoreItemEvent == 0) {
          ignoreItemEvent++;
          setSelectedFiles (namesToFiles (comboBoxTextField.getText ()));  //コンボボックスのテキストのファイルを選択する
          ignoreItemEvent--;
        }
      }
    });
    //コンボボックスにアイテムリスナーを設定する
    comboBox.addItemListener (new ItemListener () {
      @Override public void itemStateChanged (ItemEvent ie) {
        if (DEBUG) {
          System.out.println ("comboBox.itemStateChanged");
          System.out.println ("  getSelectedIndex()=" + comboBox.getSelectedIndex ());
          System.out.println ("  getText()=" + comboBoxTextField.getText ());
        }
        if (ignoreItemEvent == 0) {
          ignoreItemEvent++;
          if (ie.getStateChange () == ItemEvent.SELECTED) {
            int i = comboBox.getSelectedIndex ();
            if (0 <= i) {
              if (i < history.size ()) {
                File[] files = history.get (i);  //選択されたファイルを
                history.remove (i);
                history.add (0, files);  //履歴の先頭に移す
              } else {
                clearHistory ();  //履歴を消去する
                //キャンセルしたときも消去が選択されたままなので元に戻す
              }
              historyToComboBox ();  //履歴をコンボボックスにコピーする
              selectLastFiles ();  //履歴の先頭のファイルを選択する
            }
          }
          ignoreItemEvent--;
        }
      }
    });
    historyToComboBox ();  //履歴をコンボボックスにコピーする
    selectLastFiles ();  //履歴の先頭のファイルを選択する
  }

  //clearHistory ()
  //  履歴を消去する
  //  コンボボックスは操作しない
  public void clearHistory () {
    XEiJ.pnlExitFullScreen (true);
    if (JOptionPane.showConfirmDialog (
      this,
      Multilingual.mlnJapanese ? "履歴を消去しますか？" : "Do you want to clear history?",
      Multilingual.mlnJapanese ? "確認" : "Confirmation",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION) {
      history.clear ();  //履歴を消去する
      history.add (0, new File[0]);
    }
  }

  //selectLastFiles ()
  //  履歴の先頭のファイルを選択する
  public void selectLastFiles () {
    if (DEBUG) {
      System.out.println ("selectLastFiles");
      if (history.size () != 0) {
        System.out.println ("  " + filesToPaths (history.get (0)));
      }
    }
    if (history.size () != 0) {
      File[] files = history.get (0);
      if (files.length == 0) {
        setCurrentDirectory (new File (".").getAbsoluteFile ().getParentFile ());
      }
      setSelectedFiles (files);
    }
  }

  //file = getSelectedFile ()
  //  選択されたファイルを返す
  //  選択されたファイルがないときはnullを返す
  @Override public File getSelectedFile () {
    if (DEBUG) {
      System.out.println ("getSelectedFile");
    }
    File[] files = getSelectedFiles ();
    return files.length == 0 ? null : files[0];
  }

  //files = getSelectedFiles ()
  //  選択された複数のファイルを返す
  //  選択されたファイルがないときは長さ0の配列を返す
  @Override public File[] getSelectedFiles () {
    if (DEBUG) {
      System.out.println ("getSelectedFiles");
    }
    if (comboBox == null) {  //コンストラクタの中で準備ができていない
      return super.getSelectedFiles ();
    }
    File[] files = namesToFiles (comboBoxTextField.getText ());
    if (DEBUG) {
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        System.out.println ("  " + file.getPath ());
      }
    }
    return files;
  }

  //setSelectedFile (file)
  //  ファイルを選択する
  @Override public void setSelectedFile (File file) {
    if (DEBUG) {
      System.out.println ("setSelectedFile");
      if (file != null) {
        System.out.println ("  " + file.getPath ());
      }
    }
    //setSelectedFiles (new File[] { file });
    //ignoreItemEvent++;
    super.setSelectedFile (file);
    //ignoreItemEvent--;
  }

  //setSelectedFiles (files)
  //  複数のファイルを選択する
  @Override public void setSelectedFiles (File[] files) {
    if (DEBUG) {
      System.out.println ("setSelectedFiles");
      if (files != null) {
        for (File file : files) {
          System.out.println ("  " + file.getPath ());
        }
      }
    }
    //ディレクトリを選択すると親ディレクトリが開いてしまうので末尾に"./"を付ける
    File[] dotFiles = null;
    if (files != null) {
      dotFiles = new File[files.length];
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        dotFiles[i] = file.isDirectory () ? new File (file, ".") : file;
      }
    }
    //ignoreItemEvent++;
    super.setSelectedFiles (dotFiles);
    //ignoreItemEvent--;
  }

  //addHistory (file)
  //  ファイルを履歴に追加する
  public void addHistory (File file) {
    if (DEBUG) {
      System.out.println ("addHistory");
      if (file != null) {
        System.out.println ("  " + file.getPath ());
      }
    }
    if (file != null) {
      addHistory (new File[] { file });
    }
  }

  //addHistory (paths)
  //  パス名を並べた文字列をファイルの配列に変換して履歴に追加する
  public void addHistory (String paths) {
    addHistory (pathsToFiles (paths));
  }  //addHistory

  //addHistory (files)
  //  複数のファイルを履歴に追加する
  public void addHistory (File[] files) {
    if (DEBUG) {
      System.out.println ("addHistory");
      if (files != null) {
        for (File file : files) {
          System.out.println ("  " + file.getPath ());
        }
      }
    }
    if (files == null || files.length == 0) {  //ファイルがない
      return;
    }
    for (int i = 0; i < history.size (); i++) {
      File[] files2 = history.get (i);
      if (files2.length == 0 ||  //空
          Arrays.equals (files, files2)) {  //一致
        history.remove (i);  //削除する
        i--;
      }
    }
    if (history.size () == MAXIMUM_HISTORY_COUNT) {  //一杯
      history.remove (MAXIMUM_HISTORY_COUNT - 1);  //末尾を削る
    }
    history.add (0, files);  //先頭に追加する
    historyToComboBox ();  //履歴をコンボボックスにコピーする
  }

  //pathsList = getHistory ()
  //  履歴をパス名を並べた文字列のリストに変換する
  public ArrayList<String> getHistory () {
    if (DEBUG) {
      System.out.println ("getHistory");
    }
    ArrayList<String> pathsList = new ArrayList<String> ();
    for (File[] files : history) {
      pathsList.add (filesToPaths (files));
    }
    return pathsList;
  }

  //setHistory (pathsList)
  //  パス名を並べた文字列のリストを履歴に変換する
  public void setHistory (ArrayList<String> pathsList) {
    if (DEBUG) {
      System.out.println ("setHistory");
      if (pathsList != null) {
        for (String paths : pathsList) {
          System.out.println ("  " + paths);
        }
      }
    }
    history.clear ();
  list:
    for (String paths : pathsList) {  //新しい順
      File[] files = pathsToFiles (paths);
      if (files.length == 0) {  //ファイルがない
        continue list;
      }
      for (File[] newerFiles : history) {
        if (Arrays.equals (files, newerFiles)) {  //既にある
          continue list;
        }
      }
      history.add (files);  //末尾に追加する
      if (history.size () == MAXIMUM_HISTORY_COUNT) {  //一杯になった
        break list;
      }
    }
    historyToComboBox ();  //履歴をコンボボックスにコピーする
  }

  //historyToComboBox ()
  //  履歴をコンボボックスにコピーする
  protected void historyToComboBox () {
    if (DEBUG) {
      System.out.println ("historyToComboBox");
      for (File[] files : history) {
        System.out.println ("  " + filesToPaths (files));
      }
    }
    ignoreItemEvent++;
    comboBox.removeAllItems ();
    for (File[] files : history) {
      comboBox.addItem (filesToNames (files));
    }
    comboBox.addItem (Multilingual.mlnJapanese ?
                      "---------- 履歴を消去する ----------" :
                      "---------- Clear history ----------");
    ignoreItemEvent--;
  }

  //names = filesToNames (files)
  //  ファイルの配列をファイル名を並べた文字列に変換する
  public String filesToNames (File[] files) {
    StringBuilder sb = new StringBuilder ();
    int n = files.length;
    if (n == 0) {
    } else if (n == 1) {
      sb.append (files[0].getName ());  //ファイル名
    } else {
      for (int i = 0; i < n; i++) {
        if (0 < i) {
          sb.append (' ');
        }
        sb.append ('"').append (files[i].getName ()).append ('"');  //ファイル名
      }
    }
    return sb.toString ();
  }

  //files = namesToFiles (names)
  //  ファイル名を並べた文字列をファイルの配列に変換する
  //  ファイル名がないときは長さ0の配列を返す
  public File[] namesToFiles (String names) {
    File directory = getCurrentDirectory ();  //現在のディレクトリ
    ArrayList<File> fileList = new ArrayList<File> ();
    Matcher matcher = NAME_PATTERN.matcher (names);
    while (matcher.find ()) {
      String name = matcher.group (1) != null ? matcher.group (1) : matcher.group (2);
      File file = new File (directory, name).getAbsoluteFile ();
      if (!fileList.contains (file)) {  //重複しない
        fileList.add (file);
      }
    }
    return fileList.toArray (new File[fileList.size ()]);
  }

  //paths = filesToPaths (files)
  //  ファイルの配列をパス名を並べた文字列に変換する
  public static String filesToPaths (File[] files) {
    StringBuilder sb = new StringBuilder ();
    int n = files.length;
    if (n == 0) {
    } else if (n == 1) {
      sb.append (files[0].getPath ());  //パス名
    } else {
      for (int i = 0; i < n; i++) {
        if (0 < i) {
          sb.append (' ');
        }
        sb.append ('"').append (files[i].getPath ()).append ('"');  //パス名
      }
    }
    return sb.toString ();
  }

  //files = pathsToFiles (paths)
  //  パス名を並べた文字列をファイルの配列に変換する
  //  パス名がないときは長さ0の配列を返す
  public static File[] pathsToFiles (String paths) {
    ArrayList<File> fileList = new ArrayList<File> ();
    Matcher matcher = NAME_PATTERN.matcher (paths);
    while (matcher.find ()) {
      String path = matcher.group (1) != null ? matcher.group (1) : matcher.group (2);
      File file = new File (path).getAbsoluteFile ();
      if (!fileList.contains (file)) {  //重複しない
        fileList.add (file);
      }
    }
    return fileList.toArray (new File[fileList.size ()]);
  }

}  //class JFileChooser2



