//========================================================================================
//  DrawingCanvas.java
//    en:Drawing canvas
//    ja:ドローイングキャンバス
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  ScrollCanvasに任意のShapeを任意のStrokeとPaintで描画する
//  ShapeはスケーリングされるがStrokeの太さやPaintのグラデーションやテクスチャの大きさは変化しない
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class DrawingCanvas extends ScrollCanvas {

  //インスタンス変数
  protected AffineTransform transformToView;  //キャンバス座標からビュー座標への変換
  protected HashMap<Integer,Shape> shapes;  //Shapeのマップ。null=描画しない
  protected HashMap<Integer,GeneralPath> scaledShapes;  //スケーリングされたShapeのマップ
  protected HashMap<Integer,Stroke> strokes;  //Strokeのマップ
  protected HashMap<Integer,Paint> drawPaints;  //draw用のPaintのマップ。null=drawしない
  protected HashMap<Integer,Paint> fillPaints;  //fill用のPaintのマップ。null=fillしない

  //コンストラクタ
  public DrawingCanvas () {
    init ();
  }
  public DrawingCanvas (int width, int height) {
    super (width, height);
    init ();
  }
  public DrawingCanvas (BufferedImage image) {
    super (image);
    init ();
  }
  private void init () {
    shapes = new HashMap<Integer,Shape> ();
    scaledShapes = new HashMap<Integer,GeneralPath> ();
    strokes = new HashMap<Integer,Stroke> ();
    drawPaints = new HashMap<Integer,Paint> ();
    fillPaints = new HashMap<Integer,Paint> ();
  }

  //ビューを描画する
  protected void paintView (Graphics g) {
    super.paintView (g);
    if (canvasImage != null) {
      Graphics2D g2 = (Graphics2D) g;
      Integer[] keyArray = shapes.keySet ().toArray (new Integer[0]);
      Arrays.sort (keyArray);
      for (int i : keyArray) {
        Shape scaledShape = scaledShapes.get (i);
        if (scaledShape != null) {
          Stroke stroke = strokes.get (i);
          if (stroke != null) {
            g2.setStroke (stroke);
          }
          Paint fillPaint = fillPaints.get (i);
          if (fillPaint != null) {
            g2.setPaint (fillPaint);
            g2.fill (scaledShape);
          }
          Paint drawPaint = drawPaints.get (i);
          if (drawPaint != null) {
            g2.setPaint (drawPaint);
            g2.draw (scaledShape);
          }
        }
      }
    }
  }

  //追加のサイズ計算
  protected void calcAdditionalSize () {
    transformToView = new AffineTransform (scaleFactor, 0.0F, 0.0F, scaleFactor,
                                           (float) originX, (float) originY);
    for (int i : shapes.keySet ()) {
      calcScaledShape (i);
    }
  }

  //Shapeを設定する
  public void setShape (int i, Shape shape, Stroke stroke, Paint drawPaint, Paint fillPaint) {
    shapes.put (i, shape);
    scaledShapes.put (i, new GeneralPath ());
    calcScaledShape (i);
    strokes.put (i, stroke);
    drawPaints.put (i, drawPaint);
    fillPaints.put (i, fillPaint);
    view.repaint ();
  }

  //Shapeをスケーリングする
  protected void calcScaledShape (int i) {
    Shape shape = shapes.get (i);
    if (shape != null) {
      GeneralPath scaledShape = scaledShapes.get (i);
      if (scaledShape != null) {
        scaledShape.reset ();
        float[] coords = new float[6];
        for (PathIterator iterator = shape.getPathIterator (transformToView);
             !iterator.isDone (); iterator.next ()) {
          switch (iterator.currentSegment (coords)) {
          case PathIterator.SEG_CLOSE:
            scaledShape.closePath ();
            break;
          case PathIterator.SEG_CUBICTO:
            scaledShape.curveTo (coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
            break;
          case PathIterator.SEG_LINETO:
            scaledShape.lineTo (coords[0], coords[1]);
            break;
          case PathIterator.SEG_MOVETO:
            scaledShape.moveTo (coords[0], coords[1]);
            break;
          case PathIterator.SEG_QUADTO:
            scaledShape.quadTo (coords[0], coords[1], coords[2], coords[3]);
            break;
          }  //switch
        }  //iterator
      }  //scaledShape!= null
    }  //shape!=null
  }  //calcScaledShape

}  //class DrawingCanvas



