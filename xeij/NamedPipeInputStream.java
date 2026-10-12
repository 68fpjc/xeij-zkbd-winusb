//========================================================================================
//  NamedPipeInputStream.java
//    en:Input from named pipe
//    ja:名前付きパイプから入力します
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
import java.util.*;  //NoSuchElementException

//class NamedPipeInputStream
//  名前付きパイプInputStream
public abstract class NamedPipeInputStream extends InputStream {

  //npis = NamedPipeInputStream.create (name)
  //  コンストラクタ
  //  npis  名前付きパイプInputStream
  //  name  名前付きパイプの名前。プレフィックスを含まない
  public static NamedPipeInputStream create (String name) throws IOException {
    return System.getProperty ("os.name").indexOf ("Windows") < 0 ? new Gen (name) : new Win (name);
  }  //open

  protected String path;  //パス。プレフィックスを含む
  protected volatile boolean closed;  //true=すでに閉じた
  protected volatile boolean connecting;  //true=接続待ちブロック中
  protected volatile boolean reading;  //true=受信待ちブロック中
  protected Thread thread;  //受信スレッド
  protected ByteQueue queue;  //受信キュー

  //npis = new NamedPipeInputStream (name)
  //  コンストラクタ
  //  npis  名前付きパイプInputStream
  //  name  名前付きパイプの名前。プレフィックスを含まない
  private NamedPipeInputStream (String name) throws IOException {
    //開始
    osStart (name);
    //受信キューを作る
    queue = new ByteQueue ();
    //受信スレッドを開始する
    thread = new Thread () {
      byte[] b = new byte[1024];
      @Override public void run () {
        try {
          //開いて接続する
          connecting = true;
          osOpenAndConnect ();  //送信側が開くまでブロックする
          connecting = false;
          while (!closed) {
            //受信する
            reading = true;
            int k = osRead (b);  //少なくとも1バイト受信するまでブロックする
            reading = false;
            if (closed) {
              break;
            }
            if (k == -1) {  //EOFのとき
              osClose ();  //直ちに接続し直す
              connecting = true;
              osOpenAndConnect ();  //送信側が開くまでブロックする
              connecting = false;
              continue;
            }
            //キューに書き込む
            queue.write (b, 0, k);
          }
        } catch (IOException ioe) {
          connecting = false;
          reading = false;
        }
        //閉じる
        osClose ();
      }
    };
    thread.start ();
    if (false) {
      System.out.println ("named pipe " + path + " opened");
    }
  }  //NamedPipeInputStream

  //------------------------------------------------------------------------
  //n = available ()
  //  ブロックせずに受信できる長さを返す
  //  n  ブロックせずに受信できる長さ
  @Override
  public int available () throws IOException {
    //閉じていたら失敗
    if (closed) {
      throw new IOException ("named pipe " + path + " closed");
    }
    //ブロックせずに受信できる長さを返す
    return queue.used ();
  }  //available

  //------------------------------------------------------------------------
  //close ()
  //  名前付きパイプを閉じる
  @Override
  public void close () {
    //閉じていたら何もしない
    if (closed) {
      return;
    }
    closed = true;
    //ブロックを解除する
    osUnblock ();
    //閉じる
    osClose ();
    //受信スレッドの終了を待つ
    try {
      thread.join (100L);
    } catch (InterruptedException ie) {
    }
    //終了
    osEnd ();
    //キューの読み出しをキャンセルする
    queue.cancel ();  //waitAndReadのブロックを解く
    if (false) {
      System.out.println ("named pipe " + path + " closed");
    }
  }  //close

  //------------------------------------------------------------------------
  //d = read ()
  //  1バイト受信する。1バイト受信するまでブロックする
  //  d  受信したデータ。0～255。-1=キャンセルされた
  @Override
  public int read () throws IOException {
    //閉じていたら失敗
    if (closed) {
      throw new IOException ("named pipe " + path + " closed");
    }
    //キューから1バイト読み出す
    return queue.waitAndRead ();  //1バイト読み出すまでブロックする
  }  //read

  //------------------------------------------------------------------------
  //k = read (b)
  //  配列に受信する。少なくとも1バイト受信するまでブロックする
  //  k  受信した長さ。-1=キャンセルされた
  //  b  配列
  @Override
  public int read (byte[] b) throws IOException {
    //閉じていたら失敗
    if (closed) {
      throw new IOException ("named pipe " + path + " closed");
    }
    //キューから配列に読み出す
    return queue.waitAndRead (b, 0, b.length);  //少なくとも1バイト読み出すまでブロックする
  }  //read

