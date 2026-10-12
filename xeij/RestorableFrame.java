//========================================================================================
//  RestorableFrame.java
//    en:Restorable frame -- Frames that can save and restore position and size
//    ja:リストアラブルフレーム -- 位置とサイズの保存と復元ができるフレーム
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;  //BufferedImage
import java.util.*;  //HashMap
import javax.swing.*;  //JFrame

public class RestorableFrame extends JFrame {

  static final boolean DEBUG = false;

  //  フレーム作成→復元する値の設定→復元→変化の記録→保存する値の取得
  //  または
  //  復元する値の設定→フレーム作成→復元→変化の記録→保存する値の取得

  static class Info {

    String key;
    RestorableFrame frame;  //フレーム
    int x, y;  //位置
    int width, height;  //サイズ
    int state;  //状態
    boolean opened;  //開いているか。開くのは個々のフレームで行う

    //new Info (frame)
    //  コンストラクタ
    Info (String key, RestorableFrame frame) {
      if (DEBUG) {
        System.out.println ("Info " + key + (frame == null ? " null" : " frame"));
      }
      this.key = key;
      this.frame = frame;
    }  //Info

    void setFrame (RestorableFrame frame) {
      if (DEBUG) {
        System.out.println ("Info.setFrame" + key);
      }
      this.frame = frame;
    }

    //set (rect, state, opened)
    //  復元する値を設定する
    void set (int[] rect, int state, boolean opened) {
      if (DEBUG) {
        System.out.println ("Info.set " + key);
      }
      x = rect[0];  //位置
      y = rect[1];
      width = rect[2];  //サイズ
      height = rect[3];
      this.state = state;  //状態
      this.opened = opened;  //開いているか
    }  //set

