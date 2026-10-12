//========================================================================================
//  ScrollTextArea.java
//    en:Text area with scroll bars -- It is a modified JScrollPage that has a JTextArea as the view.
//    ja:スクロールバー付きテキストエリア -- JTextAreaをビューに持つJScrollPaneです。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  追加機能
//    背景にグリッドを表示できる
//    ハイライトエリアを表示できる
//    ハイライトカーソルを表示できる
//    アンダーラインカーソルを表示できる
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument

public class ScrollTextArea extends JScrollPane {

  private boolean opaqueOn;  //true=背景を表示する
  private boolean gridOn;  //true=グリッドを表示する
  private boolean highlightCursorOn;  //true=ハイライトカーソルを表示する
  private boolean underlineCursorOn;  //true=アンダーラインカーソルを表示する

  private Color foregroundColor;  //文字の色
  private Color backgroundColor;  //背景の色
  private Color rowGridColor;  //行グリッドの色
  private Color columnGridColor;  //列グリッドの色
  private Color tabColumnGridColor;  //タブ列グリッドの色
  private Color highlightAreaColor;  //ハイライトエリアの色
  private Color highlightCursorColor;  //ハイライトカーソルの色
  private Color underlineCursorColor;  //アンダーラインカーソルの色

  private Paint gridPaint;  //グリッドのテクスチャペイント
  private int highlightAreaStart;  //ハイライトエリアの開始位置
  private int highlightAreaEnd;  //ハイライトエリアの終了位置。-1=ハイライトエリアなし
  private Stroke underlineCursorStroke;  //アンダーラインカーソルのストローク

  private JTextArea textArea;

  private int fontWidth;
  private int fontHeight;
  private int marginTop;
  private int marginLeft;

  private DefaultCaret caret;

  //コンストラクタ
  //  super.setOpaque (false)で警告this-escapeが出るので@SuppressWarnings ("this-escape")を追加
  @SuppressWarnings ("this-escape") public ScrollTextArea () {
    super ();

    opaqueOn = true;
    gridOn = true;
    highlightCursorOn = false;
    underlineCursorOn = false;

    foregroundColor      = new Color (LnF.lnfRGB[14]);
    backgroundColor      = new Color (LnF.lnfRGB[0]);
    rowGridColor         = new Color (LnF.lnfRGB[2]);
    columnGridColor      = new Color (LnF.lnfRGB[1]);
    tabColumnGridColor   = new Color (LnF.lnfRGB[2]);
    highlightAreaColor   = new Color (LnF.lnfRGB[3]);
    highlightCursorColor = new Color (LnF.lnfRGB[6]);
    underlineCursorColor = new Color (LnF.lnfRGB[10]);

    gridPaint = backgroundColor;
    highlightAreaStart = highlightAreaEnd = -1;
    underlineCursorStroke = new BasicStroke (1.0F,
                                             BasicStroke.CAP_SQUARE,
                                             BasicStroke.JOIN_MITER,
                                             10.0F,
                                             new float[] { 0.0F, 2.0F },
                                             0.0F);

    textArea = new JTextArea () {
      //描画
      @Override public void paintComponent (Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth ();
        int height = getHeight ();
        //背景
        if (opaqueOn) {
          g2.setPaint (gridOn ? gridPaint : backgroundColor);
          g2.fillRect (0, 0, width, height);
        }
        paintAfterBackground (this, g2);
        //ハイライトエリア
        if (0 <= highlightAreaEnd) {
          try {
            //Rectangle startRect = modelToView (highlightAreaStart).getBounds ();  //modelToView2Dは9から
            Rectangle startRect = modelToView2D (highlightAreaStart).getBounds ();  //modelToView2Dは9から
            //Rectangle endRect = modelToView (highlightAreaEnd).getBounds ();  //modelToView2Dは9から
            Rectangle endRect = modelToView2D (highlightAreaEnd).getBounds ();  //modelToView2Dは9から
            Insets margin = getMargin ();
            g2.setPaint (highlightAreaColor);
            g2.fillRect (margin.left, startRect.y,
                         width - margin.left - margin.right, endRect.y - startRect.y + endRect.height);
          } catch (BadLocationException ble) {
          }
        }
        //ラインカーソル
        if (highlightCursorOn || underlineCursorOn) {
          try {
            //Rectangle caretRect = modelToView (getCaretPosition ()).getBounds ();  //modelToView2Dは9から
            Rectangle caretRect = modelToView2D (getCaretPosition ()).getBounds ();  //modelToView2Dは9から
            Insets margin = getMargin ();
            if (highlightCursorOn) {
              g2.setPaint (highlightCursorColor);
              g2.fillRect (margin.left, caretRect.y,
                           width - margin.left - margin.right, caretRect.height);
            }
            if (underlineCursorOn) {
              g2.setPaint (underlineCursorColor);
              g2.setStroke (underlineCursorStroke);
              g2.drawLine (margin.left, caretRect.y + caretRect.height - 1,
                           width - margin.right - 1, caretRect.y + caretRect.height - 1);
            }
          } catch (BadLocationException ble) {
          }
        }
        //テキスト
        super.paintComponent (g);
        paintAfterText (this, g2);
      }
    };
    textArea.setOpaque (false);  //背景色を塗らせない
    super.setOpaque (false);

    textArea.setForeground (foregroundColor);
    textArea.setSelectionColor (new Color (LnF.lnfRGB[7]));  //選択領域の背景の色
    //textArea.setSelectedTextColor (new Color (LnF.lnfRGB[14]));  //選択領域のテキストの色

    Font font = textArea.getFont ();
    fontWidth = font.getSize () + 1 >> 1;
    fontHeight = textArea.getFontMetrics (font).getHeight ();
    Insets margin = textArea.getMargin ();
    marginTop = margin.top;
    marginLeft = margin.left;

    createGrid ();

    caret = new DefaultCaret () {
      @Override protected void damage (Rectangle r) {
        if (r != null) {
          if (highlightCursorOn || underlineCursorOn) {
            x = 0;
            y = r.y;
            width = textArea.getWidth ();
            height = r.height;
            textArea.repaint ();
          } else {
            super.damage (r);
          }
        }
      }
    };
    caret.setBlinkRate (500);
    textArea.setCaret (caret);

    getViewport ().setView (textArea);
  }

