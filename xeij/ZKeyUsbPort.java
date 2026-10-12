//========================================================================================
//  ZKeyUsbPort.java
//    en:Z keyboard USB port (libusb / WinUSB)
//    ja:ZキーボードUSBポート (libusb / WinUSB)
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.io.*;
import java.lang.foreign.*;  //Arena,FunctionDescriptor,Linker,MemorySegment,SymbolLookup,ValueLayout
import java.lang.invoke.*;  //MethodHandle
import java.nio.file.*;  //Files,Path

//Zキーボードを libusb 経由で直接読み書きする
//  Interface 0 と Interface 1 の両方を Zadig などで WinUSB ドライバに置き換えておくこと
//  libusb-1.0.dll は次の順に探す。jar に同梱した DLL (x64 / ARM64) があるので通常は何も置かなくてよい
//    システムプロパティ xeij.libusb で指定したパス
//    カレントディレクトリ
//    XEiJ の jar ファイルと同じディレクトリ
//    DLL の検索パス
//    jar に同梱した DLL (一時ディレクトリに展開)
//  キー入力は HID Boot Keyboard のレポート (Interface 0) から X68000 のスキャンコードに変換して通知する
public final class ZKeyUsbPort implements ZKeyPort {

  //キーの押下/解放の通知先
  public interface Listener {
    void key (int x68kCode, boolean pressed);
  }

  private static final int VID = 0x33dd;
  private static final int PID = 0x0011;
  private static final int EP_KEYS = 0x81;
  private static final int LIBUSB_ERROR_TIMEOUT = -7;
  private static final int POLL_TIMEOUT_MS = 500;
  private static final int REPORT_LENGTH = 65;
  private static final int[] LED_INDEXES = { 7, 8, 9, 10, 11, 13, 14 };

  //HID Usage ID (Keyboard/Keypad page) → X68000 スキャンコード。0=割り当てなし
  private static final byte[] USAGE_TO_X68K = new byte[256];
  //修飾キーのビット → X68000 スキャンコード。LCtrl,LShift,LAlt,LWin,RCtrl,RShift,RAlt,RWin
  private static final int[] MOD_TO_X68K = { 0x71, 0x70, 0x55, 0x5f, 0x71, 0x70, 0x59, 0x60 };
  static {
    int[][] t = {
      //文字キー
      { 0x04, 0x1e }, { 0x05, 0x2e }, { 0x06, 0x2c }, { 0x07, 0x20 }, { 0x08, 0x13 },  //A B C D E
      { 0x09, 0x21 }, { 0x0a, 0x22 }, { 0x0b, 0x23 }, { 0x0c, 0x18 }, { 0x0d, 0x24 },  //F G H I J
      { 0x0e, 0x25 }, { 0x0f, 0x26 }, { 0x10, 0x30 }, { 0x11, 0x2f }, { 0x12, 0x19 },  //K L M N O
      { 0x13, 0x1a }, { 0x14, 0x11 }, { 0x15, 0x14 }, { 0x16, 0x1f }, { 0x17, 0x15 },  //P Q R S T
      { 0x18, 0x17 }, { 0x19, 0x2d }, { 0x1a, 0x12 }, { 0x1b, 0x2b }, { 0x1c, 0x16 },  //U V W X Y
      { 0x1d, 0x2a },  //Z
      //数字キー 1～0
      { 0x1e, 0x02 }, { 0x1f, 0x03 }, { 0x20, 0x04 }, { 0x21, 0x05 }, { 0x22, 0x06 },
      { 0x23, 0x07 }, { 0x24, 0x08 }, { 0x25, 0x09 }, { 0x26, 0x0a }, { 0x27, 0x0b },
      //リターン ESC BS TAB スペース － ＾ ＠ ［ ］ ； ： ， ． ／
      { 0x28, 0x1d }, { 0x29, 0x01 }, { 0x2a, 0x0f }, { 0x2b, 0x10 }, { 0x2c, 0x35 },
      { 0x2d, 0x0c }, { 0x2e, 0x0d }, { 0x2f, 0x1b }, { 0x30, 0x1c }, { 0x32, 0x29 },
      { 0x33, 0x27 }, { 0x34, 0x28 }, { 0x36, 0x31 }, { 0x37, 0x32 }, { 0x38, 0x33 },
      //BREAK CAPS
      { 0x35, 0x61 }, { 0x39, 0x5d },
      //かな ローマ字 コード入力
      { 0x46, 0x5a }, { 0x47, 0x5b }, { 0x48, 0x5c },
      //INS HOME ROLLUP DEL ROLLDOWN → ← ↓ ↑
      { 0x49, 0x5e }, { 0x4a, 0x36 }, { 0x4b, 0x38 }, { 0x4c, 0x37 }, { 0x4e, 0x39 },
      { 0x4f, 0x3d }, { 0x50, 0x3b }, { 0x51, 0x3e }, { 0x52, 0x3c },
      //UNDO CLR
      { 0x4d, 0x3a }, { 0x53, 0x3f },
      //テンキー ／ ＊ － ＋ ENTER 1～9 0 ． ＝ ，
      { 0x54, 0x40 }, { 0x55, 0x41 }, { 0x56, 0x42 }, { 0x57, 0x46 }, { 0x58, 0x4e },
      { 0x59, 0x4b }, { 0x5a, 0x4c }, { 0x5b, 0x4d }, { 0x5c, 0x47 }, { 0x5d, 0x48 },
      { 0x5e, 0x49 }, { 0x5f, 0x43 }, { 0x60, 0x44 }, { 0x61, 0x45 }, { 0x62, 0x4f },
      { 0x63, 0x51 }, { 0x67, 0x4a }, { 0x85, 0x50 },
      //F1～F10 OPT.1 OPT.2
      { 0x3a, 0x63 }, { 0x3b, 0x64 }, { 0x3c, 0x65 }, { 0x3d, 0x66 }, { 0x3e, 0x67 },
      { 0x3f, 0x68 }, { 0x40, 0x69 }, { 0x41, 0x6a }, { 0x42, 0x6b }, { 0x43, 0x6c },
      { 0x44, 0x72 }, { 0x45, 0x73 },
      //COPY 記号入力 登録 HELP XF2 XF3 XF4 ￥ ＿
      { 0x65, 0x62 }, { 0x7f, 0x52 }, { 0x81, 0x53 }, { 0x80, 0x54 }, { 0x8b, 0x56 },
      { 0x8a, 0x57 }, { 0x88, 0x58 }, { 0x89, 0x0e }, { 0x87, 0x34 },
    };
    for (int[] e : t) {
      USAGE_TO_X68K[e[0]] = (byte) e[1];
    }
  }