    //restore ()
    //  復元する。開くのは個々のフレームで行う
    void restore () {
      if (DEBUG) {
        System.out.println ("Info.restore " + key);
      }
      //フレームの上端の48x48が収まる画面を探す
    test:
      {
        Rectangle testBounds = new Rectangle (x, y, width, 48);  //フレームの上端のwidthx48
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment ().getScreenDevices ()) {
          for (GraphicsConfiguration gc : gd.getConfigurations ()) {
            Rectangle intersectionBounds = testBounds.intersection (gc.getBounds ());  //フレームの上端のwidthx48と画面が重なっている範囲
            if (48 <= intersectionBounds.width && 48 <= intersectionBounds.height) {  //48x48以上ある
              //フレームの上端の48x48が収まる画面が見つかった
              break test;  //そのまま復元する
            }
          }  //for gc
        }  //for gd
        //フレームの上端の48x48が収まる画面が見つからなかった
        //フレームの上端の48x48がデフォルトの画面に収まるように位置を調整する
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getDefaultConfiguration ();
        Rectangle s = gc.getBounds ();  //画面のレクタングル。左上が(0,0)とは限らない
        x = Math.min (x, s.x + s.width - 48);  //フレームの左端は画面の右端-48より左にあること
        if (0 < width) {
          x = Math.max (x + width, s.x + 48) - width;  //フレームの右端は画面の左端+48より右にあること
        }
        y = Math.min (y, s.y + s.height - 48);  //フレームの上端は画面の下端-48より上にあること
        y = Math.max (y, s.y);  //フレームの上端は画面の上端より下にあること。タイトルバーが見えないと困るので下端ではなく上端の条件
      }  //test
      //復元する
      frame.setLocation (x, y);  //位置
      if (0 < width && 0 < height) {
        frame.setSize (width, height);  //サイズ
        frame.setPreferredSize (new Dimension (width, height));  //setSizeだけだとパネルのsetPreferredSizeに負けてしまう
      }
      frame.setExtendedState (state);  //状態
      //変化の記録を開始する
      frame.addComponentListener (new ComponentAdapter () {
        @Override public void componentMoved (ComponentEvent ce) {
          record (false);  //記録する
        }
        @Override public void componentResized (ComponentEvent ce) {
          record (false);  //記録する
        }
      });
      frame.addWindowStateListener (new WindowStateListener () {
        @Override public void windowStateChanged (WindowEvent we) {
          record (false);  //記録する
        }
      });
      frame.addWindowListener (new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          //HIDE_ON_CLOSEのときclosedは呼び出されないがclosingは呼び出される
          //isShowing()はtrue
          record (true);  //記録する
        }
        @Override public void windowOpened (WindowEvent we) {
          //isShowing()はtrue
          record (false);  //記録する
        }
      });
    }  //restore

    //record (closing)
    //  変化を記録する
    void record (boolean closing) {
      if (DEBUG) {
        System.out.println ("Info.record " + key);
      }
      if (frame != GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getFullScreenWindow ()) {  //全画面でない
        boolean opened = !closing && frame.isShowing ();  //開いているか
        if (opened) {  //開いている
          Point p = frame.getLocationOnScreen ();  //位置
          Dimension d = frame.getSize ();  //サイズ
          int state = frame.getExtendedState ();  //状態
          if ((state & (Frame.ICONIFIED | Frame.MAXIMIZED_HORIZ)) == 0) {  //アイコン化または水平方向に最大化されていない
            x = p.x;  //X座標
            width = d.width;  //幅
          }
          if ((state & (Frame.ICONIFIED | Frame.MAXIMIZED_VERT)) == 0) {  //アイコン化または垂直方向に最大化されていない
            y = p.y;  //Y座標
            height = d.height;  //高さ
          }
          this.state = state;  //状態
        }
        this.opened = opened;  //開いているか
      }
    }  //record

    //rect = getRect ()
    //  位置とサイズを取得する
    int[] getRect () {
      if (DEBUG) {
        System.out.println ("Info.getRect " + key);
      }
      return new int[] { x, y, width, height };
    }  //getRect

    //state = getState ()
    //  状態を取得する
    int getState () {
      if (DEBUG) {
        System.out.println ("Info.getState " + key);
      }
      return state;
    }  //getState

    //opened = getOpened ()
    //  開いているかを取得する
    boolean getOpened () {
      if (DEBUG) {
        System.out.println ("Info.getOpened " + key);
      }
      return opened;
    }  //getOpened

    //image = capture ()
    //  キャプチャする
    BufferedImage capture () {
      if (DEBUG) {
        System.out.println ("Info.capture " + key);
      }
      if (frame != null) {  //フレームがある
        try {
          Point p = frame.getLocationOnScreen ();
          Dimension d = frame.getSize ();
          if (0 < d.width && 0 < d.height) {
            return new Robot ().createScreenCapture (new Rectangle (p.x, p.y, d.width, d.height));
          }
        } catch (Exception e) {
          //getLocationOnScreen IllegalComponentStateException 画面にない
          //Robot,AWTException
          //Robot,createScreenCapture SecurityException 権限がない
          //createScreenCapture IllegalArgumentException サイズが0以下
        }
      }
      return null;
    }  //capture

  }  //class Info



  static HashMap<String,Info> rfmMap = new HashMap<String,Info> ();

  //rfmSet (key, rect, state, opened)
  //  復元する値を設定する
  public static void rfmSet (String key, int[] rect, int state, boolean opened) {
    if (DEBUG) {
      System.out.println ("rfmSet " + key);
    }
    Info i = rfmMap.get (key);
    if (i == null) {  //フレームが作成されていない
      i = new Info (key, null);
      rfmMap.put (key, i);
      i.set (rect, state, opened);  //復元する値を設定する
    } else {  //フレームが作成されている
      i.set (rect, state, opened);  //復元する値を設定する
      i.restore ();  //復元する
    }
  }  //rfmSet

  //x = rfmGetRect (key)
  //  位置とサイズを取得する
  public static int[] rfmGetRect (String key) {
    if (DEBUG) {
      System.out.println ("rfmGetRect " + key);
    }
    return rfmMap.get (key).getRect ();
  }  //rfmGetRect

  //state = rfmGetState (key)
  //  状態を取得する
  public static int rfmGetState (String key) {
    if (DEBUG) {
      System.out.println ("rfmGetState " + key);
    }
    return rfmMap.get (key).getState ();
  }  //rfmGetState

  //opened = rfmGetOpened (key)
  //  開いているかを取得する
  public static boolean rfmGetOpened (String key) {
    if (DEBUG) {
      System.out.println ("rfmGetOpened " + key);
    }
    return rfmMap.get (key).getOpened ();
  }  //rfmGetOpened



  Info info;

  //new RestorableFrame (key, title)
  //  コンストラクタ
  public RestorableFrame (String key, String title) {
    super (title, GraphicsEnvironment.getLocalGraphicsEnvironment ().getDefaultScreenDevice ().getDefaultConfiguration ());
    if (DEBUG) {
      System.out.println ("RestorableFrame " + key);
    }
    Info i = rfmMap.get (key);
    if (i == null) {  //復元する値が設定されていない
      info = i = new Info (key, this);
    } else {  //復元する値が設定されている
      info = i;
      i.setFrame (this);
      i.restore ();  //復元する。開くのは個々のフレームで行う
    }
  }  //RestorableFrame

  //image = rfmCapture (key)
  //  フレームをキャプチャする
  public static BufferedImage rfmCapture (String key) {
    if (DEBUG) {
      System.out.println ("rfmCapture " + key);
    }
    return rfmMap.get (key).capture ();
  }  //rfmCapture



}  //class RestorableFrame