  //------------------------------------------------------------------------
  //k = read (b, o, n)
  //  配列に受信する。少なくとも1バイト受信するまでブロックする
  //  k  受信した長さ。-1=キャンセルされた
  //  b  配列
  //  o  位置
  //  n  長さ
  @Override
  public int read (byte[] b, int o, int n) throws IOException {
    //閉じていたら失敗
    if (closed) {
      throw new IOException ("named pipe " + path + " closed");
    }
    //引数を確認する
    if (o < 0 || n < 0 || b.length < o + n) {  //範囲外
      throw new IndexOutOfBoundsException ("b.length=" + b.length + ", o=" + o + ", n=" + n);
    }
    //キューから配列に読み出す。少なくとも1バイト読み出すまでブロックする
    return queue.waitAndRead (b, o, n);
  }  //read

  //------------------------------------------------------------------------
  //k = readNBytes (b, o, n)
  //  配列に受信する。nバイト受信するまでブロックする
  //  k  受信した長さ。-1=キャンセルされた
  //  b  配列
  //  o  位置
  //  n  長さ
  @Override
  public int readNBytes (byte[] b, int o, int n) throws IOException {
    //閉じていたら失敗
    if (closed) {
      throw new IOException ("named pipe " + path + " closed");
    }
    //引数を確認する
    if (o < 0 || n < 0 || b.length < o + n) {  //範囲外
      throw new IndexOutOfBoundsException ("b.length=" + b.length + ", o=" + o + ", n=" + n);
    }
    //キューから配列に読み出す
    int k = 0;
    while (k < n) {
      int t = queue.waitAndRead (b, o + k, n - k);  //少なくとも1バイト読み出すまでブロックする
      if (t == -1) {  //キャンセルされた
        return -1;
      }
      k += t;
    }
    //受信した長さを返す
    return k;
  }  //readNBytes

  protected abstract void osStart (String name) throws IOException;
  protected abstract void osOpenAndConnect () throws IOException;
  protected abstract int osRead (byte[] b) throws IOException;
  protected abstract void osUnblock ();
  protected abstract void osClose ();
  protected abstract void osEnd ();

  //class Gen
  //  UNIX系OS用
  private static class Gen extends NamedPipeInputStream {

    private File file;  //ファイル
    private FileInputStream stream;  //FileInputStream。null=まだ開いていないまたはすでに閉じた

    //npis = new Gen (name)
    //  コンストラクタ
    protected Gen (String name) throws IOException {
      super (name);
    }  //Gen

    //osStart (name)
    //  開始
    @Override
    protected void osStart (String name) throws IOException {
      //パス
      path = System.getProperty ("java.io.tmpdir") + "/" + name;  //通常は/tmp/～
      //ファイル
      file = new File (path);
      //mkfifoでFIFOを作る
      file.delete ();
      Process process;
      try {
        process = new ProcessBuilder ("mkfifo", path).inheritIO ().start ();
      } catch (IOException ioe) {  //start失敗。mkfifoを開始できなかった
        throw new IOException ("mkfifo " + path + " not started");
      }
      try {
        int exitCode = process.waitFor ();
        if (exitCode != 0) {  //mkfifoがエラー終了した
          file.delete ();
          throw new IOException ("mkfifo " + path + " terminated with exit code " + exitCode);
        }
      } catch (InterruptedException ie) {  //waitFor中断。mkfifoの終了を確認できなかった
        file.delete ();
        throw new IOException ("mkfifo " + path + " interrupted");
      }
    }  //osStart

    //osOpenAndConnect ()
    //  開いて接続する。送信側が開くまでブロックする
    @Override
    protected void osOpenAndConnect () throws IOException {
      if (stream == null) {
        stream = new FileInputStream (file);  //送信側が開くまでブロックする
      }
    }  //osOpenAndConnect

    //k = osRead (b)
    //  受信する。少なくとも1バイト受信するまでブロックする
    @Override
    protected int osRead (byte[] b) throws IOException {
      return stream.read (b);
    }  //osRead