  private boolean debugFlag;
  @Override public void setDebugFlag (boolean debugFlag) {
    this.debugFlag = debugFlag;
  }

  private final Arena arena = Arena.ofShared ();
  private final Listener listener;
  private MethodHandle libusb_init;
  private MethodHandle libusb_exit;
  private MethodHandle libusb_open_device_with_vid_pid;
  private MethodHandle libusb_close;
  private MethodHandle libusb_claim_interface;
  private MethodHandle libusb_release_interface;
  private MethodHandle libusb_interrupt_transfer;
  private MethodHandle libusb_control_transfer;
  private MemorySegment context = MemorySegment.NULL;
  private MemorySegment handle = MemorySegment.NULL;
  private volatile boolean running;
  private Thread readThread;
  private final boolean[] held = new boolean[128];  //X68000 スキャンコードごとの押下状態

  //port = new ZKeyUsbPort (listener, debugFlag)
  //  libusb-1.0.dll が見つからないとき、デバイスが見つからないとき、WinUSB ドライバが割り当てられていないときは IOException
  public ZKeyUsbPort (Listener listener, boolean debugFlag) throws IOException {
    this.listener = listener;
    this.debugFlag = debugFlag;
    try {
      bind ();
      int r;
      MemorySegment ctxPtr = arena.allocate (ValueLayout.ADDRESS);
      if ((r = (int) libusb_init.invoke (ctxPtr)) != 0) {
        throw new IOException ("libusb_init failed (" + r + ")");
      }
      context = ctxPtr.get (ValueLayout.ADDRESS, 0L);
      handle = (MemorySegment) libusb_open_device_with_vid_pid.invoke (context, (short) VID, (short) PID);
      if (handle.equals (MemorySegment.NULL)) {
        throw new IOException ("device not found or WinUSB driver is not installed");
      }
      for (int i = 0; i < 2; i++) {
        if ((r = (int) libusb_claim_interface.invoke (handle, i)) != 0) {
          throw new IOException ("libusb_claim_interface " + i + " failed (" + r + ")");
        }
      }
    } catch (IOException e) {
      closeInternal ();
      throw e;
    } catch (Throwable e) {
      closeInternal ();
      throw new IOException (e);
    }
    if (debugFlag) {
      System.out.println ("ZKeyUsbPort: opened");
    }
    running = true;
    readThread = new Thread (this::readLoop, "ZKeyUsbPort");
    readThread.setDaemon (true);
    readThread.start ();
  }

