//========================================================================================
//  ScrollCanvas.java
//    en:Scroll canvas
//    ja:スクロールキャンバス
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  BufferedImageで与えられた画像をスケーリングしてスクロールバーを付けて表示する
//  スケーリングされたキャンバスとマージンを合わせたサイズがビューポートよりも小さいとき
//    ビューポートのサイズがビューと一致する
//    スケーリングされたキャンバスの全体がビューポートの中央に表示される
//                   origin(>margin)
//                    ┌──┴──┐view(=viewport)
//                  ┌┌─────────┴─────────┐
//                  ││                                      │
//  origin(>margin) ┤│                                      │
//                  ││                scaled                │
//                  └│          ┏━━━┷━━━┓          │
//                    │          ┃              ┃          │
//                    │          ┃              ┃          │
//                    │          ┃              ┃          │
//                    │          ┃              ┃          │
//                    │          ┗━━━━━━━┛          │
//                    │                                      │
//                    │                                      │
//                    │                                      │
//                    └───────────────────┘
//  スケーリングされたキャンバスとマージンを合わせたサイズがビューポートよりも大きいとき
//    スケーリングされたキャンバスとマージンを合わせたサイズがビューと一致する
//    スケーリングされたキャンバスの一部分がビューポートの全体に表示される
//               origin(=margin)
//                    ┌┴┐     view(=margin*2+scaled)
//                  ┌┌─────────┴─────────┐
//  origin(=margin) ┤│                scaled                │
//                  └│  ┏━━━━━━━┷━━━━━━━┓  │
//                    │  ┃                              ┃  │
//                    │  ┃                              ┃  │
//                    │  ┃   viewport                   ┃  │
//                    │┌╂───┴────┐            ┃  │
//                    ││┃                │            ┃  │
//                    ││┃                │            ┃  │
//                    ││┃                │            ┃  │
//                    ││┗━━━━━━━━┿━━━━━━┛  │
//                    │└─────────┘                │
//                    └───────────────────┘
//  縦と横の条件が独立していることに注意
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class ScrollCanvas extends JScrollPane implements MouseListener, MouseMotionListener, MouseWheelListener {

  //スケールシフトの範囲
  protected int minScaleShift = -4;
  protected int maxScaleShift = 4;

  //キャンバス
  protected BufferedImage canvasImage;  //キャンバスのイメージ。キャンバスのサイズの方が小さいとき左上を使う
  protected int canvasWidth;  //キャンバスのサイズ。イメージのサイズと等しいか小さい
  protected int canvasHeight;

  //ビューポート
  protected int viewportWidth;  //ビューポートのサイズ
  protected int viewportHeight;
  protected int marginX;  //マージン
  protected int marginY;
  protected int scaleShift;  //スケーリングのシフトカウント(0=等倍,正=拡大,負=縮小)
  protected float scaleFactor;  //スケーリングの係数(pow(2,scaleShift);1=等倍,1より大きい=拡大,1より小さい=縮小)
  protected int direction;  //方向。0=0deg,1=90deg,2=180deg,3=270deg
  protected int scaledWidth;  //スケーリングされたキャンバスのサイズ
  protected int scaledHeight;
  protected int rotatedScaledWidth;  //スケーリングおよび回転させたキャンバスのサイズ
  protected int rotatedScaledHeight;
  protected int viewWidth;  //ビューのサイズ
  protected int viewHeight;
  protected int originX;  //スケーリングされたキャンバスのビュー座標
  protected int originY;

  //マウス
  protected ArrayList<MouseListener> mouseListeners;  //マウスリスナー
  protected ArrayList<MouseMotionListener> mouseMotionListeners;  //マウスモーションリスナー
  protected ArrayList<MouseWheelListener> mouseWheelListeners;  //マウスホイールリスナー
  protected boolean dragStarted;  //ドラッグ中か
  protected int pressedX;  //ドラッグ開始時のマウスのビュー座標
  protected int pressedY;

  //スケールシフトリスナー
  protected ArrayList<ScaleShiftListener> scaleShiftListeners;  //スケールシフトリスナー

  //ビュー
  protected JPanel view;  //ビュー

  //new ScrollCanvas ()
  //new ScrollCanvas (width, height)
  //new ScrollCanvas (image)
  //new ScrollCanvas (image, width, height)
  //  コンストラクタ
  public ScrollCanvas () {
    this (480, 360);
  }
  public ScrollCanvas (int width, int height) {
    this (new BufferedImage (width, height, BufferedImage.TYPE_INT_ARGB));
  }
  public ScrollCanvas (BufferedImage image) {
    this (image,
          image == null ? 1 : image.getWidth (),
          image == null ? 1 : image.getHeight ());
  }
  @SuppressWarnings ("this-escape") public ScrollCanvas (BufferedImage image, int width, int height) {
    //マウス
    mouseListeners = new ArrayList<MouseListener> ();  //マウスリスナー
    mouseMotionListeners = new ArrayList<MouseMotionListener> ();  //マウスモーションリスナー
    mouseWheelListeners = new ArrayList<MouseWheelListener> ();  //マウスホイールリスナー
    dragStarted = false;  //ドラッグ中か
    pressedX = 0;  //ドラッグ開始時のマウスのビュー座標
    pressedY = 0;
    //スケールシフトリスナー
    scaleShiftListeners = new ArrayList<ScaleShiftListener> ();  //スケールシフトリスナー
    //キャンバス
    canvasImage = image;
    canvasWidth = width;
    canvasHeight = height;
    //ビューポート
    viewportWidth = canvasWidth;  //ビューポートのサイズ
    viewportHeight = canvasHeight;
    marginX = 0;  //マージン
    marginY = 0;
    scaleShift = 0;  //スケーリングのシフトカウント(0=等倍,正=拡大,負=縮小)
    scaleFactor = 1.0F;  //スケーリングの係数(pow(2,scaleShift);1=等倍,1より大きい=拡大,1より小さい=縮小)
    direction = 0; //方向。0=0deg,1=90deg,2=180deg,3=270deg
    calcScaledSize ();  //スケーリングされたキャンバスのサイズを計算する
    calcViewSize ();  //ビューのサイズを計算する
    //ビュー
    view = new JPanel () {
      public void paintComponent (Graphics g) {
        super.paintComponent (g);
        paintView (g);
      }
    };
    view.setOpaque (true);  //不透明
    view.setBackground (Color.lightGray);  //背景色(明るいグレー)
    view.setPreferredSize (new Dimension (viewWidth, viewHeight));  //サイズ
    view.addMouseListener (this);  //[this-escape]マウスイベント
    view.addMouseMotionListener (this);  //マウスモーションイベント
    view.addMouseWheelListener (this);  //マウスホイールイベント
    //ビューポート
    viewport.setScrollMode (JViewport.BLIT_SCROLL_MODE);
    viewport.setPreferredSize (new Dimension (viewportWidth, viewportHeight));
    viewport.setMinimumSize (new Dimension (64, 64));
    viewport.setView (view);
    viewport.addChangeListener (new ChangeListener () {
      public void stateChanged (ChangeEvent e) {
        int width = viewport.getWidth ();  //新しいビューポートのサイズ
        int height = viewport.getHeight ();
        if (viewportWidth != width || viewportHeight != height) {  //ビューポートのサイズが変化した
          Point2D p = getCenterPoint ();
          viewportWidth = width;
          viewportHeight = height;
          calcViewSize ();  //ビューのサイズを計算する
          calcAdditionalSize ();  //追加のサイズ計算
          view.setPreferredSize (new Dimension (viewWidth, viewHeight));  //ビューのサイズを更新する
          setCenterPoint (p);
        }
      }
    });
    setWheelScrollingEnabled (false);  //デフォルトのホイールスクロールを禁止する
    setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);  //スクロールバーを常に表示する。イメージがスクロールバーのサイズよりも小さいときスクロールバーが点滅してしまわないようにする
    setVerticalScrollBarPolicy (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
  }

  //scrollCanvas.paintView (g)
  //  ビューを描画する
  //  スケーリングしたキャンバスは巨大化することがあるので保持しない
  protected void paintView (Graphics g) {
    if (canvasImage != null) {
      Graphics2D g2 = (Graphics2D) g;
      AffineTransform saveAT = g2.getTransform ();
      g2.translate ((double) originX, (double) originY);
      if (direction == 0) {  //0deg
      } else if (direction == 1) {  //90deg
        g2.translate ((double) rotatedScaledWidth, 0.0);
        g2.rotate (Math.PI / 2.0);
      } else if (direction == 2) {  //180deg
        g2.translate ((double) rotatedScaledWidth, (double) rotatedScaledHeight);
        g2.rotate (Math.PI);
      } else {  //270deg
        g2.translate (0.0, (double) rotatedScaledHeight);
        g2.rotate (Math.PI * 3.0 / 2.0);
      }
      g2.translate ((double) (-originX), (double) (-originY));
      //
      if (scaleShift == 0) {  //スケーリングなし
        g2.drawImage (canvasImage,
                      originX, originY, originX + canvasWidth, originY + canvasHeight,  //dst
                      0, 0, canvasWidth, canvasHeight,  //src
                      null);
      } else {  //スケーリングあり
        //g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);  //BICUBICにしても大差ない
        g2.drawImage (canvasImage,
                      originX, originY, originX + scaledWidth, originY + scaledHeight,  //dst
                      0, 0, canvasWidth, canvasHeight,  //src
                      null);
      }
      //
      g2.setTransform (saveAT);
    }
  }

  //マウスリスナーの操作
  @Override public void addMouseListener (MouseListener ml) {
    if (ml != null && !mouseListeners.contains (ml)) {
      mouseListeners.add (ml);
    }
  }
  @Override public void removeMouseListener (MouseListener ml) {
    mouseListeners.remove (ml);
  }
  @Override public MouseListener[] getMouseListeners () {
    return mouseListeners.toArray (new MouseListener[mouseListeners.size ()]);
  }
  //マウスモーションリスナーの操作
  @Override public void addMouseMotionListener (MouseMotionListener mml) {
    if (mml != null && !mouseMotionListeners.contains (mml)) {
      mouseMotionListeners.add (mml);
    }
  }
  @Override public void removeMouseMotionListener (MouseMotionListener mml) {
    mouseMotionListeners.remove (mml);
  }
  @Override public MouseMotionListener[] getMouseMotionListeners () {
    return mouseMotionListeners.toArray (new MouseMotionListener[mouseMotionListeners.size ()]);
  }
  //マウスホイールリスナーの操作
  @Override public void addMouseWheelListener (MouseWheelListener mml) {
    if (mouseWheelListeners != null) {  //スーパークラスのコンストラクタから呼ばれたとき初期化されていない
      if (mml != null && !mouseWheelListeners.contains (mml)) {
        mouseWheelListeners.add (mml);
      }
    }
  }
  @Override public void removeMouseWheelListener (MouseWheelListener mml) {
    mouseWheelListeners.remove (mml);
  }
  @Override public MouseWheelListener[] getMouseWheelListeners () {
    return mouseWheelListeners.toArray (new MouseWheelListener[mouseWheelListeners.size ()]);
  }

  //マウスイベントの処理
  //  マウスイベントの座標をビュー座標からキャンバス座標に変換してからリスナーに配布する
  //  Javaのイベントモデルのルールにより、複数のリスナーが登録されているとき、
  //  どのリスナーがイベントを消費してもすべてのリスナーにイベントが配布されなければならない
  //  どのリスナーもイベントを消費しなかったときだけスクロールキャンバスはドラッグを処理する
  @Override public void mouseClicked (MouseEvent me) {
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseListener ml : mouseListeners) {
      ml.mouseClicked (me2D);
    }
    if (!me2D.isConsumed ()) {
      if (isFocusable () && !isFocusOwner ()) {
        requestFocus ();
      }
    }
  }
  @Override public void mouseEntered (MouseEvent me) {
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseListener ml : mouseListeners) {
      ml.mouseEntered (me2D);
    }
  }
  @Override public void mouseExited (MouseEvent me) {
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseListener ml : mouseListeners) {
      ml.mouseExited (me2D);
    }
  }
  @Override public void mousePressed (MouseEvent me) {
    int x = me.getX ();
    int y = me.getY ();
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseListener ml : mouseListeners) {
      ml.mousePressed (me2D);
    }
    if (!me2D.isConsumed ()) {
      dragStarted = true;
      pressedX = x;
      pressedY = y;
    }
  }
  @Override public void mouseReleased (MouseEvent me) {
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseListener ml : mouseListeners) {
      ml.mouseReleased (me2D);
    }
    if (!me2D.isConsumed ()) {
      dragStarted = false;
    }
  }
  //マウスモーションイベントの処理
  //  マウスモーションイベントの座標をビュー座標からキャンバス座標に変換してからリスナーに配布する
  //  Javaのイベントモデルのルールにより、複数のリスナーが登録されているとき、
  //  どのリスナーがイベントを消費してもすべてのリスナーにイベントが配布されなければならない
  //  どのリスナーもイベントを消費しなかったときだけスクロールキャンバスはドラッグを処理する
  @Override public void mouseDragged (MouseEvent me) {
    int x = me.getX ();
    int y = me.getY ();
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseMotionListener ml : mouseMotionListeners) {
      ml.mouseDragged (me2D);
    }
    if (!me2D.isConsumed ()) {
      if (dragStarted) {
        Point p = viewport.getViewPosition ();
        updateViewPosition (p.x - (x - pressedX), p.y - (y - pressedY));
      }
    }
  }
  @Override public void mouseMoved (MouseEvent me) {
    MouseEvent2D me2D = adjustMouseEvent (me);
    for (MouseMotionListener ml : mouseMotionListeners) {
      ml.mouseMoved (me2D);
    }
  }
  //マウスホイールイベントの処理
  //  マウスホイールイベントの座標をビュー座標からキャンバス座標に変換してからリスナーに配布する
  //  Javaのイベントモデルのルールにより、複数のリスナーが登録されているとき、
  //  どのリスナーがイベントを消費してもすべてのリスナーにイベントが配布されなければならない
  @Override public void mouseWheelMoved (MouseWheelEvent mwe) {
    MouseWheelEvent2D mwe2D = adjustMouseWheelEvent (mwe);
    for (MouseWheelListener mwl : mouseWheelListeners) {
      mwl.mouseWheelMoved (mwe2D);
    }
    if (!mwe2D.isConsumed ()) {
      int n = mwe2D.getWheelRotation ();
      if (n < 0) {
        setScaleShift (scaleShift + 1, mwe2D.getPoint2D ());
      } else if (n > 0) {
        setScaleShift (scaleShift - 1, mwe2D.getPoint2D ());
      }
    }
  }

  //width = scrollCanvas.getCanvasWidth ()
  //  キャンバスの幅を取得する
  public int getCanvasWidth () {
    return canvasWidth;
  }

  //height = scrollCanvas.getCanvasHeight ()
  //  キャンバスの高さを取得する
  public int getCanvasHeight () {
    return canvasHeight;
  }

  //image = scrollCanvas.getImage (image)
  //  キャンバスのイメージを取得する
  public BufferedImage getImage () {
    return canvasImage;
  }

  //scrollCanvas.setImage (image)
  //scrollCanvas.setImage (width, height)
  //scrollCanvas.setImage (image, width, height)
  //  キャンバスのイメージを設定する
  public void setImage (BufferedImage image) {
    setImage (image,
              image == null ? 1 : image.getWidth (),
              image == null ? 1 : image.getHeight ());
  }
  public void setImage (int width, int height) {
    setImage (canvasImage, width, height);
  }
  public void setImage (BufferedImage image, int width, int height) {
    if (width != canvasWidth || height != canvasHeight) {  //キャンバスのサイズが変わった
      canvasImage = image;
      canvasWidth = width;
      canvasHeight = height;
      updateView ();  //ビューを更新する
    } else if (canvasImage != image) {  //サイズは変わらずイメージが変わった
      canvasImage = image;
      view.repaint ();
    }
  }

  //marginX = scrollCanvas.getMarginX ()
  //  左右のマージンを取得する
  public int getMarginX () {
    return marginX;
  }

  //marginY = scrollCanvas.getMarginY ()
  //  上下のマージンを取得する
  public int getMarginY () {
    return marginY;
  }

  //scrollCanvas.setMargin (x, y)
  //  マージンを設定する
  public void setMargin (int x, int y) {
    if (marginX != x || marginY != y) {
      marginX = x;
      marginY = y;
      updateView ();  //ビューを更新する
    }
  }

  //color = scrollCanvas.getMatColor ()
  //  マットの色を取得する
  public Color getMatColor () {
    return view.getBackground ();
  }

  //scrollCanvas.setMatColor (color)
  //  マットの色を設定する
  public void setMatColor (Color color) {
    view.setBackground (color);
    view.repaint ();
  }

  public int getMinScaleShift () {
    return minScaleShift;
  }
  public void setMinScaleShift (int shift) {
    minScaleShift = shift;
    if (scaleShift < minScaleShift) {
      setScaleShift (minScaleShift);
    }
  }

  public int getMaxScaleShift () {
    return maxScaleShift;
  }
  public void setMaxScaleShift (int shift) {
    maxScaleShift = shift;
    if (maxScaleShift < scaleShift) {
      setScaleShift (maxScaleShift);
    }
  }

  //scaleShift = scrollCanvas.getScaleShift ()
  //  スケーリングのシフトカウントを取得する
  public int getScaleShift () {
    return scaleShift;
  }

  //scrollCanvas.setScaleShift (shift)
  //  スケーリングのシフトカウントを設定する
  public void setScaleShift (int shift) {
    setScaleShift (shift, getCenterPoint ());
  }
  public void setScaleShift (int shift, Point2D p) {
    shift = Math.max (minScaleShift, Math.min (maxScaleShift, shift));
    if (scaleShift != shift) {  //スケーリングのシフトカウントが変わった
      Point2D c = getCenterPoint ();
      double dx = (c.getX () - p.getX ()) * scaleFactor;  //pからcまでのピクセル数
      double dy = (c.getY () - p.getY ()) * scaleFactor;
      scaleShift = shift;
      scaleFactor = shift >= 0 ? (float) (1 << shift) : 1.0F / (float) (1 << -shift);  //スケーリングの係数
      updateView ();  //ビューを更新する
      setCenterPoint (new Point2D.Double (p.getX () + dx / scaleFactor, p.getY () + dy / scaleFactor));
      for (ScaleShiftListener listener : scaleShiftListeners) {
        listener.scaleShiftChanged (scaleShift);
      }
    }
  }

  //スケールシフトリスナー
  public static class ScaleShiftListener {
    public void scaleShiftChanged (int scaleShift) {
    }
  }
  public void addScaleShiftListener (ScaleShiftListener listener) {
    if (listener != null) {
      scaleShiftListeners.add (listener);
    }
  }
  public void removeScaleShiftListener (ScaleShiftListener listener) {
    scaleShiftListeners.remove (listener);
  }
  public ScaleShiftListener[] getScaleShiftListeners () {
    return scaleShiftListeners.toArray (new ScaleShiftListener[scaleShiftListeners.size ()]);
  }

  //direction = scrollCanvas.getDirection ()
  //  方向を取得する
  public int getDirection () {
    return direction;
  }

  //scrollCanvas.setDirection (direction)
  //  方向を設定する
  public void setDirection (int direction) {
    this.direction = direction;
    updateView ();
  }

  //p = scrollCanvas.getCenterPoint ()
  //  ビューポートの中央のイメージ座標を取得する
  public Point2D getCenterPoint () {
    Point p = viewport.getViewPosition ();
    return new Point2D.Float ((p.x + (viewportWidth >> 1) - originX) / scaleFactor,
                              (p.y + (viewportHeight >> 1) - originY) / scaleFactor);
  }

  //scrollCanvas.setCenterPoint (p)
  //  ビューポートの中央のイメージ座標を設定する
  public void setCenterPoint (Point2D p) {
    updateViewPosition ((int) (p.getX () * scaleFactor) + originX - (viewportWidth >> 1),
                        (int) (p.getY () * scaleFactor) + originY - (viewportHeight >> 1));
  }

  //me = scrollCanvas.adjustMouseEvent (me)
  //me = scrollCanvas.adjustMouseWheelEvent (me)
  //  マウスイベントを調節する
  //  ソースをビューではなくキャンバス自身にする
  //  座標をビュー座標ではなくイメージの座標にする
  //  スケーリングされたキャンバスがビューポートよりも小さいときキャンバスの範囲外の座標が設定されることがある
  protected MouseEvent2D adjustMouseEvent (MouseEvent me) {
    return new MouseEvent2D (this, me.getID (), me.getWhen (), me.getModifiersEx (),
                             (float) (me.getX () - originX) / scaleFactor,
                             (float) (me.getY () - originY) / scaleFactor,
                             me.getClickCount (), me.isPopupTrigger (),
                             me.getButton ());
  }
  protected MouseWheelEvent2D adjustMouseWheelEvent (MouseWheelEvent mwe) {
    return new MouseWheelEvent2D (this, mwe.getID (), mwe.getWhen (), mwe.getModifiersEx (),
                                  (float) (mwe.getX () - originX) / scaleFactor,
                                  (float) (mwe.getY () - originY) / scaleFactor,
                                  mwe.getClickCount (), mwe.isPopupTrigger (),
                                  mwe.getScrollType (), mwe.getScrollAmount (),
                                  mwe.getWheelRotation ());
  }

  //p = getPopupPoint (me2D)
  //p = getPopupPoint (mwe2D)
  //  マウスイベントまたはマウスホイールイベントからポップアップ座標を求める
  public Point getPopupPoint (MouseEvent2D me2D) {
    Point p = viewport.getViewPosition ();
    return new Point ((int) (me2D.getX2D () * scaleFactor) + originX - p.x,
                      (int) (me2D.getY2D () * scaleFactor) + originY - p.y);
  }
  public Point getPopupPoint (MouseWheelEvent2D mwe2D) {
    Point p = viewport.getViewPosition ();
    return new Point ((int) (mwe2D.getX2D () * scaleFactor) + originX - p.x,
                      (int) (mwe2D.getY2D () * scaleFactor) + originY - p.y);
  }

  //scrollCanvas.updateView ()
  //  ビューを更新する
  protected void updateView () {
    calcScaledSize ();  //スケーリングされたキャンバスのサイズを計算する
    calcViewSize ();  //ビューのサイズを計算する
    calcAdditionalSize ();  //追加のサイズ計算
    Dimension d = new Dimension (viewWidth, viewHeight);
    view.setPreferredSize (d);  //ビューのサイズを更新する
    //view.revalidate()ではなくviewport.setViewSize()を使わないと
    //viewport.setViewPosition()がy方向のスクロール位置の更新に失敗する
    viewport.setViewSize (d);
    view.repaint ();  //ビューがビューポートに収まっている状態からさらに小さくなるとき必要
  }

  //scrollCanvas.calcScaledSize ()
  //  スケーリングされたキャンバスのサイズを計算する
  protected final void calcScaledSize () {
    if (scaleShift >= 0) {
      scaledWidth = canvasWidth << scaleShift;
      scaledHeight = canvasHeight << scaleShift;
    } else {
      scaledWidth = canvasWidth >> -scaleShift;
      scaledHeight = canvasHeight >> -scaleShift;
      if (scaledWidth < 1) {
        scaledWidth = 1;
      }
      if (scaledHeight < 1) {
        scaledHeight = 1;
      }
    }
    if ((direction & 1) == 0) {  //0deg,180deg
      rotatedScaledWidth = scaledWidth;
      rotatedScaledHeight = scaledHeight;
    } else {  //90deg,270deg
      rotatedScaledWidth = scaledHeight;
      rotatedScaledHeight = scaledWidth;
    }
  }

  //scrollCanvas.calcViewSize ()
  //  ビューのサイズを計算する
  protected final void calcViewSize () {
    if (viewportWidth < (marginX << 1) + rotatedScaledWidth) {
      viewWidth = (marginX << 1) + rotatedScaledWidth;
      originX = marginX;
    } else {
      viewWidth = viewportWidth;
      originX = (viewportWidth - rotatedScaledWidth) >> 1;
    }
    if (viewportHeight < (marginY << 1) + rotatedScaledHeight) {
      viewHeight = (marginY << 1) + rotatedScaledHeight;
      originY = marginY;
    } else {
      viewHeight = viewportHeight;
      originY = (viewportHeight - rotatedScaledHeight) >> 1;
    }
  }

  //scrollCanvas.calcAdditionalSize ()
  //  追加のサイズ計算
  protected void calcAdditionalSize () {
  }

  //scrollCanvas.updateViewPosition (x, y)
  //  ビューポートの位置を更新する
  protected void updateViewPosition (int x, int y) {
    if (originX > marginX || x < 0) {
      x = 0;
    } else if (x > (marginX << 1) + rotatedScaledWidth - viewportWidth) {
      x = (marginX << 1) + rotatedScaledWidth - viewportWidth;
    }
    if (originY > marginY || y < 0) {
      y = 0;
    } else if (y > (marginY << 1) + rotatedScaledHeight - viewportHeight) {
      y = (marginY << 1) + rotatedScaledHeight - viewportHeight;
    }
    viewport.setViewPosition (new Point (x, y));
  }

  //scrollCanvas.repaint ()
  //  キャンバスを再描画する
  @Override public void repaint () {
    super.repaint ();
    if (view != null) {
      view.repaint ();
    }
  }

}  //class ScrollCanvas