  //paintAfterBackground (textArea, g2)
  //  背景を描画した後に呼び出される
  public void paintAfterBackground (JTextArea textArea, Graphics2D g2) {
  }

  //paintAfterText (textArea, g2)
  //  テキストを描画した後に呼び出される
  public void paintAfterText (JTextArea textArea, Graphics2D g2) {
  }

  //グリッドを作る
  //  フォントの右下にグリッドを表示する
  //  フォント(6,13),マージン(3,3,3,3)のとき
  //  ..t.....c.....c.....c.....c.....c.....c.....c...
  //  ................................................
  //  r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.r.
  //  ..t.....c.....c.....c.....c.....c.....c.....c...
  //  ................................................
  //  ..t.....c.....c.....c.....c.....c.....c.....c...
  //  ................................................
  //  ..t.....c.....c.....c.....c.....c.....c.....c...
  //  ................................................
  //  ..t.....c.....c.....c.....c.....c.....c.....c...
  //  ................................................
  //  ..t.....c.....c.....c.....c.....c.....c.....c...
  //  ................................................
  private void createGrid () {
    int backgroundRGB = backgroundColor.getRGB ();
    int rowGridRGB = rowGridColor.getRGB ();
    int columnGridRGB = columnGridColor.getRGB ();
    int tabColumnGridRGB = tabColumnGridColor.getRGB ();
    int imageWidth = fontWidth * 8;
    BufferedImage image = new BufferedImage (imageWidth, fontHeight, BufferedImage.TYPE_INT_RGB);
    int[] bitmap = ((DataBufferInt) image.getRaster ().getDataBuffer ()).getData ();
    for (int y = 0; y < fontHeight; y++) {
      for (int x = 0; x < imageWidth; x++) {
        bitmap[(marginLeft + x) % imageWidth + imageWidth * ((marginTop + y) % fontHeight)] =
          (((y ^ fontHeight) & 1) == 0 ? backgroundRGB :
           y == fontHeight - 1 ? ((x % fontWidth ^ fontWidth) & 1) != 0 ? rowGridRGB : backgroundRGB :
           x == imageWidth - 1 ? tabColumnGridRGB : x % fontWidth == fontWidth - 1 ? columnGridRGB : backgroundRGB);
      }
    }
    gridPaint = new TexturePaint (image, new Rectangle (0, 0, imageWidth, fontHeight));
  }

  //サイズ
  @Override public void setMaximumSize (Dimension size) {
    super.setMaximumSize (size);
    getViewport ().setMaximumSize (size);
  }
  @Override public void setMinimumSize (Dimension size) {
    super.setMinimumSize (size);
    getViewport ().setMinimumSize (size);
  }
  @Override public void setPreferredSize (Dimension size) {
    super.setPreferredSize (size);
    getViewport ().setPreferredSize (size);
  }

  //マージン
  public Insets getMargin () {
    return textArea.getMargin ();
  }
  public void setMargin (Insets m) {
    textArea.setMargin (m);
    int t = m.top;
    int l = m.left;
    if (marginTop != t || marginLeft != l) {
      marginTop = t;
      marginLeft = l;
      createGrid ();
    }
  }

  //背景
  @Override public boolean isOpaque () {
    return opaqueOn;
  }
  @Override public void setOpaque (boolean opaque) {
    opaqueOn = opaque;
  }

  //色
  @Override public Color getForeground () {
    return foregroundColor;
  }
  @Override public void setForeground (Color color) {
    foregroundColor = color;
    if (textArea != null) {  //スーパークラスのコンストラクタからの呼び出しではない
      textArea.setForeground (color);
    }
  }
  @Override public Color getBackground () {
    return backgroundColor;
  }
  @Override public void setBackground (Color color) {
    backgroundColor = color;
    if (textArea != null) {  //スーパークラスのコンストラクタからの呼び出しではない
      createGrid ();
    }
  }