  //jar に同梱した libusb-1.0.dll を一時ディレクトリに展開してパスを返す。同梱されていなければ null
  private static String extractBundled (StringBuilder log) {
    String arch = System.getProperty ("os.arch", "");
    String res = "/libusb/windows-" + arch + "/libusb-1.0.dll";
    try (InputStream in = ZKeyUsbPort.class.getResourceAsStream (res)) {
      if (in == null) {
        log.append ("\n  (no bundled library for ").append (arch).append (")");
        return null;
      }
      byte[] data = in.readAllBytes ();
      Path dir = Path.of (System.getProperty ("java.io.tmpdir"), "xeij-libusb", arch);
      Files.createDirectories (dir);
      Path file = dir.resolve ("libusb-1.0.dll");
      if (!Files.exists (file) || !java.util.Arrays.equals (Files.readAllBytes (file), data)) {
        Files.write (file, data);
      }
      return file.toString ();
    } catch (IOException | RuntimeException e) {
      log.append ("\n  bundled extraction failed: ").append (e);
      return null;
    }
  }

  private void bind () throws IOException {
    SymbolLookup lookup = null;
    StringBuilder tried = new StringBuilder ();
    String property = System.getProperty ("xeij.libusb");
    java.util.List<String> candidates = new java.util.ArrayList<String> ();
    if (property != null) {
      candidates.add (property);
    }
    candidates.add (Path.of ("libusb-1.0.dll").toAbsolutePath ().toString ());
    try {
      Path jar = Path.of (ZKeyUsbPort.class.getProtectionDomain ().getCodeSource ().getLocation ().toURI ());
      Path dir = Files.isDirectory (jar) ? jar : jar.getParent ();
      if (dir != null) {
        candidates.add (dir.resolve ("libusb-1.0.dll").toString ());
      }
    } catch (Exception e) {
    }
    candidates.add ("libusb-1.0.dll");
    String bundled = extractBundled (tried);
    if (bundled != null) {
      candidates.add (bundled);
    }
    for (String name : candidates) {
      try {
        lookup = SymbolLookup.libraryLookup (name, arena);
        if (debugFlag) {
          System.out.println ("ZKeyUsbPort: loaded " + name);
        }
        break;
      } catch (RuntimeException e) {
        tried.append ("\n  ").append (name).append (" : ").append (e.getMessage ());
      }
    }
    if (lookup == null) {
      throw new IOException ("libusb-1.0.dll not found" + tried);
    }
    Linker linker = Linker.nativeLinker ();
    ValueLayout.OfInt I = ValueLayout.JAVA_INT;
    ValueLayout.OfShort S = ValueLayout.JAVA_SHORT;
    ValueLayout.OfByte B = ValueLayout.JAVA_BYTE;
    AddressLayout A = ValueLayout.ADDRESS;
    libusb_init = linker.downcallHandle (lookup.find ("libusb_init").orElseThrow (),
                                         FunctionDescriptor.of (I, A));
    libusb_exit = linker.downcallHandle (lookup.find ("libusb_exit").orElseThrow (),
                                         FunctionDescriptor.ofVoid (A));
    libusb_open_device_with_vid_pid = linker.downcallHandle (lookup.find ("libusb_open_device_with_vid_pid").orElseThrow (),
                                                             FunctionDescriptor.of (A, A, S, S));
    libusb_close = linker.downcallHandle (lookup.find ("libusb_close").orElseThrow (),
                                          FunctionDescriptor.ofVoid (A));
    libusb_claim_interface = linker.downcallHandle (lookup.find ("libusb_claim_interface").orElseThrow (),
                                                    FunctionDescriptor.of (I, A, I));
    libusb_release_interface = linker.downcallHandle (lookup.find ("libusb_release_interface").orElseThrow (),
                                                      FunctionDescriptor.of (I, A, I));
    libusb_interrupt_transfer = linker.downcallHandle (lookup.find ("libusb_interrupt_transfer").orElseThrow (),
                                                       FunctionDescriptor.of (I, A, B, A, I, A, I));
    libusb_control_transfer = linker.downcallHandle (lookup.find ("libusb_control_transfer").orElseThrow (),
                                                     FunctionDescriptor.of (I, A, B, B, S, S, A, S, I));
  }

