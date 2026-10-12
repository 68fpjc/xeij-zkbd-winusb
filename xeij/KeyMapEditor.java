//========================================================================================
//  KeyMapEditor.java
//    en:Key map editor
//    ja:キーマップエディタ
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.awt.im.*;  //InputContext
import java.io.*;
import java.util.*;
import javax.swing.*;

public class KeyMapEditor implements KeyListener {

  //定数
  public static final Font FONT = new Font ("SansSerif", Font.PLAIN, 12);  //10
  public static final int LABEL_HEIGHT = 14;  //12
  public static final int COL_WIDTH = 14;  //12  列の幅(px)。可変キーの幅の1/4
  public static final int ROW_HEIGHT = 18;  //15  15*4=12+12*4  行の高さ(px)。可変キーの高さの1/4
  public static final int COLS = 94;  //列数
  public static final int ROWS = 25;  //行数
  public static final int PADDING_TOP = 10;  //パディング(px)
  public static final int PADDING_BOTTOM = 10;
  public static final int PADDING_LEFT = 10;
  public static final int PADDING_RIGHT = 10;
  public static final int KEYBOARD_WIDTH = PADDING_LEFT + COL_WIDTH * COLS + PADDING_RIGHT;
  public static final int KEYBOARD_HEIGHT = PADDING_TOP + ROW_HEIGHT * ROWS + PADDING_BOTTOM;
  public static final int KEYS = 113;  //キーの数
  public static final int BOXES = 114;  //箱の数。右SHIFTを追加
  public static final int TAB = 15;  //TAB
  public static final int LEFT_SHIFT = 108;  //左SHIFT
  public static final int RIGHT_SHIFT = 113;  //右SHIFT

  //色
  public static Color backgroundColor;  //キーの背景色
  public static Color assignedColor;  //文字が割り当てられたキーの背景色
  public static Color focusedColor;  //フォーカスされたキーの背景色
  public static Color foregroundColor;  //キーの文字色

  //パネル
  public JPanel keyboardPanel;
  public JComponent mainPanel;

  //マップ
  public int[] currentMap;  //現在のキーマップ。3*KEYS個
  public int focusedBox;  //フォーカスが当たっている箱
  public JTextArea[] textAreaArray;  //箱のテキストエリア

  //保存と復元
  public static javax.swing.filechooser.FileFilter txtFileFilter;
  public static File lastFile;

  //取り消しとやり直し
  public static LinkedList<int[]> undoList;  //取り消しリスト
  public static LinkedList<int[]> redoList;  //やり直しリスト
  public static final int UNDO_LIST_MAX_LENGTH = 1000;  //取り消しリストの長さの上限