    //osUnblock ()
    //  ブロックを解除する
    @Override
    protected void osUnblock () {
      //new FileInputStream()のブロックを解除する
      //  new FileInputStream()は送信側が開くまでブロックする。new FileOutputStream()でダミーの送信側を開くことで解除できる
      //  new FileInputStream()でブロックしていないのにnew FileOutputStream()を行うとnew FileOutputStream()がブロックしてしまう可能性がある
      if (connecting) {
        try {
          Thread.sleep (50L);
        } catch (InterruptedException ie) {
        }
        if (connecting) {
          try {
            new FileOutputStream (file).close ();
          } catch (IOException ioe) {
          }
          for (int i = 0; i < 100 && connecting; i++) {
            try {
              Thread.sleep (50L);
            } catch (InterruptedException ie) {
            }
          }
          if (connecting) {
            System.out.println ("named pipe " + path + " unblocking timeout");
          }
        }
      }
      //FileInputStream.read()のブロックが解除されるまで待つ
      //  FileInputStream.read()は送信側が開いたまま送信しないとブロックする。送信側が送信するか閉じるまでブロックを解除できない
      if (reading) {
        for (int i = 0; i < 100 && reading; i++) {
          try {
            Thread.sleep (50L);
          } catch (InterruptedException ie) {
          }
        }
        if (reading) {
          System.out.println ("user operation required to unblock named pipe " + path);
          while (reading) {
            try {
              Thread.sleep (1000L);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
    }  //osUnblock

    //osClose ()
    //  閉じる
    @Override
    protected void osClose () {
      //開いていたら閉じる
      if (stream != null) {
        try {
          stream.close ();
        } catch (IOException ioe) {
        }
        stream = null;
      }
    }  //osClose

    //osEnd ()
    //  終了
    @Override
    protected void osEnd () {
      file.delete ();
    }  //osEnd

  }  //class Gen

  //class Win
  //  Windows用
  private static class Win extends NamedPipeInputStream {

    //エラーコード
    //  https://learn.microsoft.com/en-us/windows/win32/debug/system-error-codes
    private static final int ERROR_BROKEN_PIPE = 109;  //パイプが終了した。誰かが開いて閉じた
    private static final int ERROR_BAD_PIPE = 230;  //パイプの状態が無効。誰も開いていない
    private static final int ERROR_NO_DATA = 232;  //既に切断された
    private static final int ERROR_PIPE_CONNECTED = 535;  //既に接続している
    private static final int ERROR_PIPE_LISTENING = 536;  //誰も開いていない
    private static final int ERROR_OPERATION_ABORTED = 995;  //スレッドが終了したか操作が取り消された
    private static final int ERROR_NOT_FOUND = 1168;  //取り消す操作がない
    private static final long INVALID_HANDLE_VALUE = -1L;

    //リンカ
    private Linker linker;
    private MethodHandle downcallHandle (MemorySegment address, FunctionDescriptor function) {
      return linker.downcallHandle (address, function);
    }

    //アリーナ
    private Arena arena;

    //メソッド
    private MethodHandle CancelIoEx;
    private MethodHandle CloseHandle;
    private MethodHandle ConnectNamedPipe;
    private static final int PIPE_ACCESS_INBOUND = 0x00000001;
    private static final int PIPE_TYPE_BYTE = 0x00000000;
    private static final int PIPE_WAIT = 0x00000000;
    private static final int PIPE_UNLIMITED_INSTANCES = 255;
    private static final int BUFFER_SIZE = 8192;
    private MethodHandle CreateNamedPipeA;
    private MethodHandle GetLastError;
    private MethodHandle ReadFile;

    private MemorySegment handle;  //HANDLE。null=まだ開いていないまたはすでに閉じた

    //npis = new Win (name)
    //  コンストラクタ
    protected Win (String name) throws IOException {
      super (name);
    }  //Win

    //osStart (name)
    //  開始
    @Override
    protected void osStart (String name) throws IOException {
      //リンカ
      linker = Linker.nativeLinker ();
      //アリーナ
      arena = Arena.ofAuto ();
      //ライブラリ
      SymbolLookup kernel32 = SymbolLookup.libraryLookup ("kernel32", arena);
      //メソッド
      try {
        //CancelIoEx関数
        //  https://learn.microsoft.com/ja-jp/windows/win32/fileio/cancelioex-func
        CancelIoEx = downcallHandle (
          kernel32.findOrThrow ("CancelIoEx"),
          FunctionDescriptor.of (
            ValueLayout.JAVA_INT,  //BOOL
            ValueLayout.ADDRESS,  //HANDLE hFile
            ValueLayout.ADDRESS));  //LPOVERLAPPED lpOverlapped
        //CloseHandle関数
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/handleapi/nf-handleapi-closehandle
        CloseHandle = downcallHandle (
          kernel32.findOrThrow ("CloseHandle"),
          FunctionDescriptor.of (
            ValueLayout.JAVA_INT,  //BOOL
            ValueLayout.ADDRESS));  //HANDLE hObject
        //ConnectNamedPipe関数
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/namedpipeapi/nf-namedpipeapi-connectnamedpipe
        ConnectNamedPipe = downcallHandle (
          kernel32.findOrThrow ("ConnectNamedPipe"),
          FunctionDescriptor.of (
            ValueLayout.JAVA_INT,  //BOOL
            ValueLayout.ADDRESS,  //HANDLE hNamedPipe
            ValueLayout.ADDRESS));  //LPOVERLAPPED lpOverlapped
        //CreateNamedPipeA関数
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/winbase/nf-winbase-createnamedpipea
        CreateNamedPipeA = downcallHandle (
          kernel32.findOrThrow ("CreateNamedPipeA"),
          FunctionDescriptor.of (
            ValueLayout.ADDRESS,  //HANDLE
            ValueLayout.ADDRESS,  //LPCSTR lpName
            ValueLayout.JAVA_INT,  //DWORD dwOpenMode
            ValueLayout.JAVA_INT,  //DWORD dwPipeMode
            ValueLayout.JAVA_INT,  //DWORD nMaxInstances
            ValueLayout.JAVA_INT,  //DWORD nOutBufferSize
            ValueLayout.JAVA_INT,  //DWORD nInBufferSize
            ValueLayout.JAVA_INT,  //DWORD nDefaultTimeOut
            ValueLayout.ADDRESS));  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
        //GetLastError関数
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/errhandlingapi/nf-errhandlingapi-getlasterror
        GetLastError = downcallHandle (
          kernel32.findOrThrow ("GetLastError"),
          FunctionDescriptor.of (
            ValueLayout.JAVA_INT));  //DWORD
        //ReadFile関数
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-readfilef
        ReadFile = downcallHandle (
          kernel32.findOrThrow ("ReadFile"),
          FunctionDescriptor.of (
            ValueLayout.JAVA_INT,  //BOOL
            ValueLayout.ADDRESS,  //HANDLE hFile
            ValueLayout.ADDRESS,  //LPVOID lpBuffer
            ValueLayout.JAVA_INT,  //DWORD nNumberOfBytesToRead
            ValueLayout.ADDRESS,  //LPDWORD lpNumberOfBytesRead
            ValueLayout.ADDRESS));  //LPOVERLAPPED lpOverlapped
      } catch (NoSuchElementException nsee) {
        nsee.printStackTrace ();
      }
      //パス
      path = "\\\\.\\pipe\\" + name;
    }  //osStart

    //osOpenAndConnect ()
    //  開いて接続する。送信側が開くまでブロックする
    @Override
    protected void osOpenAndConnect () throws IOException {
      //閉じていたら開く
      if (handle == null) {
        try {
          int error;
          //  https://learn.microsoft.com/ja-jp/windows/win32/api/winbase/nf-winbase-createnamedpipea
          if ((handle = (MemorySegment) CreateNamedPipeA.invoke (
            arena.allocateFrom (path),  //LPCSTR lpName
            PIPE_ACCESS_INBOUND,  //DWORD dwOpenMode
            PIPE_TYPE_BYTE | PIPE_WAIT,  //DWORD dwPipeMode
            PIPE_UNLIMITED_INSTANCES,  //DWORD nMaxInstances
            0,  //DWORD nOutBufferSize
            BUFFER_SIZE,  //DWORD nInBufferSize
            0,  //DWORD nDefaultTimeOut
            MemorySegment.NULL  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
            )).address () == INVALID_HANDLE_VALUE &&
              (error = (int) GetLastError.invoke ()) != -1) {
            handle = null;
            //開けなかったら失敗
            throw new IOException ("CreateNamedPipeA returned error code " + error);
          }
        } catch (IOException ioe) {
          throw ioe;
        } catch (Throwable e) {
          e.printStackTrace ();
          throw new IOException ("CreateNamedPipeA invocation failed");
        }
      }
      //接続を待つ。送信側が開くまでブロックする
      try {
        int error;
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/namedpipeapi/nf-namedpipeapi-connectnamedpipe
        if ((int) ConnectNamedPipe.invoke (
          handle,  //HANDLE hNamedPipe
          MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
            (error = (int) GetLastError.invoke ()) != -1) {
          if (error == 0 ||  //誰かがエラーコードを上書きした
              error == ERROR_NO_DATA ||  //232 既に切断された
              error == ERROR_PIPE_CONNECTED) {  //535 既に接続している
          } else if (error == ERROR_OPERATION_ABORTED) {  //995 スレッドが終了したか操作が取り消された
            throw new InterruptedIOException ("ConnectNamedPipe aborted");
          } else {
            throw new IOException ("ConnectNamedPipe returned error code " + error);
          }
        }
      } catch (IOException ioe) {
        throw ioe;
      } catch (Throwable e) {
        e.printStackTrace ();
        throw new IOException ("ConnectNamedPipe invocation failed");
      }
    }  //osOpenAndConnect

    //k = osRead (b)
    //  受信する。少なくとも1バイト受信するまでブロックする
    @Override
    protected int osRead (byte[] b) throws IOException {
      int n = b.length;
      MemorySegment buf = arena.allocate ((long) n);  //MemorySegment.ofArray(b)はasSlice(o+k)できない
      int k = 0;
      MemorySegment t = arena.allocate (ValueLayout.JAVA_INT);
      t.set (ValueLayout.JAVA_INT, 0L, 0);
      try {
        while (k < 1) {
          t.set (ValueLayout.JAVA_INT, 0L, 0);
          int error;
          //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-readfile
          if ((int) ReadFile.invoke (
            handle,  //HANDLE hFile
            buf.asSlice ((long) k),  //LPVOID lpBuffer
            n - k,  //DWORD nNumberOfBytesToRead
            t,  //LPDWORD lpNumberOfBytesRead
            MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
              (error = (int) GetLastError.invoke ()) != -1) {
            if (closed) {
              throw new IOException ("named pipe " + path + " closed");
            }
            if (error == 0 ||  //誰かがエラーコードを上書きした
                error == ERROR_BROKEN_PIPE ||  //109 パイプが終了した。誰かが開いて閉じた
                error == ERROR_PIPE_LISTENING) {  //536 誰も開いていない
              //0のとき閉じたのか取り消されたのかわからないが、後者はclosedなのでここには来ない
              return -1;  //EOF
            } else if (error == ERROR_OPERATION_ABORTED) {  //995 スレッドが終了したか操作が取り消された
              throw new InterruptedIOException ("ReadFile aborted");
            } else {
              throw new IOException ("ReadFile returned error code " + error);
            }
          }
          if (closed) {
            throw new IOException ("named pipe " + path + " closed");
          }
          k += t.get (ValueLayout.JAVA_INT, 0L);
        }
      } catch (IOException ioe) {
        throw ioe;
      } catch (Throwable e) {
        e.printStackTrace ();
        throw new IOException ("ReadFile invocation failed");
      }
      System.arraycopy (buf.toArray (ValueLayout.JAVA_BYTE), 0, b, 0, k);
      return k;
    }  //osRead

    //osUnblock ()
    //  ブロックを解除する
    @Override
    protected void osUnblock () {
      //ConnectNamedPipeまたはReadFileのブロックを解除する
      //  CancelIoExで取り消された操作はERROR_OPERATION_ABORTEDになる
      //  MethodHandle.invoke()の問題で、取り消された操作がERROR_OPERATION_ABORTEDでなく0を受け取る可能性がある
      if (connecting || reading) {
        try {
          Thread.sleep (50L);
        } catch (InterruptedException ie) {
        }
        if (connecting) {
          try {
            int error;
            //  https://learn.microsoft.com/ja-jp/windows/win32/fileio/cancelioex-func
            if ((int) CancelIoEx.invoke (
              handle,  //HANDLE hFile
              MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
                (error = (int) GetLastError.invoke ()) != -1) {
              //if (error == ERROR_NOT_FOUND) {  //1168 取り消す操作がない
              //}
            }
          } catch (Throwable e) {
            //e.printStackTrace ();
            //throw new IOException ("CancelIoEx invocation failed");
          }
          for (int i = 0; i < 100 && (connecting || reading); i++) {
            try {
              Thread.sleep (50L);
            } catch (InterruptedException ie) {
            }
          }
          if (connecting || reading) {
            System.out.println ("named pipe " + path + " unblocking timeout");
          }
        }
      }
    }  //osUnblock

    //osClose ()
    //  閉じる
    @Override
    protected void osClose () {
      //開いていたら閉じる
      if (handle != null) {
        try {
          int error;
          //  https://learn.microsoft.com/ja-jp/windows/win32/api/handleapi/nf-handleapi-closehandle
          if ((int) CloseHandle.invoke (
            handle) == 0 &&  //HANDLE hObject
              (error = (int) GetLastError.invoke ()) != -1) {
            //throw new IOException ("CloseHandle returned error code " + error);
          }
          //} catch (IOException ioe) {
          //  throw ioe;
        } catch (Throwable e) {
          e.printStackTrace ();
          //throw new IOException ("CloseHandle invocation failed");
        }
        handle = null;
      }
    }  //osClose

    //osEnd ()
    //  終了
    @Override
    protected void osEnd () {
    }  //osEnd

  }  //class Win

}  //class NamedPipeInputStream
