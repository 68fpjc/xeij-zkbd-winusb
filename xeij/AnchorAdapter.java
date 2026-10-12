//========================================================================================
//  AnchorAdapter.java
//    en:Anchor adapter -- It is a mouse adapter which passes the predetermined URI to a browser when it is clicked.
//    ja:アンカーアダプタ -- クリックされたとき所定のURIをブラウザに渡すマウスアダプタです。
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
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.net.*;  //MalformedURLException,URI,URL

public class AnchorAdapter extends MouseAdapter {
  private URI uri;
  public AnchorAdapter (String str) {
    uri = null;
    try {
      uri = new URI (str);
    } catch (Exception e) {
    }
  }
  @Override public void mouseClicked (MouseEvent me) {
    if (uri != null) {
      try {
        Desktop.getDesktop ().browse (uri);  //URIをブラウザに渡す
      } catch (Exception e) {
        //e.printStackTrace ();
      }
    }
  }
}  //class AnchorAdapter