  //コンストラクタ
  @SuppressWarnings ("this-escape") public KeyMapEditor (int[] map) {

    //マップ
    currentMap = map;

    //保存と復元
    txtFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String lowerName = name.toLowerCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 (lowerName.endsWith (".csv") ||
                  lowerName.endsWith (".txt"))));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "CSV またはテキストファイル (*.csv,*.txt)" :
                "CSV or text file (*.csv,*.txt)");
      }
    };
    lastFile = new File ("keymap.csv").getAbsoluteFile ();

    //取り消しとやり直し
    undoList = new LinkedList<int[]> ();
    redoList = new LinkedList<int[]> ();

    //色
    backgroundColor = new Color (LnF.lnfRGB[0]);
    assignedColor = new Color (LnF.lnfRGB[4]);
    focusedColor = new Color (LnF.lnfRGB[8]);
    foregroundColor = Color.white;

    //パネル
    keyboardPanel = new JPanel ();
    keyboardPanel.setLayout (null);
    keyboardPanel.setPreferredSize (new Dimension (KEYBOARD_WIDTH, KEYBOARD_HEIGHT));

    //フォーカス
    focusedBox = -1;
    FocusListener focusListener = new FocusAdapter () {
      @Override public void focusGained (FocusEvent fe) {
        int xo = Integer.parseInt (fe.getComponent ().getName ());
        if (focusedBox != -1) {
          Color color = currentMap[3 * xo] != 0 ? assignedColor : backgroundColor;
          textAreaArray[focusedBox].setBackground (color);
          if (focusedBox == LEFT_SHIFT) {
            textAreaArray[RIGHT_SHIFT].setBackground (color);
          }
          //focusedBox = -1;
        }
        focusedBox = xo;
        textAreaArray[focusedBox].setBackground (focusedColor);
        if (focusedBox == LEFT_SHIFT) {
          textAreaArray[RIGHT_SHIFT].setBackground (focusedColor);
        }
      }
      @Override public void focusLost (FocusEvent fe) {
        int xo = Integer.parseInt (fe.getComponent ().getName ());
        if (focusedBox != -1) {
          Color color = currentMap[3 * xo] != 0 ? assignedColor : backgroundColor;
          textAreaArray[focusedBox].setBackground (color);
          if (focusedBox == LEFT_SHIFT) {
            textAreaArray[RIGHT_SHIFT].setBackground (color);
          }
          focusedBox = -1;
        }
      }
    };

    //キーマップ
    textAreaArray = new JTextArea[BOXES];
    for (int xo = 0; xo < BOXES; xo++) {
      textAreaArray[xo] = null;
      int[] bounds = BOUNDS_ARRAY[xo];
      JLabel label = ComponentFactory.createLabel (TEXT_ARRAY[xo]);
      label.setFont (FONT);
      label.setBounds (PADDING_LEFT + COL_WIDTH * bounds[0],
                       PADDING_TOP + ROW_HEIGHT * bounds[1],
                       COL_WIDTH * bounds[2],
                       LABEL_HEIGHT);
      label.setHorizontalAlignment (SwingConstants.CENTER);
      keyboardPanel.add (label);
      JTextArea textArea = new JTextArea ();
      textAreaArray[xo] = textArea;
      textArea.setFont (FONT);
      ComponentFactory.setEtchedBorder (textArea);  //枠を描く
      textArea.setLineWrap (true);  //折り返す
      //textArea.setEditable (false);  //編集不可。キャレットを表示しない。キーイベントが発生しなくなる環境がある？
      textArea.setBackground (backgroundColor);
      textArea.setForeground (foregroundColor);
      textArea.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
      textArea.setName (String.valueOf (xo == RIGHT_SHIFT ? LEFT_SHIFT : xo));  //右SHIFTに左SHIFTの番号を入れる
      textArea.setBounds (PADDING_LEFT + COL_WIDTH * bounds[0],
                          PADDING_TOP + ROW_HEIGHT * bounds[1] + LABEL_HEIGHT,
                          COL_WIDTH * bounds[2],
                          ROW_HEIGHT * bounds[3] - LABEL_HEIGHT);
      if (xo == TAB) {
        textArea.setFocusTraversalKeysEnabled (false);  //Tabを入力できる
      } else {
        textArea.setFocusTraversalKeysEnabled (true);  //Tabで次のキーに移る
      }
      textArea.addKeyListener (this);  //[this-escape]
      textArea.addFocusListener (focusListener);
      keyboardPanel.add (textArea);
    }  //for xo
    updateTextAll ();

    //パネル
    mainPanel = new JScrollPane (keyboardPanel);
    //mainPanel.setPreferredSize (new Dimension (KEYBOARD_WIDTH + 3, KEYBOARD_HEIGHT + 3));
    mainPanel.setPreferredSize (new Dimension (Math.min (700, KEYBOARD_WIDTH + 20), KEYBOARD_HEIGHT + 20));

  }  //コンストラクタ

  //パネルを取得する
  public JComponent getPanel () {
    return mainPanel;
  }  //getPanel

  //白紙にする
  public void blank () {
    if (JOptionPane.showConfirmDialog (
      null,
      Multilingual.mlnJapanese ? "キー割り当てを白紙にしますか？" : "Do you want to blank the key assignments?",
      Multilingual.mlnJapanese ? "確認" : "Confirmation",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE) != JOptionPane.YES_OPTION) {
      return;
    }
    beforeChange ();
    Arrays.fill (currentMap, 0);  //array,value
    updateTextAll ();
  }  //blank

  //初期値に戻す
  public void reset (int[] map) {
    if (JOptionPane.showConfirmDialog (
      null,
      Multilingual.mlnJapanese ? "キー割り当てを初期値に戻しますか？" : "Do you want to reset the key assignments to default?",
      Multilingual.mlnJapanese ? "確認" : "Confirmation",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE) != JOptionPane.YES_OPTION) {
      return;
    }
    beforeChange ();
    System.arraycopy (map, 0,  //from
                      currentMap, 0,  //to
                      currentMap.length);  //length
    updateTextAll ();
  }  //reset

  //保存する
  public void save () {
    JFileChooser2 fileChooser = new JFileChooser2 (lastFile);
    fileChooser.setFileFilter (txtFileFilter);
    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile ();
      String path = file.getPath ();
      String lowerPath = path.toLowerCase ();
      if (lowerPath.endsWith (".csv")) {
        saveCSV (path);
        file = lastFile;
      } else if (lowerPath.endsWith (".txt")) {
        saveText (path);
        file = lastFile;
      }
    }
  }  //save

  //CSV形式で保存する
  public void saveCSV (String path) {
    StringBuilder sb = new StringBuilder ();
    sb.append (XEiJ.prgIsMac ?
               "X68000,keyCode,extendedKeyCode,keyLocation,keytop\r\n" :
               "X68000,keyCode,rawCode,keyLocation,keytop\r\n");
    for (int xo = 0; xo < KEYS; xo++) {
      for (int i = 0; i < 3; i++) {
        int t = currentMap[3 * xo + i];
        if (t == 0) {
          break;
        }
        sb.append (xo + (xo < 108 ? 1 : 4));
        sb.append (",");
        sb.append ((t >> 16) & (XEiJ.prgIsMac ? 0x00000fff : 0x0000ffff));
        sb.append (",");
        sb.append ((t >> 4) & (XEiJ.prgIsMac ? 0x0f000fff : 0x00000fff));
        sb.append (",");
        sb.append (t & 0xf);
        sb.append (",【");  //全角でも"～"で囲んでも数字は数値と見なされる
        sb.append (TEXT_ARRAY[xo]);
        sb.append ("】\r\n");
      }
    }
    XEiJ.rscPutTextFile (path, sb.toString (), "cp932");  //Shift_JISは不可
  }  //saveCSV

  //テキスト形式で保存する
  public void saveText (String path) {
    StringBuilder sb = new StringBuilder ();
    int length = currentMap.length;
    while (0 < length && currentMap[length - 1] == 0) {
      length--;
    }
    sb.append ("keymap=-3");
    for (int i = 0; i < length; i++) {
      sb.append (',');
      if (currentMap[i] != 0) {
        sb.append (currentMap[i]);
      }
    }
    sb.append ("\n");
    XEiJ.rscPutTextFile (path, sb.toString ());
  }  //saveText

  //復元する
  public void restore () {
    JFileChooser2 fileChooser = new JFileChooser2 (lastFile);
    fileChooser.setFileFilter (txtFileFilter);
    if (fileChooser.showOpenDialog (null) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = fileChooser.getSelectedFile ();
    String path = file.getPath ();
    String lowerPath = path.toLowerCase ();
    if (lowerPath.endsWith (".csv")) {
      if (restoreCSV (path)) {
        lastFile = file;
      }
    } else if (lowerPath.endsWith (".txt")) {
      if (restoreText (path)) {
        lastFile = file;
      }
    }
  }  //restore

  //CSV形式で復元する
  public boolean restoreCSV (String path) {
    String string = XEiJ.rscGetTextFile (path, "cp932");  //Shift_JISは不可
    if (string.length () == 0) {  //読み込めないまたは空
      return false;
    }
    String[] lines;
    if (0 <= string.indexOf ("\r\n")) {
      lines = string.split ("\r\n");
    } else if (0 <= string.indexOf ("\n")) {
      lines = string.split ("\n");
    } else if (0 <= string.indexOf ("\r")) {
      lines = string.split ("\r");
    } else {
      lines = new String[] { string };
    }
    int rows = lines.length;
    if (rows < 1) {  //ヘッダがない
      return false;
    }
    int[] map = new int[3 * KEYS];
    Arrays.fill (map, 0);
    for (int row = 0; row < rows; row++) {
      String[] line = lines[row].split (",");  //!!!"～"で囲まれた","があると分割してしまう
      int cols = line.length;
      if (cols < 4) {  //列数が足りない
        return false;
      }
      if (row == 0) {  //ヘッダ
        String cell = line[0];
        if (cell.startsWith ("\"") && cell.endsWith ("\"")) {  //"～"で囲まれている。!!!"\"\""→"\""が必要
          cell = cell.substring (1, cell.length () - 1);  //start,end
        }
        cell = cell.trim ();
        if (!cell.equals ("X68000")) {  //ヘッダの1列目がX68000でない
          return false;
        }
        continue;
      }
      int[] va = new int[4];
      for (int col = 0; col < 4; col++) {
        String cell = line[col];
        if (cell.startsWith ("\"") && cell.endsWith ("\"")) {  //"～"で囲まれている。!!!"\"\""→"\""が必要
          cell = cell.substring (1, cell.length () - 1);  //start,end
        }
        cell = cell.trim ();
        try {
          va[col] = Integer.parseInt (cell, 10);
        } catch (NumberFormatException nfe) {  //intに変換できない
          return false;
        }
      }
      int v0 = va[0];
      int v1 = va[1];
      int v2 = va[2];
      int v3 = va[3];
      if (!(((1 <= v0 && v0 <= 108) || (112 <= v0 && v0 <= 116)) &&
            (v1 & (XEiJ.prgIsMac ? 0x00000fff : 0x0000ffff)) == v1 &&
            (v2 & (XEiJ.prgIsMac ? 0x0f000fff : 0x00000fff)) == v2 &&
            (v3 & 0xf) == v3)) {  //値が範囲外
        return false;
      }
      int t = v1 << 16 | v2 << 4 | v3;
      if (t == 0) {  //0は割り当てられない
        return false;
      }
      int xo = v0 - (v0 < 112 ? 1 : 4);
      if (map[3 * xo] == 0) {  //1個目
        map[3 * xo] = t;
      } else if (map[3 * xo + 1] == 0) {  //2個目
        map[3 * xo + 1] = t;
      } else if (map[3 * xo + 2] == 0) {  //3個目
        map[3 * xo + 2] = t;
      } else {  //同じキーの割り当てが多すぎる
        return false;
      }
    }
    beforeChange ();
    System.arraycopy (map, 0, currentMap, 0, map.length);
    updateTextAll ();
    return true;
  }  //restoreCSV

  //テキスト形式で復元する
  public boolean restoreText (String path) {
    String string = XEiJ.rscGetTextFile (path);
    if (string.length () == 0) {  //読み込めないか空
      return false;
    }
    String[] lr = string.split ("=");
    if (lr.length != 2) {  //左辺=右辺でない
      return false;
    }
    String l = lr[0].trim ();
    String r = lr[1].trim ();
    if (!l.equals ("keymap")) {  //左辺がkeymapでない
      return false;
    }
    String[] sa = r.split (",");
    int[] ia = new int[sa.length];
    for (int i = 0; i < sa.length; i++) {
      String s = sa[i].trim ();  //前後の空白を取り除く
      if (s.length () == 0) {  //""は0と見なす
        ia[i] = 0;
      } else {
        try {
          ia[i] = Integer.parseInt (s, 10);
        } catch (NumberFormatException nfe) {  //intに変換できない
          return false;
        }
      }
    }
    if (ia.length < 1 ||  //要素が足りないか
        1 + 3 * KEYS < ia.length ||  //多すぎるか
        ia[0] != -3) {  //-3で始まっていない
      return false;
    }
    int[] map = new int[3 * KEYS];
    Arrays.fill (map, 0);
    for (int i = 0; i < 3 * KEYS; i++) {
      if (1 + i < ia.length) {
        map[i] = ia[1 + i];
      }
    }
    beforeChange ();
    System.arraycopy (map, 0, currentMap, 0, map.length);
    updateTextAll ();
    return true;
  }  //restoreText

  //取り消す
  public void undo () {
    if (!undoList.isEmpty ()) {  //取り消しリストが空でないとき
      int[] map = new int[currentMap.length];
      System.arraycopy (currentMap, 0, map, 0, currentMap.length);  //現在のマップをコピーして
      redoList.addFirst (map);  //やり直しリストの先頭に追加する
      map = undoList.removeLast ();  //取り消しリストの末尾を削除して
      System.arraycopy (map, 0, currentMap, 0, currentMap.length);  //現在のマップにコピーする
      updateTextAll ();
    }
  }  //undo

  //やり直す
  public void redo () {
    if (!redoList.isEmpty ()) {  //やり直しリストが空でないとき
      int[] map = new int[currentMap.length];
      System.arraycopy (currentMap, 0, map, 0, currentMap.length);  //現在のマップをコピーして
      undoList.addLast (map);  //取り消しリストの末尾に追加する
      map = redoList.removeFirst ();  //やり直しリストの先頭を削除して
      System.arraycopy (map, 0, currentMap, 0, currentMap.length);  //現在のマップにコピーする
      updateTextAll ();
    }
  }  //redo



  //変更前
  public void beforeChange () {
    if (undoList.size () == UNDO_LIST_MAX_LENGTH) {  //取り消しリストの長さが上限のとき
      undoList.removeFirst (); //取り消しリストの先頭を削除する
    }
    redoList.clear ();  //やり直しリストを空にする
    int[] map = new int[currentMap.length];
    System.arraycopy (currentMap, 0, map, 0, currentMap.length);  //現在のマップをコピーして
    undoList.addLast (map);  //取り消しリストの末尾に追加する
  }  //beforeChange



  //キーリスナー
  @Override public void keyPressed (KeyEvent ke) {
    closeIME (ke);
  pressed:
    {
      beforeChange ();
      int xo = Integer.parseInt (ke.getComponent ().getName ());
      int keyCode = ke.getKeyCode ();
      if (true) {
        if (xo != 0 && keyCode == KeyEvent.VK_ESCAPE) {  //ESC以外でEscが押された
          currentMap[3 * xo] = 0;  //全部消す
          currentMap[3 * xo + 1] = 0;
          currentMap[3 * xo + 2] = 0;
          updateText (xo);
          break pressed;
        }
      }
      if ((xo != 108 && keyCode == KeyEvent.VK_SHIFT) ||  //SHIFT以外でShiftが押された
          (xo != 109 && keyCode == KeyEvent.VK_CONTROL)) {  //CTRL以外でCtrlが押された
        break pressed;
      }
      int keyLocation = ke.getKeyLocation ();
      int extendedOrRaw = XEiJ.prgIsMac ? ke.getExtendedKeyCode () : getRawCode (ke);
      int intCode = keyCode << 16 | extendedOrRaw << 4 | keyLocation;
      if ((keyCode & (XEiJ.prgIsMac ? 0x00000fff : 0x0000ffff)) != keyCode ||
          (extendedOrRaw & (XEiJ.prgIsMac ? 0x0f000fff : 0x00000fff)) != extendedOrRaw ||
          (keyLocation & 0x0000000f) != keyLocation ||
          intCode == 0) {  //範囲外
        System.out.printf ("KeyEvent: keyCode=0x%08x, extendedOrRaw=0x%08x, keyLocation=0x%08x\n",
                           keyCode, extendedOrRaw, keyLocation);
        break pressed;
      }
      if (false) {
        if (currentMap[3 * xo] == intCode &&  //1個目にある
            currentMap[3 * xo + 1] == 0) {  //2個目がない
          currentMap[3 * xo] = 0;  //1個目を消す
          updateText (xo);
          break pressed;
        }
      }
      if (currentMap[3 * xo] == intCode ||  //1個目にある
          currentMap[3 * xo + 1] == intCode ||  //2個目にある
          currentMap[3 * xo + 2] == intCode) {  //3個目にある
        currentMap[3 * xo] = intCode;  //1個目にする
        currentMap[3 * xo + 1] = 0;  //2個目を消す
        currentMap[3 * xo + 2] = 0;  //3個目を消す
        updateText (xo);
        break pressed;
      }
      if (currentMap[3 * xo + 2] != 0) {  //3個目があるとき1個目を消して詰める
        currentMap[3 * xo] = currentMap[3 * xo + 1];
        currentMap[3 * xo + 1] = currentMap[3 * xo + 2];
        currentMap[3 * xo + 2] = 0;
      }
      if (currentMap[3 * xo] == 0) {  //1個目がないとき1個目にする
        currentMap[3 * xo] = intCode;
      } else if (currentMap[3 * xo + 1] == 0) {  //2個目がないとき2個目にする
        currentMap[3 * xo + 1] = intCode;
      } else {  //3個目にする
        currentMap[3 * xo + 2] = intCode;
      }
      updateText (xo);
      for (int xp = 0; xp < KEYS; xp++) {
        if (xp != xo) {  //他のキーについて
          if (currentMap[3 * xp] == intCode) {  //1個目にあるとき1個目を消して詰める
            currentMap[3 * xp] = currentMap[3 * xp + 1];
            currentMap[3 * xp + 1] = currentMap[3 * xp + 2];
            currentMap[3 * xp + 2] = 0;
            updateText (xp);
            break;
          }
          if (currentMap[3 * xp + 1] == intCode) {  //2個目にあるとき2個目を消して詰める
            currentMap[3 * xp + 1] = currentMap[3 * xp + 2];
            currentMap[3 * xp + 2] = 0;
            updateText (xp);
            break;
          }
          if (currentMap[3 * xp + 2] == intCode) {  //3個目にあるとき3個目を消す
            currentMap[3 * xp + 2] = 0;
            updateText (xp);
            break;
          }
        }
      }  //for xp
    }  //pressed
    ke.consume ();
  }  //keyPressed

  @Override public void keyReleased (KeyEvent ke) {
    closeIME (ke);
    ke.consume ();
  }  //keyReleased

  @Override public void keyTyped (KeyEvent ke) {
    closeIME (ke);
    ke.consume ();
  }  //keyTyped

  public void closeIME (KeyEvent ke) {
    JTextArea textArea = (JTextArea) ke.getComponent ();
    try {
      InputContext context = textArea.getInputContext ();
      if (context != null && context.isCompositionEnabled ()) {
        context.setCompositionEnabled (false);
        //context.setCharacterSubsets (null);
      }
    } catch (UnsupportedOperationException uoe) {
    }
  }

  public void updateTextAll () {
    for (int xo = 0; xo < KEYS; xo++) {
      updateText (xo);
    }
  }  //updateTextAll

  //キーの文字列を更新する
  public void updateText (int xo) {
    StringBuilder sb = new StringBuilder ();
    for (int i = 0; i < 3; i++) {
      int intCode = currentMap[3 * xo + i];
      if (intCode == 0) {
        if (i == 0) {
          //sb.append (Multilingual.mlnJapanese ? "なし" : "none");
        }
        break;
      }
      if (i != 0) {
        //sb.append (Multilingual.mlnJapanese ? " または " : " or ");
        //sb.append ("\n");
        sb.append (" ");
      }
      int keyCode = (intCode >> 16) & (XEiJ.prgIsMac ? 0x00000fff : 0x0000ffff);
      int extendedOrRaw = (intCode >> 4) & (XEiJ.prgIsMac ? 0x0f000fff : 0x00000fff);
      int keyLocation = intCode & 0x0000000f;
      switch (keyLocation) {
      case 2:  //LEFT
        sb.append (Multilingual.mlnJapanese ? "左" : "Left ");
        break;
      case 3:  //RIGHT
        sb.append (Multilingual.mlnJapanese ? "右" : "Right ");
        break;
      case 4:  //NUMPAD
        //sb.append (Multilingual.mlnJapanese ? "テンキー" : "Numpad ");
        sb.append ("#");
        break;
      }
      sb.append (KeyEvent.getKeyText (XEiJ.prgIsMac && extendedOrRaw != 0 ? extendedOrRaw : keyCode));
    }
    String text = sb.toString ();
    if (!text.equals (textAreaArray[xo].getText ())) {
      Color color = xo == focusedBox ? focusedColor : currentMap[3 * xo] != 0 ? assignedColor : backgroundColor;
      textAreaArray[xo].setText (text);
      textAreaArray[xo].setBackground (color);
      if (xo == LEFT_SHIFT) {  //左SHIFTを更新するとき右SHIFTも更新する
        textAreaArray[RIGHT_SHIFT].setText (text);
        textAreaArray[RIGHT_SHIFT].setBackground (color);
      }
    }
  }  //updateText

  //rawCode = getRawCode (ke)
  //  KeyEventからrawCodeを取り出す
  public int getRawCode (KeyEvent ke) {
    int rawCode = 0;
    //KeyEvent.paramString()で出力される文字列の中からrawCode=～を取り出す
    String s = ke.paramString ();
    int i = s.indexOf ("rawCode=");
    if (0 <= i) {
      i += 8;
      for (int k = s.length (); i < k; i++) {
        char c = s.charAt (i);
        if (c < '0' || '9' < c) {
          break;
        }
        rawCode = rawCode * 10 + (c - '0');
      }
    }
    return rawCode;
  }  //getRawCode

  //位置
  public static final int[][] BOUNDS_ARRAY = {
    {  0,  5,  4, 4 },  //  0  0x01  ESC
    {  4,  5,  4, 4 },  //  1  0x02  １！ぬ　
    {  8,  5,  4, 4 },  //  2  0x03  ２＂ふ　
    { 12,  5,  4, 4 },  //  3  0x04  ３＃あぁ
    { 16,  5,  4, 4 },  //  4  0x05  ４＄うぅ
    { 20,  5,  4, 4 },  //  5  0x06  ５％えぇ
    { 24,  5,  4, 4 },  //  6  0x07  ６＆おぉ
    { 28,  5,  4, 4 },  //  7  0x08  ７＇やゃ
    { 32,  5,  4, 4 },  //  8  0x09  ８（ゆゅ
    { 36,  5,  4, 4 },  //  9  0x0a  ９）よょ
    { 40,  5,  4, 4 },  // 10  0x0b  ０　わを
    { 44,  5,  4, 4 },  // 11  0x0c  －＝ほ　
    { 48,  5,  4, 4 },  // 12  0x0d  ＾～へ　
    { 52,  5,  4, 4 },  // 13  0x0e  ￥｜ー　
    { 56,  5,  6, 4 },  // 14  0x0f  BS
    {  0,  9,  6, 4 },  // 15  0x10  TAB
    {  6,  9,  4, 4 },  // 16  0x11  Ｑ　た　
    { 10,  9,  4, 4 },  // 17  0x12  Ｗ　て　
    { 14,  9,  4, 4 },  // 18  0x13  Ｅ　いぃ
    { 18,  9,  4, 4 },  // 19  0x14  Ｒ　す　
    { 22,  9,  4, 4 },  // 20  0x15  Ｔ　か　
    { 26,  9,  4, 4 },  // 21  0x16  Ｙ　ん　
    { 30,  9,  4, 4 },  // 22  0x17  Ｕ　な　
    { 34,  9,  4, 4 },  // 23  0x18  Ｉ　に　
    { 38,  9,  4, 4 },  // 24  0x19  Ｏ　ら　
    { 42,  9,  4, 4 },  // 25  0x1a  Ｐ　せ　
    { 46,  9,  4, 4 },  // 26  0x1b  ＠｀゛　
    { 50,  9,  4, 4 },  // 27  0x1c  ［｛゜「
    { 55,  9,  7, 8 },  // 28  0x1d  リターン
    {  7, 13,  4, 4 },  // 29  0x1e  Ａ　ち　
    { 11, 13,  4, 4 },  // 30  0x1f  Ｓ　と　
    { 15, 13,  4, 4 },  // 31  0x20  Ｄ　し　
    { 19, 13,  4, 4 },  // 32  0x21  Ｆ　は　
    { 23, 13,  4, 4 },  // 33  0x22  Ｇ　き　
    { 27, 13,  4, 4 },  // 34  0x23  Ｈ　く　
    { 31, 13,  4, 4 },  // 35  0x24  Ｊ　ま　
    { 35, 13,  4, 4 },  // 36  0x25  Ｋ　の　
    { 39, 13,  4, 4 },  // 37  0x26  Ｌ　り　
    { 43, 13,  4, 4 },  // 38  0x27  ；＋れ　
    { 47, 13,  4, 4 },  // 39  0x28  ：＊け　
    { 51, 13,  4, 4 },  // 40  0x29  ］｝む」
    {  9, 17,  4, 4 },  // 41  0x2a  Ｚ　つっ
    { 13, 17,  4, 4 },  // 42  0x2b  Ｘ　さ　
    { 17, 17,  4, 4 },  // 43  0x2c  Ｃ　そ　
    { 21, 17,  4, 4 },  // 44  0x2d  Ｖ　ひ　
    { 25, 17,  4, 4 },  // 45  0x2e  Ｂ　こ　
    { 29, 17,  4, 4 },  // 46  0x2f  Ｎ　み　
    { 33, 17,  4, 4 },  // 47  0x30  Ｍ　も　
    { 37, 17,  4, 4 },  // 48  0x31  ，＜ね、
    { 41, 17,  4, 4 },  // 49  0x32  ．＞る。
    { 45, 17,  4, 4 },  // 50  0x33  ／？め・
    { 49, 17,  4, 4 },  // 51  0x34  　＿ろ□
    { 21, 21, 14, 4 },  // 52  0x35  スペース
    { 64,  5,  4, 4 },  // 53  0x36  HOME
    { 72,  5,  4, 4 },  // 54  0x37  DEL
    { 64,  9,  4, 4 },  // 55  0x38  ROLLUP
    { 68,  9,  4, 4 },  // 56  0x39  ROLLDOWN
    { 72,  9,  4, 4 },  // 57  0x3a  UNDO
    { 64, 13,  4, 8 },  // 58  0x3b  ←
    { 68, 13,  4, 4 },  // 59  0x3c  ↑
    { 72, 13,  4, 8 },  // 60  0x3d  →
    { 68, 17,  4, 4 },  // 61  0x3e  ↓
    { 78,  5,  4, 4 },  // 62  0x3f  CLR
    { 82,  5,  4, 4 },  // 63  0x40  ／
    { 86,  5,  4, 4 },  // 64  0x41  ＊
    { 90,  5,  4, 4 },  // 65  0x42  －
    { 78,  9,  4, 4 },  // 66  0x43  ７
    { 82,  9,  4, 4 },  // 67  0x44  ８
    { 86,  9,  4, 4 },  // 68  0x45  ９
    { 90,  9,  4, 4 },  // 69  0x46  ＋
    { 78, 13,  4, 4 },  // 70  0x47  ４
    { 82, 13,  4, 4 },  // 71  0x48  ５
    { 86, 13,  4, 4 },  // 72  0x49  ６
    { 90, 13,  4, 4 },  // 73  0x4a  ＝
    { 78, 17,  4, 4 },  // 74  0x4b  １
    { 82, 17,  4, 4 },  // 75  0x4c  ２
    { 86, 17,  4, 4 },  // 76  0x4d  ３
    { 90, 17,  4, 8 },  // 77  0x4e  ENTER
    { 78, 21,  4, 4 },  // 78  0x4f  ０
    { 82, 21,  4, 4 },  // 79  0x50  ，
    { 86, 21,  4, 4 },  // 80  0x51  ．
    { 82,  0,  4, 4 },  // 81  0x52  記号入力
    { 86,  0,  4, 4 },  // 82  0x53  登録
    { 90,  0,  4, 4 },  // 83  0x54  HELP
    { 11, 21,  5, 4 },  // 84  0x55  XF1
    { 16, 21,  5, 4 },  // 85  0x56  XF2
    { 35, 21,  6, 4 },  // 86  0x57  XF3
    { 41, 21,  5, 4 },  // 87  0x58  XF4
    { 46, 21,  5, 4 },  // 88  0x59  XF5
    { 64,  0,  4, 4 },  // 89  0x5a  かな
    { 68,  0,  4, 4 },  // 90  0x5b  ローマ字
    { 72,  0,  4, 4 },  // 91  0x5c  コード入力
    { 78,  0,  4, 4 },  // 92  0x5d  CAPS
    { 68,  5,  4, 4 },  // 93  0x5e  INS
    {  7, 21,  4, 4 },  // 94  0x5f  ひらがな
    { 51, 21,  4, 4 },  // 95  0x60  全角
    {  0,  0,  4, 4 },  // 96  0x61  BREAK
    {  5,  0,  4, 4 },  // 97  0x62  COPY
    { 11,  1,  5, 3 },  // 98  0x63  F1
    { 16,  1,  5, 3 },  // 99  0x64  F2
    { 21,  1,  5, 3 },  //100  0x65  F3
    { 26,  1,  5, 3 },  //101  0x66  F4
    { 31,  1,  5, 3 },  //102  0x67  F5
    { 37,  1,  5, 3 },  //103  0x68  F6
    { 42,  1,  5, 3 },  //104  0x69  F7
    { 47,  1,  5, 3 },  //105  0x6a  F8
    { 52,  1,  5, 3 },  //106  0x6b  F9
    { 57,  1,  5, 3 },  //107  0x6c  F10
    {  0, 17,  9, 4 },  //108  0x70  SHIFT
    {  0, 13,  7, 4 },  //109  0x71  CTRL
    { 64, 21,  6, 4 },  //110  0x72  OPT.1
    { 70, 21,  6, 4 },  //111  0x73  OPT.2
    {  0, 21,  4, 4 },  //112  0x74  NUM
    //
    { 53, 17,  9, 4 },  //113  0x70  右SHIFT
  };

  //文字
  public static final String[] TEXT_ARRAY = (
    "ESC,"        +  //  0  0x01
    "１！ぬ　,"   +  //  1  0x02
    "２＂ふ　,"   +  //  2  0x03
    "３＃あぁ,"   +  //  3  0x04
    "４＄うぅ,"   +  //  4  0x05
    "５％えぇ,"   +  //  5  0x06
    "６＆おぉ,"   +  //  6  0x07
    "７＇やゃ,"   +  //  7  0x08
    "８（ゆゅ,"   +  //  8  0x09
    "９）よょ,"   +  //  9  0x0a
    "０　わを,"   +  // 10  0x0b
    "－＝ほ　,"   +  // 11  0x0c
    "＾～へ　,"   +  // 12  0x0d
    "￥｜ー　,"   +  // 13  0x0e
    "BS,"         +  // 14  0x0f
    "TAB,"        +  // 15  0x10
    "Ｑ　た　,"   +  // 16  0x11
    "Ｗ　て　,"   +  // 17  0x12
    "Ｅ　いぃ,"   +  // 18  0x13
    "Ｒ　す　,"   +  // 19  0x14
    "Ｔ　か　,"   +  // 20  0x15
    "Ｙ　ん　,"   +  // 21  0x16
    "Ｕ　な　,"   +  // 22  0x17
    "Ｉ　に　,"   +  // 23  0x18
    "Ｏ　ら　,"   +  // 24  0x19
    "Ｐ　せ　,"   +  // 25  0x1a
    "＠｀゛　,"   +  // 26  0x1b
    "［｛゜「,"   +  // 27  0x1c
    "リターン,"   +  // 28  0x1d
    "Ａ　ち　,"   +  // 29  0x1e
    "Ｓ　と　,"   +  // 30  0x1f
    "Ｄ　し　,"   +  // 31  0x20
    "Ｆ　は　,"   +  // 32  0x21
    "Ｇ　き　,"   +  // 33  0x22
    "Ｈ　く　,"   +  // 34  0x23
    "Ｊ　ま　,"   +  // 35  0x24
    "Ｋ　の　,"   +  // 36  0x25
    "Ｌ　り　,"   +  // 37  0x26
    "；＋れ　,"   +  // 38  0x27
    "：＊け　,"   +  // 39  0x28
    "］｝む」,"   +  // 40  0x29
    "Ｚ　つっ,"   +  // 41  0x2a
    "Ｘ　さ　,"   +  // 42  0x2b
    "Ｃ　そ　,"   +  // 43  0x2c
    "Ｖ　ひ　,"   +  // 44  0x2d
    "Ｂ　こ　,"   +  // 45  0x2e
    "Ｎ　み　,"   +  // 46  0x2f
    "Ｍ　も　,"   +  // 47  0x30
    "，＜ね、,"   +  // 48  0x31
    "．＞る。,"   +  // 49  0x32
    "／？め・,"   +  // 50  0x33
    "　＿ろ□,"   +  // 51  0x34
    "スペース,"   +  // 52  0x35
    "HOME,"       +  // 53  0x36
    "DEL,"        +  // 54  0x37
    "ROLLUP,"     +  // 55  0x38
    "ROLLDOWN,"   +  // 56  0x39
    "UNDO,"       +  // 57  0x3a
    "←,"         +  // 58  0x3b
    "↑,"         +  // 59  0x3c
    "→,"         +  // 60  0x3d
    "↓,"         +  // 61  0x3e
    "CLR,"        +  // 62  0x3f
    "／,"         +  // 63  0x40
    "＊,"         +  // 64  0x41
    "－,"         +  // 65  0x42
    "７,"         +  // 66  0x43
    "８,"         +  // 67  0x44
    "９,"         +  // 68  0x45
    "＋,"         +  // 69  0x46
    "４,"         +  // 70  0x47
    "５,"         +  // 71  0x48
    "６,"         +  // 72  0x49
    "＝,"         +  // 73  0x4a
    "１,"         +  // 74  0x4b
    "２,"         +  // 75  0x4c
    "３,"         +  // 76  0x4d
    "ENTER,"      +  // 77  0x4e
    "０,"         +  // 78  0x4f
    "，,"         +  // 79  0x50
    "．,"         +  // 80  0x51
    "記号入力,"   +  // 81  0x52
    "登録,"       +  // 82  0x53
    "HELP,"       +  // 83  0x54
    "XF1,"        +  // 84  0x55
    "XF2,"        +  // 85  0x56
    "XF3,"        +  // 86  0x57
    "XF4,"        +  // 87  0x58
    "XF5,"        +  // 88  0x59
    "かな,"       +  // 89  0x5a
    "ローマ字,"   +  // 90  0x5b
    "コード入力," +  // 91  0x5c
    "CAPS,"       +  // 92  0x5d
    "INS,"        +  // 93  0x5e
    "ひらがな,"   +  // 94  0x5f
    "全角,"       +  // 95  0x60
    "BREAK,"      +  // 96  0x61
    "COPY,"       +  // 97  0x62
    "F1,"         +  // 98  0x63
    "F2,"         +  // 99  0x64
    "F3,"         +  //100  0x65
    "F4,"         +  //101  0x66
    "F5,"         +  //102  0x67
    "F6,"         +  //103  0x68
    "F7,"         +  //104  0x69
    "F8,"         +  //105  0x6a
    "F9,"         +  //106  0x6b
    "F10,"        +  //107  0x6c
    "SHIFT,"      +  //108  0x70
    "CTRL,"       +  //109  0x71
    "OPT.1,"      +  //110  0x72
    "OPT.2,"      +  //111  0x73
    "NUM,"        +  //112  0x74
    //
    "SHIFT"          //113  0x70
    ).split (",");

}  //class KeyMapEditor

