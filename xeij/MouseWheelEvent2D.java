//========================================================================================
//  MouseWheelEvent2D.java
//    en:MouseWheelEvent2D -- It is a MouseWheelEvent that has floating-point coordinates.
//    ja:MouseWheelEvent2D -- 浮動小数点座標を持つMouseWheelEventです。
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.geom.*;  //AffineTransform,GeneralPath,Point2D,Rectangle2D
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class MouseWheelEvent2D extends MouseWheelEvent {
  protected float x2D;
  protected float y2D;
  public MouseWheelEvent2D (Component source, int id, long when, int modifiers,
                            float x2D, float y2D,
                            int clickCount, boolean popupTrigger,
                            int scrollType, int scrollAmount, int wheelRotation) {
    super (source, id, when, modifiers,
           (int) Math.floor ((double) x2D), (int) Math.floor ((double) y2D),
           clickCount, popupTrigger,
           scrollType, scrollAmount, wheelRotation);
    this.x2D = x2D;
    this.y2D = y2D;
  }
  public float getX2D () {
    return x2D;
  }
  public float getY2D () {
    return y2D;
  }
  public Point2D getPoint2D () {
    return new Point2D.Float (x2D, y2D);
  }
}  //class MouseWheelEvent2D