  //フォント
  @Override public Font getFont () {
    if (textArea != null) {  //スーパークラスのコンストラクタからの呼び出しではない
      return textArea.getFont ();
    }
    return super.getFont ();
  }
  @Override public void setFont (Font font) {
    if (textArea != null) {  //スーパークラスのコンストラクタからの呼び出しではない
      textArea.setFont (font);
      int w = font.getSize () + 1 >> 1;
      int h = textArea.getFontMetrics (font).getHeight ();
      if (fontWidth != w || fontHeight != h) {
        fontWidth = w;
        fontHeight = h;
        createGrid ();
      }
    }
  }

  //イベント
  public void addCaretListener (CaretListener listener) {
    textArea.addCaretListener (listener);
  }
  public void removeCaretListener (CaretListener listener) {
    textArea.removeCaretListener (listener);
  }
  public void addDocumentListener (DocumentListener listener) {
    textArea.getDocument ().addDocumentListener (listener);
  }
  public void removeDocumentListener (DocumentListener listener) {
    textArea.getDocument ().removeDocumentListener (listener);
  }
  @Override public void addKeyListener (KeyListener listener) {
    textArea.addKeyListener (listener);
  }
  @Override public void removeKeyListener (KeyListener listener) {
    textArea.removeKeyListener (listener);
  }
  @Override public void addMouseListener (MouseListener listener) {
    textArea.addMouseListener (listener);
  }
  @Override public void removeMouseListener (MouseListener listener) {
    textArea.removeMouseListener (listener);
  }
  @Override public void addFocusListener (FocusListener listener) {
    textArea.addFocusListener (listener);
  }
  @Override public void removeFocusListener (FocusListener listener) {
    textArea.removeFocusListener (listener);
  }

  //折り返し
  public boolean getLineWrap () {
    return textArea.getLineWrap ();
  }
  public void setLineWrap (boolean wrap) {
    textArea.setLineWrap (wrap);
  }

  //キャレット
  public Color getCaretColor () {
    return textArea.getCaretColor ();
  }
  public void setCaretColor (Color color) {
    textArea.setCaretColor (color);
  }
  public int getCaretPosition () {
    return textArea.getCaretPosition ();
  }
  public void setCaretPosition (int pos) {
    textArea.setCaretPosition (pos);
  }
  public boolean isCaretVisible () {
    return caret.isVisible ();
  }
  public void setCaretVisible (boolean visible) {
    caret.setVisible (visible);
  }

  //テキスト
  public JTextArea getTextArea () {
    return textArea;
  }
  public String getText () {
    return textArea.getText ();
  }
  public void setText (String text) {
    highlightAreaEnd = -1;
    textArea.setText (text);
  }

  //編集
  public void append (String text) {
    highlightAreaEnd = -1;
    textArea.append (text);
  }
  public void insert (String text, int pos) {
    highlightAreaEnd = -1;
    textArea.insert (text, pos);
  }
  public void replaceRange (String text, int start, int end) {
    highlightAreaEnd = -1;
    textArea.replaceRange (text, start, end);
  }

  //選択
  public void selectAll () {
    textArea.selectAll ();
  }
  public String getSelectedText () {
    return textArea.getSelectedText ();
  }
  public int getSelectionStart () {
    return textArea.getSelectionStart ();
  }
  public int getSelectionEnd () {
    return textArea.getSelectionEnd ();
  }

  //モード
  public boolean isEditable () {
    return textArea.isEditable ();
  }
  public void setEditable (boolean editable) {
    textArea.setEditable (editable);
  }

  //グリッド
  public boolean isGridOn () {
    return gridOn;
  }
  public void setGridOn (boolean on) {
    gridOn = on;
  }
  public Color getRowGridColor () {
    return rowGridColor;
  }
  public void setRowGridColor (Color color) {
    rowGridColor = color;
    createGrid ();
  }
  public Color getColumnGridColor () {
    return columnGridColor;
  }
  public void setColumnGridColor (Color color) {
    columnGridColor = color;
    createGrid ();
  }
  public Color getTabColumnGridColor () {
    return tabColumnGridColor;
  }
  public void setTabColumnGridColor (Color color) {
    tabColumnGridColor = color;
    createGrid ();
  }

  //ハイライトエリア
  public void setHighlightArea (int start, int end) {
    highlightAreaStart = start;
    highlightAreaEnd = end;
  }

  //ハイライトカーソル
  public boolean isHighlightCursorOn () {
    return highlightCursorOn;
  }
  public void setHighlightCursorOn (boolean on) {
    highlightCursorOn = on;
  }
  public Color getHighlightCursorColor () {
    return highlightCursorColor;
  }
  public void setHighlightCursorColor (Color color) {
    highlightCursorColor = color;
  }

  //アンダーラインカーソル
  public boolean isUnderlineCursorOn () {
    return underlineCursorOn;
  }
  public void setUnderlineCursorOn (boolean on) {
    underlineCursorOn = on;
  }
  public Color getUnderlineCursorColor () {
    return underlineCursorColor;
  }
  public void setUnderlineCursorColor (Color color) {
    underlineCursorColor = color;
  }

}  //class ScrollTextArea