  //読み取りスレッド
  private void readLoop () {
    MemorySegment buf = arena.allocate (8);
    MemorySegment actual = arena.allocate (ValueLayout.JAVA_INT);
    byte[] report = new byte[8];
    try {
      while (running) {
        actual.set (ValueLayout.JAVA_INT, 0L, 0);
        int r = (int) libusb_interrupt_transfer.invoke (handle, (byte) EP_KEYS, buf, 8, actual, POLL_TIMEOUT_MS);
        //タイムアウトで取り消されたときも、競合でデータが届いていることがあるので捨てない
        if (r != 0 && !(r == LIBUSB_ERROR_TIMEOUT && actual.get (ValueLayout.JAVA_INT, 0L) >= 8)) {
          if (r == LIBUSB_ERROR_TIMEOUT) {
            continue;
          }
          if (debugFlag) {
            System.out.printf ("ZKeyUsbPort: interrupt transfer failed (%d)\n", r);
          }
          break;
        }
        if (actual.get (ValueLayout.JAVA_INT, 0L) < 8) {
          continue;
        }
        MemorySegment.copy (buf, ValueLayout.JAVA_BYTE, 0L, report, 0, 8);
        process (report);
      }
    } catch (Throwable e) {
      e.printStackTrace ();
    }
    //切断やエラーで終了するときは押されているキーをすべて離す
    for (int code = 0; code < held.length; code++) {
      if (held[code]) {
        held[code] = false;
        listener.key (code, false);
      }
    }
  }

  //レポートを前回との差分に変換して通知する
  private void process (byte[] report) {
    boolean[] now = new boolean[128];
    for (int i = 0; i < 8; i++) {
      if ((report[0] & (1 << i)) != 0) {
        now[MOD_TO_X68K[i]] = true;
      }
    }
    for (int i = 2; i < 8; i++) {
      int code = USAGE_TO_X68K[report[i] & 0xff];
      if (code != 0) {
        now[code] = true;
      }
    }
    if (debugFlag) {
      System.out.printf ("ZKeyUsbPort: %02x %02x %02x %02x %02x %02x %02x %02x\n",
                         report[0], report[1], report[2], report[3], report[4], report[5], report[6], report[7]);
    }
    for (int code = 1; code < now.length; code++) {
      if (now[code] != held[code]) {
        held[code] = now[code];
        listener.key (code, now[code]);
      }
    }
  }

  //LEDのデータを送る。ZKeyLEDPort.sendと同じ形式
  @Override public synchronized boolean send (long data) {
    if (handle.equals (MemorySegment.NULL)) {
      return false;
    }
    try (Arena a = Arena.ofConfined ()) {
      MemorySegment report = a.allocate (REPORT_LENGTH);
      report.fill ((byte) 0);
      report.set (ValueLayout.JAVA_BYTE, 0L, (byte) 0x0a);
      report.set (ValueLayout.JAVA_BYTE, 1L, (byte) 0xf8);
      for (int i = 0; i < 7; i++) {
        report.set (ValueLayout.JAVA_BYTE, (long) LED_INDEXES[i], (byte) (data >> (8 * i)));
      }
      //SET_REPORT, Feature, Report ID 10, Interface 1
      int r = (int) libusb_control_transfer.invoke (handle, (byte) 0x21, (byte) 0x09, (short) 0x030a, (short) 1,
                                                    report, (short) REPORT_LENGTH, 1000);
      if (r < 0) {
        if (debugFlag) {
          System.out.printf ("ZKeyUsbPort: SET_REPORT failed (%d)\n", r);
        }
        return false;
      }
      return true;
    } catch (Throwable e) {
      e.printStackTrace ();
      return false;
    }
  }

  //WinUSBではOSのロック状態に依存しないので、NumLockは常に点灯しているものとして扱う
  @Override public boolean isKeyToggled (int vk) {
    return true;
  }

  @Override public void hitKey (int vk) {
  }

  @Override public void close () {
    running = false;
    if (readThread != null && readThread != Thread.currentThread ()) {
      try {
        readThread.join (1000L);
      } catch (InterruptedException e) {
      }
    }
    closeInternal ();
  }

  private synchronized void closeInternal () {
    try {
      if (!handle.equals (MemorySegment.NULL)) {
        for (int i = 0; i < 2; i++) {
          libusb_release_interface.invoke (handle, i);
        }
        libusb_close.invoke (handle);
        handle = MemorySegment.NULL;
      }
      if (!context.equals (MemorySegment.NULL)) {
        libusb_exit.invoke (context);
        context = MemorySegment.NULL;
      }
    } catch (Throwable e) {
      e.printStackTrace ();
    }
  }

}  //class ZKeyUsbPort
