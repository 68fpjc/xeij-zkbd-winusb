//========================================================================================
//  MMLCompiler.java
//    en:Simplified MML compiler
//    ja:簡易MMLコンパイラ
//  Copyright (C) 2003-2022 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.util.*;

public class MMLCompiler {

  public static final int TONES = 200;  //音色データの数
  public static final int TRACKS = 80;  //トラックの数

  //MMLCompiler ()
  //  コンストラクタ
  public MMLCompiler () {
    //音色データ
    mmcToneData = new byte[55 * TONES];
    System.arraycopy (TONE_DATA_68, 0, mmcToneData, 0, TONE_DATA_68.length);
    Arrays.fill (mmcToneData, TONE_DATA_68.length, 55 * TONES, (byte) 0);
    //テンポ
    mmcTempo = (60.0 / 120.0) / 48.0 * 1000000.0;
    //トラック
    mmcTqToTrack = new Track[TRACKS];
    for (int tq = 0; tq < TRACKS; tq++) {
      mmcTqToTrack[tq] = new Track (tq);
    }
    mmcCnToTq = new int[8];
    for (int cn = 0; cn < 8; cn++) {
      mmcCnToTq[cn] = -1;
    }
  }

  //array = compile (program)
  //  OPMデータをコンパイルする
  //  program  OPMプログラム
  //    (A ch tr)
  //      assign
  //      チャンネルにトラックを割り当てる
  //      ch  チャンネル番号。1～8またはFM1～FM8
  //      tr  トラック番号。1～TRACKS。0=解除
  //      複数のチャンネルに同じトラックを割り当てられる
  //    (I mode)
  //      init
  //      初期化する
  //      mode  0  音色データを初期化しない
  //            1  音色データをX68000の音色で初期化する
  //            2  音色データをX1の音色で初期化する
  //    (M tr size)
  //      alloc
  //      トラックを確保する
  //      tr    トラック番号。1～TRACKS
  //      size  トラックの容量
  //      ここでは何もしない
  //    (P)
  //      play
  //      演奏を開始する
  //      ここではMMLデータをコンパイルする
  //    (T ch) mml
  //      trk
  //      トラックにMMLデータを追加する
  //      ch   チャンネル番号。1～8
  //      mml  MMLデータ
  //    (V n s v[s] ... v[54])
  //      vset
  //      音色を設定する
  //      n     音色番号
  //      s     設定開始インデックス
  //      v[i]  音色要素
  //    /～
  //      comment
  //      注釈。行末まで無視する
  //  array  {時刻(us),アドレス<<8|データ}の並び。末尾は{長さ(us),-1}。null=失敗
  //         このデータ構造では(2^31-1)/1000000=2147秒を超えられない
  public int[] compile (String program) {
    mmcProgram = program;
    mmcIndex = 0;
    mmcLine = 1;
    mmcOutput = new ArrayList<Integer> ();
    mmcError = "";
    for (;;) {
      mmcSkipSpace (-1);
      int c = mmcGetChar ();
      if (c == -1) {
        break;
      }
      if (c != '(') {  //'('がない
        mmcError = "line " + mmcLine + ": ( not found";
        return null;
      }
      c = mmcGetChar ();
      boolean success = true;
      if (c == 'A' || c == 'a') {
        success = mmcCommandA ();
      } else if (c == 'I' || c == 'i') {
        success = mmcCommandI ();
      } else if (c == 'M' || c == 'm') {
        success = mmcCommandM ();
      } else if (c == 'P' || c == 'p') {
        success = mmcCommandP ();
      } else if (c == 'T' || c == 't') {
        success = mmcCommandT ();
      } else if (c == 'V' || c == 'v') {
        success = mmcCommandV ();
      } else {
        mmcError = "line " + mmcLine + ": unknown command";
        return null;
      }
      if (!success) {
        return null;
      }
    }
    //終了コードを書き込む
    mmcOutput.add (mmcTqToTrack[0].getTimeUS ());
    mmcOutput.add (-1);
    //リストを配列に変換する
    int size = mmcOutput.size ();
    int[] array = new int[size];
    for (int i = 0; i < size; i++) {
      array[i] = mmcOutput.get (i);
    }
    mmcOutput = null;
    return array;
  }

  //error = getError ()
  //  エラーメッセージを返す
  public String getError () {
    return mmcError;
  }



  protected byte[] mmcToneData;  //音色データ
  protected double mmcTempo;  //テンポ(us/絶対音長)。(60/BPM)/48*1000000
  protected Track[] mmcTqToTrack;  //[トラック番号-1]=トラック
  protected int[] mmcCnToTq;  //[チャンネル番号-1]=トラック番号-1

  protected String mmcProgram;  //OPMデータ
  protected int mmcIndex;  //mmcProgramのインデックス
  protected int mmcLine;  //mmcProgramの行番号
  protected ArrayList<Integer> mmcOutput;  //コンパイル結果
  protected String mmcError;  //エラーメッセージ

  //c = mmcGetChar ()
  //  次の文字を取り出す
  //  c  次の文字。-1=終了
  protected int mmcGetChar () {
    int c = -1;
    if (mmcIndex < mmcProgram.length ()) {
      c = mmcProgram.charAt (mmcIndex++);
      if (c == '\n') {
        mmcLine++;
      }
    }
    return c;
  }

  //mmcUngetChar (c)
  //  次の文字を取り出さなかったことにする
  //  c  取り出さなかったことにする文字。-1=終了
  protected void mmcUngetChar (int c) {
    if (c != -1) {
      mmcIndex--;
      if (c == '\n') {
        mmcLine--;
      }
    }
  }

  //c = mmcSkipSpace (comma)
  //  空白と注釈と0～1個のコンマを読み飛ばす
  //  c      次の文字。-1=終了
  //  comma  -1   コンマを読み飛ばさない
  //         ','  コンマを1個まで読み飛ばす
  protected int mmcSkipSpace (int comma) {
    for (;;) {
      int c = mmcGetChar ();
      if (c == -1) {  //終了
        return -1;
      }
      if (c <= ' ') {  //空白
        continue;
      }
      if (c == '/') {  //注釈
        do {
          c = mmcGetChar ();
          if (c == -1) {
            return -1;
          }
        } while (c != '\n');
        continue;
      }
      if (c == comma) {  //1個目のコンマ
        comma = -1;
        continue;
      }
      mmcUngetChar (c);
      return c;
    }
  }

  //n = mmcGetNumber (comma)
  //  数値を取り出す
  //  n      数値。0～。-1=数値がない
  //  comma  -1   コンマを読み飛ばさない
  //         ','  コンマを1個まで読み飛ばす
  protected int mmcGetNumber (int comma) {
    mmcSkipSpace (comma);
    int n = -1;
    int c = mmcGetChar ();
    if (c == '$') {  //16進数
      c = mmcGetChar ();
      if (('0' <= c && c <= '9') ||
          ('A' <= c && c <= 'F') ||
          ('a' <= c && c <= 'f')) {
        n = 0;
        do {
          n = 16 * n + (c <= '9' ? c - '0' : (c | 0x20) - 'a' + 10);
          c = mmcGetChar ();
        } while (('0' <= c && c <= '9') ||
                 ('A' <= c && c <= 'F') ||
                 ('a' <= c && c <= 'f'));
      }
    } else {  //10進数
      if ('0' <= c && c <= '9') {
        n = 0;
        do {
          n = 10 * n + (c - '0');
          c = mmcGetChar ();
        } while ('0' <= c && c <= '9');
      }
    }
    mmcUngetChar (c);
    return n;
  }

  //success = mmcCommandA ()
  //    (A ch tr)
  //      assign
  //      チャンネルにトラックを割り当てる
  //      ch  チャンネル番号。1～8またはFM1～FM8
  //      tr  トラック番号。1～TRACKS。0=解除
  //      複数のチャンネルに同じトラックを割り当てられる
  protected boolean mmcCommandA () {
    //チャンネル
    int c = mmcGetChar ();
    if (c == 'F' || c == 'f') {
      c = mmcGetChar ();
      if (c == 'M' || c == 'm') {
      } else {
        mmcUngetChar (c);
        mmcError = "line " + mmcLine + ": syntax error";
      }
    } else {
      mmcUngetChar (c);
    }
    int ch = mmcGetNumber (-1);
    if (ch < 1 || 8 < ch) {  //チャンネルが範囲外
      mmcError = "line " + mmcLine + ": channel out of range";
      return false;
    }
    int cn = ch - 1;
    //トラック番号
    int tr = mmcGetNumber (',');
    if (tr < 1 || TRACKS < tr) {  //トラック番号が範囲外
      mmcError = "line " + mmcLine + ": track number out of range";
      return false;
    }
    int tq = tr - 1;
    c = mmcGetChar ();
    if (c != ')') {  //')'がない
      mmcError = "line " + mmcLine + ": ) not found";
      return false;
    }
    mmcCnToTq[cn] = tq;
    return true;
  }

  //success = mmcCommandI ()
  //    (I mode)
  //      init
  //      初期化する
  //      mode  0  音色データを初期化しない
  //            1  音色データをX68000の音色で初期化する
  //            2  音色データをX1の音色で初期化する
  protected boolean mmcCommandI () {
    //モード
    int mode = mmcGetNumber (-1);
    if (mode == -1) {  //モードが指定されていない
      mode = 0;  //音色データを初期化しない
    } else {  //モードが指定されている
      if (mode < 0 || 2 < mode) {  //モードが範囲外
        mmcError = "line " + mmcLine + ": mode out of range";
        return false;
      }
    }
    int c = mmcGetChar ();
    if (c != ')') {  //')'がない
      mmcError = "line " + mmcLine + ": ) not found";
      return false;
    }
    if (mode == 1) {
      System.arraycopy (TONE_DATA_68, 0, mmcToneData, 0, TONE_DATA_68.length);
      Arrays.fill (mmcToneData, TONE_DATA_68.length, 55 * TONES, (byte) 0);
    } else if (mode == 2) {
      System.arraycopy (TONE_DATA_X1, 0, mmcToneData, 0, TONE_DATA_X1.length);
      Arrays.fill (mmcToneData, TONE_DATA_X1.length, 55 * TONES, (byte) 0);
    }
    //テンポ
    mmcTempo = (60.0 / 120.0) / 48.0 * 1000000.0;
    //トラック
    for (int tq = 0; tq < TRACKS; tq++) {
      Track track = mmcTqToTrack[tq];
      track.init ();
    }
    return true;
  }

  //success = mmcCommandM ()
  //    (M tr size)
  //      alloc
  //      トラックを確保する
  //      tr    トラック番号。1～TRACKS
  //      size  トラックの容量
  //      ここでは何もしない
  protected boolean mmcCommandM () {
    //トラック番号
    int tr = mmcGetNumber (-1);
    if (tr == -1) {  //トラック番号が指定されていない
      mmcError = "line " + mmcLine + ": track number not specified";
      return false;
    }
    if (tr < 1 || TRACKS < tr) {  //トラック番号が範囲外
      mmcError = "line " + mmcLine + ": track number out of range";
      return false;
    }
    int tq = tr - 1;
    //サイズ
    int size = mmcGetNumber (',');
    if (size == -1) {  //サイズが指定されていない
      mmcError = "line " + mmcLine + ": size not specified";
      return false;
    }
    int c = mmcGetChar ();
    if (c != ')') {  //')'がない
      mmcError = "line " + mmcLine + ": ) not found";
      return false;
    }
    //何もしない
    return true;
  }

  //success = mmcCommandP ()
  //    (P)
  //      play
  //      演奏を開始する
  //      ここではMMLデータをコンパイルする
  protected boolean mmcCommandP () {
    mmcSkipSpace (-1);
    int c = mmcGetChar ();
    if (c != ')') {  //')'がない
      mmcError = "line " + mmcLine + ": ) not found";
      return false;
    }
    //コマンドを処理する
    for (;;) {
      //hasNext()のトラックの中からgetTimeUS()が最小のものを選ぶ
      int selectedTimeUS = Integer.MAX_VALUE;
      Track selectedTrack = null;
      for (int tq = 0; tq < TRACKS; tq++) {
        Track track = mmcTqToTrack[tq];
        if (track.hasNext ()) {
          int timeUS = track.getTimeUS ();
          if (timeUS < selectedTimeUS) {
            selectedTimeUS = timeUS;
            selectedTrack = track;
          }
        }
      }
      //なければ終了
      if (selectedTrack == null) {
        break;
      }
      //コマンドを処理する
      if (!selectedTrack.trkCommand ()) {
        return false;
      }
    }  //for
    //トラックを空にして時刻を合わせる
    int endTimeUS = 0;
    for (int tq = 0; tq < TRACKS; tq++) {
      Track track = mmcTqToTrack[tq];
      int timeUS = track.getTimeUS ();
      if (endTimeUS < timeUS) {
        endTimeUS = timeUS;
      }
    }
    endTimeUS += 1000000 * 2;  //2秒追加する
    for (int tq = 0; tq < TRACKS; tq++) {
      Track track = mmcTqToTrack[tq];
      track.flush ();
      track.setTimeUS (endTimeUS);
    }
    return true;
  }

  //success mmcCommandT ()
  //    (T ch) mml
  //      trk
  //      トラックにMMLデータを追加する
  //      ch   チャンネル番号。1～8
  //      mml  MMLデータ
  protected boolean mmcCommandT () {
    int tr = mmcGetNumber (-1);
    if (tr == -1) {  //トラック番号がない
      mmcError = "line " + mmcLine + ": track number not specified";
      return false;
    }
    if (tr < 1 || TRACKS < tr) {  //トラック番号が範囲外
      mmcError = "line " + mmcLine + ": track number out of range";
      return false;
    }
    int tq = tr - 1;
    mmcSkipSpace (-1);
    int c = mmcGetChar ();
    if (c == -1 || c != ')') {  //')'がない
      mmcError = "line " + mmcLine + ": ) not found";
      return false;
    }
    int start = mmcIndex;
    while (c != -1 && c != '(') {  //'('の手前まで
      c = mmcGetChar ();
    }
    mmcUngetChar (c);
    int end = mmcIndex;
    Track track = mmcTqToTrack[tq];
    track.add (mmcProgram.substring (start, end));
    return true;
  }

  //success = mmcCommandV ()
  //    (V n s v[s] ... v[54])
  //      vset
  //      音色を設定する
  //      n     音色番号
  //      s     設定開始インデックス
  //      v[i]  音色要素
  protected boolean mmcCommandV () {
    //音色番号
    int n = mmcGetNumber (-1);
    if (n == -1) {  //音色番号がない
      mmcError = "line " + mmcLine + ": tone number not specified";
      return false;
    }
    if (n < 1 || 200 < n) {  //音色番号が範囲外
      mmcError = "line " + mmcLine + ": tone number out of range";
      return false;
    }
    //開始位置
    int s = mmcGetNumber (',');
    if (s == -1) {  //開始位置がない
      mmcError = "line " + mmcLine + ": start position not specified";
      return false;
    }
    if (s < 0 || 54 <= s) {  //開始位置が範囲外
      mmcError = "line " + mmcLine + ": start position out of range";
      return false;
    }
    //音色要素
    for (int i = s; i < 55; i++) {
      int e = mmcGetNumber (',');
      if (e == -1) {  //音色要素がない
        mmcError = "line " + mmcLine + ": tone element not specified";
        return false;
      }
      if (e < 0 || (TONE_MASK[i] & 255) < e) {  //音色要素が範囲外
        mmcError = "line " + mmcLine + ": tone element out of range";
        return false;
      }
      mmcToneData[55 * (n - 1) + i] = (byte) e;
    }
    mmcSkipSpace (-1);
    int c = mmcGetChar ();
    if (c != ')') {  //')'がない
      mmcError = "line " + mmcLine + ": ) not found";
      return false;
    }
    return true;
  }



  //class Track
  //  トラック
  protected class Track {

    //new Track (tq)
    //  コンストラクタ
    //  tq  トラック番号-1
    public Track (int tq) {
      trkTq = tq;

      init ();
    }

    //init ()
    //  初期化する
    public void init () {
      trkTi = 55 * (1 - 1);  // @1
      trkLength = 48;  // L4
      trkOctave = 4;  // O4
      trkPan = 3;  // P3
      trkGate = 0;  // Q8
      trkVolume = trkVToAtV[8];  // V8
      trkKeyTranspose = 0;
      trkDetune = 0;

      flush ();
    }

    //flush ()
    //  トラックを空にする
    public void flush () {
      trkMML = new StringBuilder ();
      trkIndex = 0;
      trkTimeUS = 0;
      trkKeyOff = false;
      trkWaitUS = 0;
      trkTie = false;
    }

    //add (mml)
    //  MMLを追加する
    public void add (String mml) {
      trkMML.append (mml);
    }

    //yes = hasNext ()
    //  MMLの処理が残っているか
    public boolean hasNext () {
      return trkIndex < trkMML.length () || trkKeyOff;
    }

    //timeUS = getTimeUS ()
    //  時刻を返す
    public int getTimeUS () {
      return trkTimeUS;
    }

    //setTimeUS (timeUS)
    //  時刻を設定する
    public void setTimeUS (int timeUS) {
      trkTimeUS = timeUS;
    }



    protected int trkTq;  //トラック番号-1

    protected int trkTi;  //音色データのインデックス。55*(音色番号-1)
    protected int trkLength;  //絶対音長。4分音符の絶対音長は48
    protected int trkOctave;  //オクターブ。0～8。440HzのAを含むオクターブは4
    protected int trkPan;  //パン。1=左,2=右,3=左右
    protected int trkGate;  //ゲートタイム。0～8=削る割合,～-1=削る絶対音長。削ると残りがマイナスになる場合は削らない
    protected int trkVolume;  //絶対音量。0=無音
    protected int trkKeyTranspose;  //キートランスポーズ。半音単位でずらす
    protected int trkDetune;  //デチューン。半音の1/64単位でずらす

    protected StringBuilder trkMML;  //MML
    protected int trkIndex;  //MMLのインデックス
    protected int trkTimeUS;  //次のコマンドを処理する時刻(us)
    protected boolean trkKeyOff;  //コマンドを処理する前にキーオフする
    protected int trkWaitUS;  //ゲートから音長までの時間(us)
    protected boolean trkTie;  //キーオンしない

    //  次の文字を取り出す
    protected int trkGetChar () {
      return trkIndex < trkMML.length () ? trkMML.charAt (trkIndex++) : -1;
    }

    //  次の文字を取り出さなかったことにする
    protected void trkUngetChar (int c) {
      if (0 < trkIndex && c != -1) {
        trkIndex--;
      }
    }

    //  空白を読み飛ばす
    protected int trkSkipSpace () {
      int c = trkGetChar ();
      while (c != -1 && c <= ' ') {
        c = trkGetChar ();
      }
      trkUngetChar (c);
      return c;
    }

    //  数値を取り出す
    protected int trkGetNumber () {
      trkSkipSpace ();
      int n = -1;
      int c = trkGetChar ();
      if (c == '$') {  //16進数
        c = trkGetChar ();
        if (('0' <= c && c <= '9') ||
            ('A' <= c && c <= 'F') ||
            ('a' <= c && c <= 'f')) {
          n = 0;
          do {
            n = 16 * n + (c <= '9' ? c - '0' : (c | 0x20) - 'a' + 10);
            c = trkGetChar ();
          } while (('0' <= c && c <= '9') ||
                   ('A' <= c && c <= 'F') ||
                   ('a' <= c && c <= 'f'));
        }
      } else {  //10進数
        if ('0' <= c && c <= '9') {
          n = 0;
          do {
            n = 10 * n + (c - '0');
            c = trkGetChar ();
          } while ('0' <= c && c <= '9');
        }
      }
      trkUngetChar (c);
      return n;
    }

    //success = trkCommand ()
    //  コマンドを処理する
    protected boolean trkCommand () {
      //キーオフする
      if (trkKeyOff) {
        trkKeyOff = false;
        for (int cn = 0; cn < 8; cn++) {
          if (mmcCnToTq[cn] == trkTq) {
            trkSetData (0x08, cn);  //KON SLOT<<3|CH
          }
        }
        if (trkWaitUS != 0) {
          trkTimeUS += trkWaitUS;  //音長まで進む
          trkWaitUS = 0;
          return true;
        }
      }
      //コマンドを処理する
      int startTimeUS = trkTimeUS;
      boolean success = true;
      do {
        trkSkipSpace ();
        int c = trkGetChar ();
        if (c == -1) {
          return true;
        }
        if (c == 'A' || c == 'a') {
          success = trkCommandA (8);
        } else if (c == 'B' || c == 'b') {
          success = trkCommandA (10);
        } else if (c == 'C' || c == 'c') {
          success = trkCommandA (-1);
        } else if (c == 'D' || c == 'd') {
          success = trkCommandA (1);
        } else if (c == 'E' || c == 'e') {
          success = trkCommandA (3);
        } else if (c == 'F' || c == 'f') {
          success = trkCommandA (4);
        } else if (c == 'G' || c == 'g') {
          success = trkCommandA (6);
        } else if (c == 'K' || c == 'k') {
          success = trkCommandK ();
        } else if (c == 'O' || c == 'o') {
          success = trkCommandO ();
        } else if (c == 'P' || c == 'p') {
          success = trkCommandP ();
        } else if (c == 'Q' || c == 'q') {
          success = trkCommandQ ();
        } else if (c == 'R' || c == 'r') {
          success = trkCommandA (92);
        } else if (c == 'T' || c == 't') {
          success = trkCommandT ();
        } else if (c == 'V' || c == 'v') {
          success = trkCommandV ();
        } else if (c == 'Y' || c == 'y') {
          success = trkCommandY ();
        } else if (c == '@') {
          trkSkipSpace ();
          c = trkGetChar ();
          if (c == 'K' || c == 'k') {
            success = trkCommandAtK ();
          } else if (c == 'L' || c == 'l') {
            success = trkCommandAtL ();
          } else if (c == 'V' || c == 'v') {
            success = trkCommandAtV ();
          } else if (c == 'W' || c == 'w') {
            success = trkCommandA (92);
          } else {
            trkUngetChar (c);
            success = trkCommandAt ();
          }
        } else if (c == '<') {
          success = trkCommandLessThan ();
        } else if (c == '>') {
          success = trkCommandGreaterThan ();
        } else if (c == '{') {
          //success = trkCommandLeftCurlyBracket ();
        } else {
          trkUngetChar (c);
          success = trkSyntaxError ();
        }
      } while (success && startTimeUS == trkTimeUS);
      return success;
    }

    //  @n 音色
    protected boolean trkCommandAt () {
      // n
      int n = trkGetNumber ();
      if (n == -1) {
        return trkSyntaxError ();
      }
      trkTi = 55 * (n - 1);
      //[0]  FLCON
      //[9]  RLPAN
      trkPan = mmcToneData[trkTi + 9] & 3;
      trkSetDataAll (0x20, trkPan << 6 | (mmcToneData[trkTi + 0] & 63));  //RLPAN<<6|FL<<3|CON
      //[1]  SLOT
      //[2]  WAVE
      trkSetData (0x1b, mmcToneData[trkTi + 2] & 3);  //WAVE
      //[3]  SYNC
      //[4]  SPEED
      trkSetData (0x18, mmcToneData[trkTi + 4] & 255);  //SPEED
      //[5]  PMD
      trkSetData (0x19, 1 << 7 | (mmcToneData[trkTi + 5] & 127));  //PMD
      //[6]  AMD
      trkSetData (0x19, 0 << 7 | (mmcToneData[trkTi + 6] & 127));  //AMD
      //[7]  PMS
      //[8]  AMS
      trkSetDataAll (0x38, (mmcToneData[trkTi + 7] & 7) << 4 | (mmcToneData[trkTi + 8] & 3));  //PMS<<4|AMS
      //[10]
      //[0]  AR
      //[6]  KS
      trkSetDataAll (0x80, (mmcToneData[trkTi + 11 + 6] & 3) << 6 | (mmcToneData[trkTi + 11 + 0] & 31));  //M1 KS<<6|AR
      trkSetDataAll (0x88, (mmcToneData[trkTi + 33 + 6] & 3) << 6 | (mmcToneData[trkTi + 33 + 0] & 31));  //M2 KS<<6|AR
      trkSetDataAll (0x90, (mmcToneData[trkTi + 22 + 6] & 3) << 6 | (mmcToneData[trkTi + 22 + 0] & 31));  //C1 KS<<6|AR
      trkSetDataAll (0x98, (mmcToneData[trkTi + 44 + 6] & 3) << 6 | (mmcToneData[trkTi + 44 + 0] & 31));  //C2 KS<<6|AR
      //[1]  D1R
      //[10] AMSEN
      trkSetDataAll (0xa0, (mmcToneData[trkTi + 11 + 10] & 1) << 7 | (mmcToneData[trkTi + 11 + 1] & 31));  //M1 AMSEN<<7|D1R
      trkSetDataAll (0xa8, (mmcToneData[trkTi + 33 + 10] & 1) << 7 | (mmcToneData[trkTi + 33 + 1] & 31));  //M2 AMSEN<<7|D1R
      trkSetDataAll (0xb0, (mmcToneData[trkTi + 22 + 10] & 1) << 7 | (mmcToneData[trkTi + 22 + 1] & 31));  //C1 AMSEN<<7|D1R
      trkSetDataAll (0xb8, (mmcToneData[trkTi + 44 + 10] & 1) << 7 | (mmcToneData[trkTi + 44 + 1] & 31));  //C2 AMSEN<<7|D1R
      //[2]  D2R
      //[9]  DT2
      trkSetDataAll (0xc0, (mmcToneData[trkTi + 11 + 9] & 3) << 6 | (mmcToneData[trkTi + 11 + 2] & 31));  //M1 DT2<<6|D2R
      trkSetDataAll (0xc8, (mmcToneData[trkTi + 33 + 9] & 3) << 6 | (mmcToneData[trkTi + 33 + 2] & 31));  //M2 DT2<<6|D2R
      trkSetDataAll (0xd0, (mmcToneData[trkTi + 22 + 9] & 3) << 6 | (mmcToneData[trkTi + 22 + 2] & 31));  //C1 DT2<<6|D2R
      trkSetDataAll (0xd8, (mmcToneData[trkTi + 44 + 9] & 3) << 6 | (mmcToneData[trkTi + 44 + 2] & 31));  //C2 DT2<<6|D2R
      //[3]  RR
      //[4]  D1L
      trkSetDataAll (0xe0, (mmcToneData[trkTi + 11 + 4] & 15) << 4 | (mmcToneData[trkTi + 11 + 3] & 15));  //M1 D1L<<4|RR
      trkSetDataAll (0xe8, (mmcToneData[trkTi + 33 + 4] & 15) << 4 | (mmcToneData[trkTi + 33 + 3] & 15));  //M2 D1L<<4|RR
      trkSetDataAll (0xf0, (mmcToneData[trkTi + 22 + 4] & 15) << 4 | (mmcToneData[trkTi + 22 + 3] & 15));  //C1 D1L<<4|RR
      trkSetDataAll (0xf8, (mmcToneData[trkTi + 44 + 4] & 15) << 4 | (mmcToneData[trkTi + 44 + 3] & 15));  //C2 D1L<<4|RR
      //[5]  TL
      //  M1はCON=7のとき出力スロットになる
      //  M2はCON=5,6,7のとき出力スロットになる
      //  C1はCON=4,5,6,7のとき出力スロットになる
      //  C2はCON=0,1,2,3,4,5,6,7のとき出力スロットになる
      //  CON=0 ┌┐
      //        └M1─C1─M2─C2→
      //  CON=1 ┌┐  C1─┐
      //        └M1───M2─C2→
      //  CON=2 ┌┐  C1─M2─┐
      //        └M1─────C2→
      //  CON=3 ┌┐      M2─┐
      //        └M1─C1───C2→
      //  CON=4 ┌┐      M2─C2→
      //        └M1─C1────→
      //  CON=5 ┌┐┌──M2──→
      //        └M1┼C1────→
      //            └────C2→
      //  CON=6 ┌┐      M2──→
      //        └M1─C1────→
      //                      C2→
      //  CON=7       C1────→
      //        ┌┐      M2──→
      //        └M1──────→
      //                      C2→
      int con = mmcToneData[trkTi + 0] & 7;
      trkSetDataAll (0x60, Math.min (127, (mmcToneData[trkTi + 11 + 5] & 127) + (con < 7 ? 0 : 127 - trkVolume)));  //M1 TL
      trkSetDataAll (0x68, Math.min (127, (mmcToneData[trkTi + 33 + 5] & 127) + (con < 5 ? 0 : 127 - trkVolume)));  //M2 TL
      trkSetDataAll (0x70, Math.min (127, (mmcToneData[trkTi + 22 + 5] & 127) + (con < 4 ? 0 : 127 - trkVolume)));  //C1 TL
      trkSetDataAll (0x78, Math.min (127, (mmcToneData[trkTi + 44 + 5] & 127) + (              127 - trkVolume)));  //C2 TL
      //[7]  MUL
      //[8]  DT1
      trkSetDataAll (0x40, (mmcToneData[trkTi + 11 + 8] & 7) << 4 | (mmcToneData[trkTi + 11 + 7] & 15));  //M1 DT1<<4|MUL
      trkSetDataAll (0x48, (mmcToneData[trkTi + 33 + 8] & 7) << 4 | (mmcToneData[trkTi + 33 + 7] & 15));  //M2 DT1<<4|MUL
      trkSetDataAll (0x50, (mmcToneData[trkTi + 22 + 8] & 7) << 4 | (mmcToneData[trkTi + 22 + 7] & 15));  //C1 DT1<<4|MUL
      trkSetDataAll (0x58, (mmcToneData[trkTi + 44 + 8] & 7) << 4 | (mmcToneData[trkTi + 44 + 7] & 15));  //C2 DT1<<4|MUL
      return true;
    }  //trkCommandAt

    //  An～Gn,Rn,@Wn 音符と休符
    //  note12  ノート。92=休符
    @SuppressWarnings ("fallthrough") protected boolean trkCommandA (int note12) {
      // #,+,- シャープとフラット
      int sharp = 0;
      if (note12 != 92) {  //休符ではない
        trkSkipSpace ();
        int c = trkGetChar ();
        while (c == '#' || c == '+' || c == '-') {
          sharp += c == '-' ? -1 : 1;
          c = trkGetChar ();
        }
        trkUngetChar (c);
      }
      //音長
      int length = trkLength;
      trkSkipSpace ();
      int c = trkGetChar ();
      if (c == '*') {  //絶対音長
        // n 絶対音長
        int n = trkGetNumber ();
        if (n == -1) {
          return trkSyntaxError ();
        }
        length = n;
      } else {  //絶対音長ではない
        trkUngetChar (c);
        // n 音長
        int n = trkGetNumber ();
        if (n == -1) {
        } else {
          if (!(1 <= n && n <= 64)) {
            return trkSyntaxError ();
          } else {
            length = 192 / n;
          }
        }
      }
      // . 符点
      int half = length >> 1;
      trkSkipSpace ();
      c = trkGetChar ();
      while (c == '.') {
        length += half;
        half >>= 1;
        c = trkGetChar ();
      }
      trkUngetChar (c);
      //キーコードを求める
      //  範囲外のとき休符にする
      int kc = 0;
      int kf = 0;
      if (note12 != 92) {  //休符ではない
        kf = 64 * (12 * trkOctave + note12 + sharp + trkKeyTranspose) - 123 + trkDetune;
        kc = kf >> 6;
        kf &= 63;
        kc += kc / 3;
        if (kc < 0 || 128 <= kc) {  //範囲外
          note12 = 92;  //休符にする
        }
      }
      if (note12 != 92) {  //休符ではない
        //SYNC
        if ((mmcToneData[trkTi + 3] & 1) != 0) {
          trkSetData (0x01, 1 << 1);  //LFORESET=1
          trkSetData (0x01, 0 << 1);  //LFORESET=0
        }
        //PAN
        trkSetDataAll (0x20, trkPan << 6 | (mmcToneData[trkTi + 0] & 63));  //RLPAN<<6|FL<<3|CON
        //キーコード
        trkSetDataAll (0x28, kc);  //KC
        trkSetDataAll (0x30, kf << 2);  //KF
        //TL
        //  M1はCON=7のとき出力スロットになる
        //  M2はCON=5,6,7のとき出力スロットになる
        //  C1はCON=4,5,6,7のとき出力スロットになる
        //  C2はCON=0,1,2,3,4,5,6,7のとき出力スロットになる
        switch (mmcToneData[trkTi + 0] & 7) {  //CON
        case 7:
          trkSetDataAll (0x60, Math.min (127, (mmcToneData[trkTi + 11 + 5] & 127) + (127 - trkVolume)));  //M1 TL
          //fallthrough
        case 6:
        case 5:
          trkSetDataAll (0x68, Math.min (127, (mmcToneData[trkTi + 33 + 5] & 127) + (127 - trkVolume)));  //M2 TL
          //fallthrough
        case 4:
          trkSetDataAll (0x70, Math.min (127, (mmcToneData[trkTi + 22 + 5] & 127) + (127 - trkVolume)));  //C1 TL
          //fallthrough
        case 3:
        case 2:
        case 1:
        case 0:
          trkSetDataAll (0x78, Math.min (127, (mmcToneData[trkTi + 44 + 5] & 127) + (127 - trkVolume)));  //C2 TL
        }
        if (!trkTie) {  //タイまたはスラーではない
          for (int cn = 0; cn < 8; cn++) {
            if (mmcCnToTq[cn] == trkTq) {
              //キーオン
              trkSetData (0x08, ((mmcToneData[trkTi + 1] & 15) << 3) + cn);  //KON SLOT<<3|CH
            }
          }
        }
      }
      // & タイまたはスラー
      trkTie = false;
      trkSkipSpace ();
      c = trkGetChar ();
      if (c == '&') {
        trkTie = true;
        c = trkGetChar ();
      }
      trkUngetChar (c);
      if (note12 == 92 || trkTie) {  //休符またはタイまたはスラー
        trkTimeUS += (int) Math.round (mmcTempo * (double) length);  //音長まで進む
        trkKeyOff = false;  //キーオフしない
        trkWaitUS = 0;
      } else {  //タイまたはスラーではない
        int gateLength = length - (trkGate < 0 ? -trkGate : (length * trkGate) >> 3);
        if (gateLength < 0) {
          gateLength = length;
        }
        trkTimeUS += (int) Math.round (mmcTempo * (double) gateLength);  //ゲートまで進む
        trkKeyOff = true;  //キーオフする
        trkWaitUS = (int) Math.round (mmcTempo * (double) (length - gateLength));  //ゲートから音長までの時間(us)
      }
      return true;
    }  //trkCommandA

    //  Ln 音長
    protected boolean trkCommandL () {
      //音長
      int length = trkLength;
      trkSkipSpace ();
      int c = trkGetChar ();
      if (c == '*') {  //絶対音長
        // n 絶対音長
        int n = trkGetNumber ();
        if (n == -1) {
          return trkSyntaxError ();
        }
        length = n;
      } else {  //絶対音長ではない
        trkUngetChar (c);
        // n 音長
        int n = trkGetNumber ();
        if (n == -1) {
          return trkSyntaxError ();
        } else {
          if (!(1 <= n && n <= 64)) {
            return trkSyntaxError ();
          } else {
            length = 192 / n;
          }
        }
      }
      // . 符点
      int half = length >> 1;
      trkSkipSpace ();
      c = trkGetChar ();
      while (c == '.') {
        length += half;
        half >>= 1;
        c = trkGetChar ();
      }
      trkUngetChar (c);
      trkLength = length;
      return true;
    }

    //  Kn キートランスポーズ
    protected boolean trkCommandK () {
      //符号
      int sign = 1;
      int c = trkGetChar ();
      if (c == '+') {
      } else if (c == '-') {
        sign = -1;
      } else {
        trkUngetChar (c);
      }
      //絶対値
      int abs = trkGetNumber ();
      if (abs == -1) {
        return trkSyntaxError ();
      }
      trkKeyTranspose = sign * abs;
      return true;
    }

    //  @Kn デチューン
    protected boolean trkCommandAtK () {
      //符号
      int sign = 1;
      int c = trkGetChar ();
      if (c == '+') {
      } else if (c == '-') {
        sign = -1;
      } else {
        trkUngetChar (c);
      }
      //絶対値
      int abs = trkGetNumber ();
      if (abs == -1) {
        return trkSyntaxError ();
      }
      trkDetune = sign * abs;
      return true;
    }

    //  @Ln 絶対音長
    protected boolean trkCommandAtL () {
      // n 音長
      int n = trkGetNumber ();
      if (n == -1) {
        return trkSyntaxError ();
      }
      trkLength = n;
      return true;
    }

    //  On オクターブ
    protected boolean trkCommandO () {
      // n オクターブ
      int n = trkGetNumber ();
      if (n == -1) {
        return trkSyntaxError ();
      }
      if (0 <= n && n <= 8) {
        trkOctave = n;
      } else {
        return trkSyntaxError ();
      }
      return true;
    }

    //  < オクターブを1つ上げる
    protected boolean trkCommandLessThan () {
      int n = trkOctave + 1;
      if (!(0 <= n && n <= 8)) {
        return trkSyntaxError ();
      }
      trkOctave = n;
      return true;
    }

    //  > オクターブを1つ下げる
    protected boolean trkCommandGreaterThan () {
      int n = trkOctave - 1;
      if (!(0 <= n && n <= 8)) {
        return trkSyntaxError ();
      }
      trkOctave = n;
      return true;
    }

    //  Pn パン
    protected boolean trkCommandP () {
      // n パン
      int n = trkGetNumber ();
      if (n == -1 || !(0 <= n && n <= 3)) {
        return trkSyntaxError ();
      }
      trkPan = n;
      return true;
    }

    //  Qn ゲートタイム
    protected boolean trkCommandQ () {
      // n ゲートタイム
      int n = trkGetNumber ();
      if (n == -1 || !(0 <= n && n <= 8)) {
        return trkSyntaxError ();
      }
      trkGate = 8 - n;
      return true;
    }

    //  @Qn 絶対ゲートタイム
    protected boolean trkCommandAtQ () {
      // n ゲートタイム
      int n = trkGetNumber ();
      if (n == -1) {
        return trkSyntaxError ();
      }
      trkGate = -n;
      return true;
    }

    //  Tn テンポ
    protected boolean trkCommandT () {
      // n テンポ
      int n = trkGetNumber ();
      if (n == -1 || !(1 <= n && n <= 10000)) {
        return trkSyntaxError ();
      }
      mmcTempo = (60.0 / (double) n) / 48.0 * 1000000.0;
      return true;
    }

    //  V→@V変換テーブル
    protected static final int[] trkVToAtV = new int[] {
      127 - 127,  //0
      127 - 40,  //1
      127 - 37,  //2
      127 - 34,  //3
      127 - 32,  //4
      127 - 29,  //5
      127 - 26,  //6
      127 - 24,  //7
      127 - 21,  //8
      127 - 18,  //9
      127 - 16,  //10
      127 - 13,  //11
      127 - 10,  //12
      127 - 8,  //13
      127 - 5,  //14
      127 - 2,  //15
      127 - 0,  //16
    };

    //  Vn 音量
    protected boolean trkCommandV () {
      // n 音量
      int n = trkGetNumber ();
      if (n == -1 || !(0 <= n && n <= 16)) {
        return trkSyntaxError ();
      }
      trkVolume = trkVToAtV[n];
      return true;
    }

    //  @Vn 絶対音量
    protected boolean trkCommandAtV () {
      // n 音量
      int n = trkGetNumber ();
      if (n == -1 || !(0 <= n && n <= 127)) {
        return trkSyntaxError ();
      }
      trkVolume = n;
      return true;
    }

    //  Ya,d レジスタ設定
    protected boolean trkCommandY () {
      // a アドレス
      int a = trkGetNumber ();
      if (a == -1 || !(0 <= a && a <= 255)) {
        return trkSyntaxError ();
      }
      trkSkipSpace ();
      int c = trkGetChar ();
      if (c != ',') {
        return trkSyntaxError ();
      }
      // d データ
      int d = trkGetNumber ();
      if (d == -1 || !(0 <= d && d <= 255)) {
        return trkSyntaxError ();
      }
      trkSetData (a, d);
      return true;
    }

    //  {A～G}n 連符
    //protected boolean trkCommandLeftCurlyBracket () {
    //  return trkSyntaxError ();  //未対応
    //}

    //  MMLの文法エラー
    protected boolean trkSyntaxError () {
      int i0 = Math.max (0, trkIndex - 20);
      int i1 = Math.min (trkMML.length (), trkIndex + 20);
      StringBuilder sb = new StringBuilder ();
      sb.append ("track ");
      sb.append (trkTq + 1);
      sb.append (" syntax error at ");
      sb.append (trkIndex);
      sb.append ("\n");
      for (int i = i0; i < i1; i++) {
        int c = trkMML.charAt (i);
        if (c < ' ') {
          c = ' ';
        }
        if (i == trkIndex) {
          sb.append ('[');
        }
        sb.append ((char) c);
        if (i == trkIndex) {
          sb.append (']');
        }
      }
      mmcError = sb.toString ();
      return false;
    }

    //trkSetDataAll (address, data)
    //  トラックに対応するすべてのチャンネルのレジスタへ書き込む
    protected void trkSetDataAll (int address, int data) {
      for (int cn = 0; cn < 8; cn++) {
        if (mmcCnToTq[cn] == trkTq) {
          trkSetData (address + cn, data);
        }
      }
    }

    //trkSetData (address, data)
    //  レジスタへ書き込む
    protected void trkSetData (int address, int data) {
      mmcOutput.add (trkTimeUS);
      mmcOutput.add (address << 8 | data);
    }

  }  //class Track



  //TONE_DATA_68
  //  X68000の音色(68SND.ZMS)
/*
  public static final byte[] TONE_DATA_68 = {
    //1:Acoustic Piano,アコースティックピアノ
    58, 15, 2, 0, 220, 0, 0, 0, 0, 3, 0,
    28, 4, 0, 5, 1, 37, 2, 1, 7, 0, 0,
    22, 9, 1, 2, 1, 47, 2, 12, 0, 0, 0,
    29, 4, 3, 6, 1, 37, 1, 3, 3, 0, 0,
    15, 7, 0, 5, 10, 0, 2, 1, 0, 0, 1,
    //2:Honky Tonk Piano,ホンキートンクピアノ
    28, 15, 2, 0, 222, 30, 10, 0, 0, 3, 0,
    31, 10, 1, 3, 15, 29, 0, 7, 3, 0, 0,
    29, 12, 9, 7, 10, 0, 0, 7, 7, 0, 1,
    31, 5, 1, 3, 15, 39, 2, 5, 3, 1, 0,
    28, 12, 9, 7, 10, 0, 0, 7, 3, 0, 1,
    //3:Electric Piano,エレクトリックピアノ
    28, 15, 2, 0, 180, 0, 0, 0, 0, 3, 0,
    31, 15, 0, 6, 7, 53, 2, 15, 5, 1, 0,
    31, 7, 5, 8, 2, 13, 3, 1, 0, 0, 1,
    31, 6, 0, 6, 4, 37, 2, 1, 2, 0, 0,
    31, 7, 0, 7, 0, 0, 1, 1, 7, 0, 1,
    //4:Clavinet,クラビネット
    58, 15, 2, 0, 130, 0, 0, 0, 0, 3, 0,
    28, 4, 3, 7, 1, 35, 2, 1, 3, 0, 0,
    27, 8, 1, 2, 0, 37, 3, 15, 7, 0, 0,
    28, 3, 0, 0, 15, 27, 2, 1, 6, 0, 0,
    26, 9, 0, 10, 15, 0, 2, 10, 0, 0, 1,
    //5:Celesta,セレスタ
    13, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 10, 12, 5, 15, 72, 1, 14, 2, 0, 1,
    31, 10, 12, 5, 15, 7, 1, 4, 7, 0, 1,
    31, 10, 12, 7, 15, 7, 1, 12, 7, 0, 1,
    31, 10, 12, 6, 15, 7, 1, 9, 3, 0, 1,
    //6:Cembalo,チェンバロ
    50, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 15, 0, 25, 1, 3, 0, 0, 0,
    31, 0, 0, 15, 0, 35, 3, 12, 4, 0, 1,
    31, 0, 0, 2, 0, 36, 1, 1, 0, 0, 0,
    31, 6, 4, 5, 15, 0, 2, 1, 4, 0, 1,
    //7:Acoustic Guitar,アコースティックギター
    33, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    28, 5, 4, 3, 15, 42, 3, 2, 1, 0, 0,
    31, 7, 4, 1, 2, 37, 1, 3, 7, 0, 0,
    31, 3, 4, 1, 2, 35, 3, 3, 4, 0, 0,
    31, 2, 1, 4, 1, 0, 2, 1, 2, 0, 0,
    //8:Electric Guitar,エレキギター
    58, 15, 2, 0, 210, 0, 0, 0, 0, 3, 0,
    31, 13, 1, 4, 15, 41, 2, 15, 3, 0, 0,
    31, 20, 5, 15, 14, 57, 1, 13, 7, 2, 0,
    20, 10, 1, 7, 8, 35, 1, 3, 7, 0, 0,
    23, 5, 1, 7, 15, 0, 0, 1, 3, 0, 1,
    //9:Wood Bass,ウッドベース
    58, 15, 2, 0, 150, 0, 0, 0, 0, 3, 0,
    31, 13, 1, 4, 15, 32, 1, 0, 7, 0, 0,
    31, 11, 1, 10, 15, 55, 1, 4, 5, 0, 0,
    31, 11, 1, 10, 15, 29, 0, 0, 2, 0, 0,
    31, 11, 1, 8, 15, 0, 1, 0, 3, 0, 1,
    //10:Electric Bass,エレキベース
    3, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 14, 1, 10, 10, 42, 0, 6, 6, 0, 0,
    31, 5, 0, 10, 6, 26, 0, 0, 4, 0, 0,
    31, 2, 4, 6, 1, 32, 0, 0, 4, 0, 0,
    28, 1, 6, 8, 1, 0, 0, 1, 3, 0, 1,
    //11:Banjo,バンジョー
    58, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    24, 10, 0, 2, 5, 27, 1, 5, 7, 0, 0,
    26, 16, 0, 8, 11, 30, 0, 15, 0, 0, 0,
    28, 16, 0, 4, 3, 32, 0, 1, 6, 0, 0,
    24, 11, 0, 6, 15, 0, 2, 1, 3, 0, 0,
    //12:Sitar,シタール
    1, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 31, 8, 2, 12, 52, 0, 3, 7, 0, 0,
    31, 11, 1, 3, 1, 35, 1, 9, 3, 0, 0,
    28, 7, 9, 4, 15, 17, 0, 1, 1, 0, 0,
    18, 1, 1, 4, 15, 0, 1, 1, 0, 0, 1,
    //13:Harp,ハープ
    58, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 25, 1, 2, 2, 76, 0, 6, 0, 0, 0,
    31, 16, 1, 2, 13, 26, 1, 3, 7, 0, 0,
    31, 4, 2, 2, 12, 37, 1, 1, 0, 0, 0,
    31, 10, 0, 3, 15, 0, 1, 1, 0, 0, 1,
    //14:Koto,琴
    56, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    26, 8, 5, 7, 2, 28, 3, 3, 7, 0, 0,
    29, 4, 5, 5, 1, 31, 3, 4, 1, 0, 0,
    28, 4, 2, 6, 2, 32, 3, 1, 7, 0, 0,
    29, 9, 3, 3, 1, 0, 3, 1, 3, 0, 1,
    //15:Pipe Organ 1,パイプオルガン 1
    62, 15, 2, 0, 2, 8, 1, 3, 2, 3, 0,
    31, 20, 0, 10, 0, 36, 0, 8, 3, 0, 0,
    20, 2, 1, 10, 3, 0, 0, 2, 7, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 1, 1, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 6, 2, 0, 1,
    //16:Pipe Organ 2,パイプオルガン 2
    63, 15, 2, 0, 190, 0, 0, 0, 0, 3, 0,
    31, 1, 1, 10, 0, 29, 0, 8, 3, 0, 1,
    19, 2, 1, 10, 1, 2, 0, 3, 7, 0, 1,
    19, 2, 1, 10, 1, 2, 0, 1, 0, 0, 1,
    19, 2, 1, 10, 1, 2, 0, 2, 6, 0, 1,
    //17:Electric Organ,エレクトリックオルガン
    31, 15, 2, 0, 200, 3, 2, 2, 1, 3, 0,
    31, 20, 0, 15, 15, 12, 0, 7, 0, 0, 1,
    31, 2, 1, 15, 0, 5, 0, 3, 2, 0, 1,
    31, 2, 1, 15, 0, 7, 0, 3, 0, 0, 1,
    31, 2, 1, 15, 0, 5, 0, 2, 6, 0, 1,
    //18:Accordion,アコーディオン
    56, 15, 2, 0, 180, 30, 0, 2, 0, 3, 0,
    31, 0, 0, 0, 0, 39, 1, 6, 3, 0, 0,
    31, 3, 1, 1, 1, 38, 1, 7, 3, 0, 1,
    19, 2, 1, 6, 1, 38, 1, 1, 7, 0, 0,
    16, 0, 0, 9, 0, 0, 1, 2, 7, 0, 1,
    //19:Violin,バイオリン
    58, 15, 2, 0, 202, 56, 3, 3, 0, 3, 0,
    20, 2, 0, 5, 1, 33, 1, 1, 0, 0, 0,
    25, 6, 0, 8, 3, 30, 1, 5, 7, 0, 0,
    28, 3, 0, 6, 1, 48, 1, 1, 0, 0, 0,
    12, 4, 0, 6, 0, 0, 1, 1, 4, 0, 1,
    //20:Cello,チェロ
    56, 15, 2, 0, 200, 80, 0, 2, 0, 3, 0,
    18, 31, 20, 10, 0, 10, 1, 15, 7, 3, 0,
    31, 17, 12, 10, 0, 35, 1, 6, 7, 0, 0,
    13, 18, 1, 3, 0, 27, 2, 1, 7, 0, 0,
    12, 2, 1, 10, 1, 0, 1, 1, 3, 0, 1,
    //21:Strings 1,ストリングス 1
    58, 15, 2, 0, 205, 80, 0, 2, 0, 3, 0,
    30, 1, 0, 1, 1, 30, 3, 0, 2, 0, 0,
    31, 1, 0, 2, 1, 38, 3, 2, 3, 0, 0,
    30, 1, 0, 1, 1, 48, 1, 1, 3, 0, 0,
    8, 2, 0, 6, 0, 0, 0, 1, 4, 0, 1,
    //22:Strings 2,ストリングス 2
    61, 15, 2, 0, 200, 90, 0, 2, 0, 3, 0,
    31, 1, 1, 2, 0, 31, 3, 0, 0, 0, 0,
    9, 1, 0, 6, 0, 0, 0, 1, 1, 0, 0,
    10, 1, 0, 7, 0, 0, 0, 1, 1, 0, 0,
    9, 2, 0, 7, 0, 0, 0, 1, 1, 0, 1,
    //23:Pizzicato,ピチカート
    60, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 22, 1, 3, 15, 24, 0, 1, 3, 0, 0,
    18, 15, 1, 5, 14, 0, 1, 1, 7, 0, 1,
    31, 15, 0, 3, 15, 32, 1, 1, 3, 0, 0,
    31, 15, 1, 5, 14, 0, 1, 1, 3, 0, 1,
    //24:Voice,ボイス
    6, 15, 2, 0, 200, 90, 0, 4, 0, 3, 0,
    10, 0, 1, 3, 0, 77, 0, 1, 0, 0, 0,
    12, 0, 0, 5, 0, 7, 2, 3, 3, 0, 1,
    12, 0, 1, 6, 2, 0, 1, 2, 7, 0, 1,
    18, 0, 0, 6, 0, 17, 1, 1, 3, 0, 1,
    //25:Chorus,コーラス
    41, 15, 2, 0, 206, 40, 0, 4, 0, 3, 0,
    19, 18, 4, 4, 5, 66, 0, 6, 3, 3, 0,
    21, 14, 6, 10, 6, 52, 0, 4, 7, 3, 0,
    11, 31, 3, 10, 0, 45, 0, 1, 7, 0, 0,
    14, 31, 1, 8, 0, 0, 0, 1, 3, 0, 1,
    //26:Glassharp,グラスハープ
    36, 15, 0, 0, 80, 1, 2, 1, 1, 3, 0,
    20, 2, 1, 5, 3, 36, 1, 4, 0, 0, 1,
    6, 7, 7, 6, 0, 0, 0, 0, 0, 1, 1,
    20, 2, 1, 5, 3, 37, 3, 4, 6, 0, 0,
    7, 7, 7, 7, 0, 0, 0, 0, 2, 1, 1,
    //27:Whistle,ホイッスル
    7, 15, 2, 0, 200, 70, 0, 4, 0, 3, 0,
    0, 0, 0, 0, 0, 127, 0, 0, 4, 0, 0,
    0, 0, 0, 0, 0, 127, 0, 0, 4, 0, 0,
    15, 12, 0, 9, 0, 0, 0, 5, 7, 2, 0,
    13, 12, 0, 9, 0, 0, 0, 8, 7, 0, 1,
    //28:Piccolo,ピッコロ
    4, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 10, 1, 10, 3, 47, 1, 2, 4, 0, 0,
    19, 11, 3, 9, 2, 0, 0, 2, 4, 0, 0,
    18, 10, 1, 10, 5, 77, 1, 6, 4, 3, 0,
    19, 11, 3, 9, 2, 0, 0, 2, 4, 0, 0,
    //29:Flute,フルート
    59, 15, 2, 0, 196, 16, 0, 5, 0, 3, 0,
    28, 5, 3, 5, 14, 42, 3, 2, 7, 1, 0,
    11, 7, 0, 5, 15, 51, 1, 2, 0, 0, 0,
    14, 2, 0, 4, 2, 48, 3, 1, 3, 0, 0,
    12, 16, 0, 6, 1, 0, 2, 1, 0, 0, 1,
    //30:Oboe,オーボエ
    58, 15, 2, 0, 198, 30, 8, 4, 1, 3, 0,
    25, 11, 0, 3, 1, 37, 3, 1, 3, 0, 0,
    28, 12, 12, 11, 5, 37, 3, 9, 3, 0, 0,
    25, 16, 0, 11, 1, 47, 1, 2, 3, 0, 0,
    17, 10, 0, 11, 1, 0, 1, 4, 3, 0, 1,
    //31:Clarinet,クラリネット
    58, 15, 2, 0, 198, 11, 0, 4, 0, 3, 0,
    19, 2, 2, 0, 1, 36, 1, 2, 0, 0, 0,
    28, 18, 3, 11, 4, 32, 0, 9, 0, 0, 0,
    29, 20, 1, 9, 1, 55, 1, 1, 0, 0, 0,
    17, 15, 0, 9, 0, 0, 0, 1, 0, 0, 1,
    //32:Bassoon,バスーン
    44, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    18, 0, 0, 10, 0, 47, 0, 1, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 1, 2, 4, 0, 0,
    19, 14, 0, 10, 1, 39, 0, 1, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 0, 5, 4, 0, 0,
    //33:Saxophone,サクソフォン
    58, 15, 2, 0, 200, 40, 0, 3, 0, 3, 0,
    18, 0, 0, 6, 0, 36, 0, 0, 0, 0, 0,
    18, 0, 0, 6, 3, 47, 0, 4, 0, 1, 0,
    18, 0, 0, 6, 0, 42, 0, 0, 0, 0, 0,
    14, 8, 0, 8, 1, 0, 0, 1, 7, 0, 1,
    //34:Trumpet,トランペット
    58, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    14, 14, 0, 3, 1, 27, 2, 1, 3, 0, 0,
    14, 14, 0, 3, 15, 37, 2, 7, 2, 0, 0,
    13, 14, 0, 3, 1, 37, 2, 1, 4, 0, 0,
    19, 3, 0, 10, 0, 0, 1, 1, 6, 0, 1,
    //35:Horn,ホルン
    58, 15, 2, 0, 205, 0, 0, 0, 0, 3, 0,
    13, 9, 0, 9, 3, 34, 0, 1, 4, 0, 0,
    31, 17, 0, 15, 12, 45, 1, 5, 4, 2, 0,
    12, 11, 0, 8, 1, 50, 0, 1, 4, 0, 0,
    14, 31, 0, 10, 0, 1, 0, 1, 4, 0, 1,
    //36:Trombone,トロンボーン
    58, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    16, 12, 0, 8, 0, 28, 0, 1, 0, 0, 0,
    14, 14, 0, 10, 15, 40, 0, 2, 0, 2, 0,
    20, 14, 0, 10, 7, 49, 0, 1, 0, 0, 0,
    16, 14, 0, 8, 1, 0, 0, 1, 0, 0, 1,
    //37:Tuba,チューバ
    54, 15, 2, 0, 203, 2, 2, 1, 1, 3, 0,
    15, 10, 1, 5, 6, 21, 1, 0, 1, 0, 0,
    17, 2, 1, 8, 3, 0, 0, 1, 3, 0, 1,
    30, 2, 18, 10, 5, 0, 2, 1, 7, 0, 1,
    15, 2, 1, 10, 5, 0, 2, 0, 3, 0, 1,
    //38:Brass 1,ブラス 1
    60, 15, 0, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 12, 1, 10, 2, 32, 1, 1, 0, 0, 0,
    18, 10, 1, 10, 3, 0, 0, 1, 1, 0, 1,
    15, 10, 1, 10, 5, 19, 1, 1, 2, 0, 0,
    20, 2, 1, 10, 3, 7, 0, 1, 6, 0, 1,
    //39:Brass 2,ブラス 2
    58, 15, 2, 0, 206, 40, 0, 3, 0, 3, 0,
    16, 15, 0, 8, 1, 24, 0, 1, 7, 0, 0,
    16, 12, 0, 4, 1, 59, 0, 8, 0, 2, 0,
    18, 0, 0, 4, 0, 51, 0, 1, 0, 0, 0,
    16, 0, 0, 10, 0, 0, 0, 2, 0, 0, 1,
    //40:Harmonica,ハーモニカ
    56, 15, 2, 0, 210, 1, 5, 3, 1, 3, 0,
    18, 0, 0, 3, 0, 38, 0, 9, 3, 0, 0,
    18, 0, 0, 3, 0, 38, 0, 7, 7, 0, 0,
    15, 5, 0, 3, 1, 37, 0, 1, 3, 0, 0,
    15, 8, 0, 9, 2, 0, 0, 3, 7, 0, 1,
    //41:Ocarina,オカリナ
    59, 15, 2, 0, 204, 20, 0, 5, 0, 3, 0,
    31, 16, 0, 10, 15, 12, 0, 4, 0, 0, 0,
    24, 10, 0, 10, 0, 77, 0, 2, 0, 0, 0,
    20, 20, 0, 10, 3, 77, 0, 3, 7, 1, 0,
    16, 5, 0, 10, 7, 0, 0, 4, 0, 0, 1,
    //42:Recorder,リコーダー
    59, 15, 2, 0, 196, 18, 0, 5, 0, 3, 0,
    17, 17, 16, 6, 3, 55, 0, 4, 4, 1, 0,
    15, 18, 1, 0, 2, 47, 0, 2, 4, 0, 0,
    13, 20, 0, 7, 2, 47, 0, 2, 7, 0, 0,
    16, 31, 0, 9, 0, 0, 0, 1, 4, 0, 1,
    //43:Apito,サンバホイッスル
    2, 15, 2, 0, 244, 47, 0, 7, 0, 3, 0,
    31, 0, 0, 10, 0, 47, 0, 6, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    31, 0, 0, 10, 0, 43, 0, 10, 0, 0, 0,
    20, 8, 0, 10, 1, 0, 0, 2, 0, 0, 1,
    //44:Pan Flute,パンフルート
    59, 15, 2, 0, 200, 80, 0, 3, 0, 3, 0,
    20, 0, 0, 10, 0, 0, 0, 4, 0, 0, 0,
    14, 16, 0, 10, 5, 62, 0, 2, 3, 0, 0,
    18, 18, 0, 10, 9, 38, 0, 3, 0, 1, 0,
    14, 12, 0, 10, 2, 0, 1, 1, 0, 0, 1,
    //45:Snare Drum,スネアドラム
    60, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 25, 5, 2, 0, 0, 0, 15, 0, 0, 0,
    31, 18, 18, 12, 7, 0, 0, 1, 0, 0, 1,
    31, 25, 0, 0, 15, 0, 0, 3, 0, 1, 0,
    31, 17, 15, 10, 15, 0, 0, 1, 0, 0, 1,
    //46:Rim Shot,リムショット
    2, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    30, 16, 1, 10, 15, 43, 0, 2, 0, 3, 0,
    30, 10, 0, 10, 15, 47, 0, 0, 7, 1, 0,
    30, 20, 0, 10, 15, 15, 0, 0, 3, 3, 0,
    30, 19, 0, 10, 15, 0, 0, 1, 0, 0, 1,
    //47:Bass Drum,バスドラム
    0, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    30, 26, 0, 13, 15, 26, 0, 1, 0, 1, 0,
    30, 28, 0, 14, 15, 37, 0, 14, 0, 3, 0,
    30, 16, 0, 8, 15, 5, 0, 0, 0, 1, 0,
    29, 16, 0, 8, 15, 0, 0, 0, 0, 0, 1,
    //48:Tam-Tam,タムタム
    59, 15, 2, 0, 110, 0, 0, 0, 0, 3, 0,
    28, 20, 12, 15, 10, 22, 0, 3, 0, 2, 0,
    28, 19, 5, 2, 10, 17, 3, 1, 0, 1, 0,
    28, 15, 10, 10, 5, 17, 3, 0, 3, 0, 0,
    30, 12, 7, 5, 6, 0, 1, 1, 0, 0, 1,
    //49:Timpani,ティンパニ
    2, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    28, 12, 0, 4, 15, 36, 1, 0, 0, 1, 0,
    20, 8, 0, 4, 15, 27, 1, 0, 0, 2, 0,
    28, 10, 0, 5, 15, 34, 0, 0, 0, 0, 0,
    16, 5, 0, 2, 15, 0, 3, 0, 0, 0, 1,
    //50:Bongo,ボンゴ
    59, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    24, 23, 0, 11, 15, 0, 0, 3, 0, 3, 0,
    26, 14, 0, 7, 15, 40, 0, 2, 0, 2, 0,
    26, 10, 0, 5, 15, 57, 0, 2, 0, 3, 0,
    22, 16, 0, 8, 15, 0, 2, 6, 0, 0, 1,
    //51:Timbales,ティンバレス
    50, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    28, 15, 0, 6, 15, 26, 1, 2, 3, 3, 0,
    24, 16, 0, 7, 15, 32, 0, 8, 7, 2, 0,
    26, 11, 0, 7, 15, 29, 1, 5, 3, 0, 0,
    24, 7, 0, 4, 15, 0, 2, 2, 7, 3, 1,
    //52:Triangle,トライアングル
    3, 15, 0, 0, 100, 0, 0, 0, 0, 3, 0,
    31, 6, 0, 4, 15, 51, 0, 1, 0, 3, 0,
    31, 0, 0, 2, 0, 27, 0, 8, 7, 2, 0,
    31, 8, 0, 6, 5, 67, 0, 9, 3, 1, 0,
    31, 10, 0, 5, 15, 0, 0, 10, 3, 2, 1,
    //53:Cow Bell,カウベル
    59, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    30, 20, 0, 10, 15, 27, 0, 15, 2, 0, 0,
    30, 17, 0, 8, 15, 27, 1, 4, 0, 1, 0,
    28, 12, 0, 6, 15, 43, 1, 2, 3, 2, 0,
    26, 16, 0, 8, 15, 0, 1, 2, 0, 3, 1,
    //54:Tubular Bells,チューブラーベル
    4, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 7, 0, 1, 1, 35, 0, 7, 3, 0, 0,
    31, 10, 0, 6, 0, 0, 0, 2, 7, 0, 1,
    31, 7, 0, 1, 1, 35, 0, 7, 7, 0, 0,
    31, 13, 0, 6, 0, 0, 0, 2, 3, 0, 1,
    //55:Steel Drum,スチールドラム
    4, 15, 2, 0, 208, 0, 0, 0, 0, 3, 0,
    13, 10, 4, 4, 15, 29, 1, 3, 7, 0, 0,
    17, 7, 0, 4, 15, 0, 2, 1, 0, 0, 1,
    14, 8, 5, 3, 15, 35, 2, 1, 7, 0, 0,
    15, 8, 0, 4, 15, 7, 2, 4, 3, 0, 1,
    //56:Glockenspiel,グロッケンシュピール
    28, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 24, 0, 12, 15, 32, 0, 14, 2, 0, 0,
    31, 15, 0, 8, 15, 0, 0, 2, 0, 0, 0,
    31, 20, 0, 4, 15, 27, 0, 15, 0, 0, 0,
    31, 14, 0, 5, 15, 0, 0, 2, 0, 0, 1,
    //57:Vibraphone,ビブラフォン
    44, 15, 2, 0, 197, 40, 13, 2, 3, 3, 0,
    24, 14, 0, 7, 15, 50, 1, 12, 3, 0, 0,
    24, 10, 0, 7, 15, 0, 1, 4, 0, 0, 1,
    26, 14, 0, 6, 15, 57, 1, 4, 0, 0, 0,
    26, 8, 0, 6, 15, 0, 2, 1, 0, 0, 1,
    //58:Marimba,マリンバ
    44, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    24, 17, 0, 7, 15, 42, 1, 4, 3, 0, 0,
    24, 4, 0, 2, 15, 0, 3, 0, 3, 0, 1,
    24, 20, 0, 10, 15, 32, 1, 6, 7, 0, 0,
    24, 12, 0, 6, 15, 0, 2, 2, 7, 0, 1,
    //59:Closed Hi-Hat,クローズハイハット
    59, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    29, 4, 3, 2, 3, 0, 0, 14, 0, 1, 0,
    29, 15, 3, 2, 7, 27, 0, 6, 0, 1, 0,
    29, 23, 0, 10, 15, 27, 0, 7, 0, 2, 0,
    30, 20, 21, 15, 15, 0, 0, 1, 0, 0, 1,
    //60:Open Hi-Hat,オープンハイハット
    52, 15, 2, 0, 111, 0, 0, 0, 0, 3, 0,
    31, 1, 0, 5, 12, 7, 0, 0, 0, 3, 0,
    31, 13, 15, 10, 15, 14, 0, 14, 0, 1, 0,
    31, 22, 7, 8, 6, 19, 0, 0, 7, 0, 0,
    31, 20, 20, 8, 15, 2, 0, 0, 0, 1, 0,
    //61:Cymbal,シンバル
    44, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 4, 0, 0, 1, 4, 0, 3, 7, 1, 0,
    31, 31, 3, 2, 1, 29, 1, 5, 0, 2, 0,
    25, 28, 5, 3, 3, 7, 0, 1, 7, 2, 0,
    31, 31, 5, 3, 7, 0, 2, 7, 0, 3, 1,
    //62:Synthesizer 1,シンセサイザ 1
    26, 15, 2, 0, 200, 20, 3, 3, 2, 3, 0,
    18, 1, 1, 10, 3, 17, 1, 2, 1, 0, 0,
    20, 2, 1, 10, 0, 12, 0, 3, 0, 1, 0,
    31, 19, 1, 0, 15, 25, 0, 0, 0, 0, 0,
    20, 2, 1, 10, 3, 0, 2, 1, 3, 0, 1,
    //63:Synthesizer 2,シンセサイザ 2
    28, 3, 2, 0, 210, 40, 0, 3, 0, 3, 0,
    31, 16, 0, 0, 15, 7, 1, 2, 3, 0, 0,
    31, 0, 0, 8, 0, 7, 1, 1, 6, 0, 1,
    31, 0, 0, 8, 0, 12, 1, 2, 7, 0, 0,
    31, 0, 0, 8, 0, 0, 1, 1, 3, 0, 1,
    //64:Ambulance,救急車
    4, 15, 1, 0, 158, 68, 0, 6, 0, 3, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 1,
    31, 0, 0, 1, 0, 37, 0, 14, 0, 0, 0,
    16, 0, 0, 4, 0, 0, 0, 5, 7, 1, 1,
    //65:Storm,嵐
    58, 15, 2, 0, 120, 120, 30, 7, 2, 3, 0,
    31, 0, 0, 0, 0, 17, 0, 2, 0, 2, 0,
    31, 0, 0, 0, 0, 10, 0, 1, 0, 1, 0,
    31, 0, 0, 0, 0, 29, 0, 1, 0, 2, 0,
    12, 0, 0, 4, 0, 0, 0, 0, 0, 0, 1,
    //66:Laser Gun,レーザーガン
    4, 15, 0, 0, 220, 120, 0, 7, 0, 3, 0,
    31, 0, 0, 5, 0, 15, 0, 0, 0, 3, 0,
    20, 0, 0, 10, 0, 7, 0, 7, 0, 1, 1,
    12, 0, 0, 5, 0, 47, 0, 3, 0, 3, 0,
    16, 0, 0, 8, 0, 0, 0, 1, 0, 0, 1,
    //67:Game Sound Effect 1,ゲーム効果音 1
    6, 15, 3, 0, 209, 70, 0, 6, 0, 3, 0,
    31, 0, 0, 0, 0, 25, 0, 12, 0, 0, 0,
    20, 14, 0, 7, 15, 7, 0, 4, 0, 0, 0,
    20, 14, 0, 7, 15, 0, 0, 2, 4, 3, 0,
    20, 14, 0, 7, 15, 0, 0, 2, 4, 0, 0,
    //68:Game Sound Effect 2,ゲーム効果音 2
    32, 15, 0, 0, 0, 0, 0, 0, 0, 3, 0,
    31, 8, 0, 4, 15, 13, 0, 3, 0, 2, 0,
    10, 7, 0, 4, 15, 17, 3, 1, 0, 1, 0,
    31, 0, 0, 0, 0, 3, 0, 1, 0, 2, 0,
    16, 9, 0, 4, 15, 0, 3, 0, 0, 0, 1,
  };
*/
  //perl ../misc/itob.pl MMLCompiler.java TONE_DATA_68
  public static final byte[] TONE_DATA_68 = ":\17\2\0\334\0\0\0\0\3\0\34\4\0\5\1%\2\1\7\0\0\26\t\1\2\1/\2\f\0\0\0\35\4\3\6\1%\1\3\3\0\0\17\7\0\5\n\0\2\1\0\0\1\34\17\2\0\336\36\n\0\0\3\0\37\n\1\3\17\35\0\7\3\0\0\35\f\t\7\n\0\0\7\7\0\1\37\5\1\3\17\'\2\5\3\1\0\34\f\t\7\n\0\0\7\3\0\1\34\17\2\0\264\0\0\0\0\3\0\37\17\0\6\0075\2\17\5\1\0\37\7\5\b\2\r\3\1\0\0\1\37\6\0\6\4%\2\1\2\0\0\37\7\0\7\0\0\1\1\7\0\1:\17\2\0\202\0\0\0\0\3\0\34\4\3\7\1#\2\1\3\0\0\33\b\1\2\0%\3\17\7\0\0\34\3\0\0\17\33\2\1\6\0\0\32\t\0\n\17\0\2\n\0\0\1\r\17\0\0\0\0\0\0\0\3\0\37\n\f\5\17H\1\16\2\0\1\37\n\f\5\17\7\1\4\7\0\1\37\n\f\7\17\7\1\f\7\0\1\37\n\f\6\17\7\1\t\3\0\0012\17\0\0\0\0\0\0\0\3\0\37\0\0\17\0\31\1\3\0\0\0\37\0\0\17\0#\3\f\4\0\1\37\0\0\2\0$\1\1\0\0\0\37\6\4\5\17\0\2\1\4\0\1!\17\0\0\0\0\0\0\0\3\0\34\5\4\3\17*\3\2\1\0\0\37\7\4\1\2%\1\3\7\0\0\37\3\4\1\2#\3\3\4\0\0\37\2\1\4\1\0\2\1\2\0\0:\17\2\0\322\0\0\0\0\3\0\37\r\1\4\17)\2\17\3\0\0\37\24\5\17\169\1\r\7\2\0\24\n\1\7\b#\1\3\7\0\0\27\5\1\7\17\0\0\1\3\0\1:\17\2\0\226\0\0\0\0\3\0\37\r\1\4\17 \1\0\7\0\0\37\13\1\n\0177\1\4\5\0\0\37\13\1\n\17\35\0\0\2\0\0\37\13\1\b\17\0\1\0\3\0\1\3\17\0\0\0\0\0\0\0\3\0\37\16\1\n\n*\0\6\6\0\0\37\5\0\n\6\32\0\0\4\0\0\37\2\4\6\1 \0\0\4\0\0\34\1\6\b\1\0\0\1\3\0\1:\17\0\0\0\0\0\0\0\3\0\30\n\0\2\5\33\1\5\7\0\0\32\20\0\b\13\36\0\17\0\0\0\34\20\0\4\3 \0\1\6\0\0\30\13\0\6\17\0\2\1\3\0\0\1\17\2\0\310\0\0\0\0\3\0\37\37\b\2\f4\0\3\7\0\0\37\13\1\3\1#\1\t\3\0\0\34\7\t\4\17\21\0\1\1\0\0\22\1\1\4\17\0\1\1\0\0\1:\17\0\0\0\0\0\0\0\3\0\37\31\1\2\2L\0\6\0\0\0\37\20\1\2\r\32\1\3\7\0\0\37\4\2\2\f%\1\1\0\0\0\37\n\0\3\17\0\1\1\0\0\18\17\2\0\310\0\0\0\0\3\0\32\b\5\7\2\34\3\3\7\0\0\35\4\5\5\1\37\3\4\1\0\0\34\4\2\6\2 \3\1\7\0\0\35\t\3\3\1\0\3\1\3\0\1>\17\2\0\2\b\1\3\2\3\0\37\24\0\n\0$\0\b\3\0\0\24\2\1\n\3\0\0\2\7\0\1\24\2\1\n\3\0\0\1\1\0\1\24\2\1\n\3\0\0\6\2\0\1?\17\2\0\276\0\0\0\0\3\0\37\1\1\n\0\35\0\b\3\0\1\23\2\1\n\1\2\0\3\7\0\1\23\2\1\n\1\2\0\1\0\0\1\23\2\1\n\1\2\0\2\6\0\1\37\17\2\0\310\3\2\2\1\3\0\37\24\0\17\17\f\0\7\0\0\1\37\2\1\17\0\5\0\3\2\0\1\37\2\1\17\0\7\0\3\0\0\1\37\2\1\17\0\5\0\2\6\0\18\17\2\0\264\36\0\2\0\3\0\37\0\0\0\0\'\1\6\3\0\0\37\3\1\1\1&\1\7\3\0\1\23\2\1\6\1&\1\1\7\0\0\20\0\0\t\0\0\1\2\7\0\1:\17\2\0\3128\3\3\0\3\0\24\2\0\5\1!\1\1\0\0\0\31\6\0\b\3\36\1\5\7\0\0\34\3\0\6\0010\1\1\0\0\0\f\4\0\6\0\0\1\1\4\0\18\17\2\0\310P\0\2\0\3\0\22\37\24\n\0\n\1\17\7\3\0\37\21\f\n\0#\1\6\7\0\0\r\22\1\3\0\33\2\1\7\0\0\f\2\1\n\1\0\1\1\3\0\1:\17\2\0\315P\0\2\0\3\0\36\1\0\1\1\36\3\0\2\0\0\37\1\0\2\1&\3\2\3\0\0\36\1\0\1\0010\1\1\3\0\0\b\2\0\6\0\0\0\1\4\0\1=\17\2\0\310Z\0\2\0\3\0\37\1\1\2\0\37\3\0\0\0\0\t\1\0\6\0\0\0\1\1\0\0\n\1\0\7\0\0\0\1\1\0\0\t\2\0\7\0\0\0\1\1\0\1<\17\0\0\0\0\0\0\0\3\0\37\26\1\3\17\30\0\1\3\0\0\22\17\1\5\16\0\1\1\7\0\1\37\17\0\3\17 \1\1\3\0\0\37\17\1\5\16\0\1\1\3\0\1\6\17\2\0\310Z\0\4\0\3\0\n\0\1\3\0M\0\1\0\0\0\f\0\0\5\0\7\2\3\3\0\1\f\0\1\6\2\0\1\2\7\0\1\22\0\0\6\0\21\1\1\3\0\1)\17\2\0\316(\0\4\0\3\0\23\22\4\4\5B\0\6\3\3\0\25\16\6\n\0064\0\4\7\3\0\13\37\3\n\0-\0\1\7\0\0\16\37\1\b\0\0\0\1\3\0\1$\17\0\0P\1\2\1\1\3\0\24\2\1\5\3$\1\4\0\0\1\6\7\7\6\0\0\0\0\0\1\1\24\2\1\5\3%\3\4\6\0\0\7\7\7\7\0\0\0\0\2\1\1\7\17\2\0\310F\0\4\0\3\0\0\0\0\0\0\177\0\0\4\0\0\0\0\0\0\0\177\0\0\4\0\0\17\f\0\t\0\0\0\5\7\2\0\r\f\0\t\0\0\0\b\7\0\1\4\17\2\0\310\0\0\0\0\3\0\22\n\1\n\3/\1\2\4\0\0\23\13\3\t\2\0\0\2\4\0\0\22\n\1\n\5M\1\6\4\3\0\23\13\3\t\2\0\0\2\4\0\0;\17\2\0\304\20\0\5\0\3\0\34\5\3\5\16*\3\2\7\1\0\13\7\0\5\0173\1\2\0\0\0\16\2\0\4\0020\3\1\3\0\0\f\20\0\6\1\0\2\1\0\0\1:\17\2\0\306\36\b\4\1\3\0\31\13\0\3\1%\3\1\3\0\0\34\f\f\13\5%\3\t\3\0\0\31\20\0\13\1/\1\2\3\0\0\21\n\0\13\1\0\1\4\3\0\1:\17\2\0\306\13\0\4\0\3\0\23\2\2\0\1$\1\2\0\0\0\34\22\3\13\4 \0\t\0\0\0\35\24\1\t\0017\1\1\0\0\0\21\17\0\t\0\0\0\1\0\0\1,\17\0\0\0\0\0\0\0\3\0\22\0\0\n\0/\0\1\4\0\0\24\0\0\n\0\0\1\2\4\0\0\23\16\0\n\1\'\0\1\4\0\0\24\0\0\n\0\0\0\5\4\0\0:\17\2\0\310(\0\3\0\3\0\22\0\0\6\0$\0\0\0\0\0\22\0\0\6\3/\0\4\0\1\0\22\0\0\6\0*\0\0\0\0\0\16\b\0\b\1\0\0\1\7\0\1:\17\0\0\0\0\0\0\0\3\0\16\16\0\3\1\33\2\1\3\0\0\16\16\0\3\17%\2\7\2\0\0\r\16\0\3\1%\2\1\4\0\0\23\3\0\n\0\0\1\1\6\0\1:\17\2\0\315\0\0\0\0\3\0\r\t\0\t\3\"\0\1\4\0\0\37\21\0\17\f-\1\5\4\2\0\f\13\0\b\0012\0\1\4\0\0\16\37\0\n\0\1\0\1\4\0\1:\17\0\0\0\0\0\0\0\3\0\20\f\0\b\0\34\0\1\0\0\0\16\16\0\n\17(\0\2\0\2\0\24\16\0\n\0071\0\1\0\0\0\20\16\0\b\1\0\0\1\0\0\0016\17\2\0\313\2\2\1\1\3\0\17\n\1\5\6\25\1\0\1\0\0\21\2\1\b\3\0\0\1\3\0\1\36\2\22\n\5\0\2\1\7\0\1\17\2\1\n\5\0\2\0\3\0\1<\17\0\0\310\0\0\0\0\3\0\22\f\1\n\2 \1\1\0\0\0\22\n\1\n\3\0\0\1\1\0\1\17\n\1\n\5\23\1\1\2\0\0\24\2\1\n\3\7\0\1\6\0\1:\17\2\0\316(\0\3\0\3\0\20\17\0\b\1\30\0\1\7\0\0\20\f\0\4\1;\0\b\0\2\0\22\0\0\4\0003\0\1\0\0\0\20\0\0\n\0\0\0\2\0\0\18\17\2\0\322\1\5\3\1\3\0\22\0\0\3\0&\0\t\3\0\0\22\0\0\3\0&\0\7\7\0\0\17\5\0\3\1%\0\1\3\0\0\17\b\0\t\2\0\0\3\7\0\1;\17\2\0\314\24\0\5\0\3\0\37\20\0\n\17\f\0\4\0\0\0\30\n\0\n\0M\0\2\0\0\0\24\24\0\n\3M\0\3\7\1\0\20\5\0\n\7\0\0\4\0\0\1;\17\2\0\304\22\0\5\0\3\0\21\21\20\6\0037\0\4\4\1\0\17\22\1\0\2/\0\2\4\0\0\r\24\0\7\2/\0\2\7\0\0\20\37\0\t\0\0\0\1\4\0\1\2\17\2\0\364/\0\7\0\3\0\37\0\0\n\0/\0\6\0\0\0\0\0\0\0\17\177\0\1\0\0\0\37\0\0\n\0+\0\n\0\0\0\24\b\0\n\1\0\0\2\0\0\1;\17\2\0\310P\0\3\0\3\0\24\0\0\n\0\0\0\4\0\0\0\16\20\0\n\5>\0\2\3\0\0\22\22\0\n\t&\0\3\0\1\0\16\f\0\n\2\0\1\1\0\0\1<\17\0\0\0\0\0\0\0\3\0\37\31\5\2\0\0\0\17\0\0\0\37\22\22\f\7\0\0\1\0\0\1\37\31\0\0\17\0\0\3\0\1\0\37\21\17\n\17\0\0\1\0\0\1\2\17\0\0\0\0\0\0\0\3\0\36\20\1\n\17+\0\2\0\3\0\36\n\0\n\17/\0\0\7\1\0\36\24\0\n\17\17\0\0\3\3\0\36\23\0\n\17\0\0\1\0\0\1\0\17\0\0\0\0\0\0\0\3\0\36\32\0\r\17\32\0\1\0\1\0\36\34\0\16\17%\0\16\0\3\0\36\20\0\b\17\5\0\0\0\1\0\35\20\0\b\17\0\0\0\0\0\1;\17\2\0n\0\0\0\0\3\0\34\24\f\17\n\26\0\3\0\2\0\34\23\5\2\n\21\3\1\0\1\0\34\17\n\n\5\21\3\0\3\0\0\36\f\7\5\6\0\1\1\0\0\1\2\17\0\0\0\0\0\0\0\3\0\34\f\0\4\17$\1\0\0\1\0\24\b\0\4\17\33\1\0\0\2\0\34\n\0\5\17\"\0\0\0\0\0\20\5\0\2\17\0\3\0\0\0\1;\17\0\0\0\0\0\0\0\3\0\30\27\0\13\17\0\0\3\0\3\0\32\16\0\7\17(\0\2\0\2\0\32\n\0\5\179\0\2\0\3\0\26\20\0\b\17\0\2\6\0\0\0012\17\0\0\0\0\0\0\0\3\0\34\17\0\6\17\32\1\2\3\3\0\30\20\0\7\17 \0\b\7\2\0\32\13\0\7\17\35\1\5\3\0\0\30\7\0\4\17\0\2\2\7\3\1\3\17\0\0d\0\0\0\0\3\0\37\6\0\4\0173\0\1\0\3\0\37\0\0\2\0\33\0\b\7\2\0\37\b\0\6\5C\0\t\3\1\0\37\n\0\5\17\0\0\n\3\2\1;\17\0\0\0\0\0\0\0\3\0\36\24\0\n\17\33\0\17\2\0\0\36\21\0\b\17\33\1\4\0\1\0\34\f\0\6\17+\1\2\3\2\0\32\20\0\b\17\0\1\2\0\3\1\4\17\0\0\0\0\0\0\0\3\0\37\7\0\1\1#\0\7\3\0\0\37\n\0\6\0\0\0\2\7\0\1\37\7\0\1\1#\0\7\7\0\0\37\r\0\6\0\0\0\2\3\0\1\4\17\2\0\320\0\0\0\0\3\0\r\n\4\4\17\35\1\3\7\0\0\21\7\0\4\17\0\2\1\0\0\1\16\b\5\3\17#\2\1\7\0\0\17\b\0\4\17\7\2\4\3\0\1\34\17\0\0\0\0\0\0\0\3\0\37\30\0\f\17 \0\16\2\0\0\37\17\0\b\17\0\0\2\0\0\0\37\24\0\4\17\33\0\17\0\0\0\37\16\0\5\17\0\0\2\0\0\1,\17\2\0\305(\r\2\3\3\0\30\16\0\7\0172\1\f\3\0\0\30\n\0\7\17\0\1\4\0\0\1\32\16\0\6\179\1\4\0\0\0\32\b\0\6\17\0\2\1\0\0\1,\17\0\0\0\0\0\0\0\3\0\30\21\0\7\17*\1\4\3\0\0\30\4\0\2\17\0\3\0\3\0\1\30\24\0\n\17 \1\6\7\0\0\30\f\0\6\17\0\2\2\7\0\1;\17\0\0\0\0\0\0\0\3\0\35\4\3\2\3\0\0\16\0\1\0\35\17\3\2\7\33\0\6\0\1\0\35\27\0\n\17\33\0\7\0\2\0\36\24\25\17\17\0\0\1\0\0\0014\17\2\0o\0\0\0\0\3\0\37\1\0\5\f\7\0\0\0\3\0\37\r\17\n\17\16\0\16\0\1\0\37\26\7\b\6\23\0\0\7\0\0\37\24\24\b\17\2\0\0\0\1\0,\17\2\0\310\0\0\0\0\3\0\37\4\0\0\1\4\0\3\7\1\0\37\37\3\2\1\35\1\5\0\2\0\31\34\5\3\3\7\0\1\7\2\0\37\37\5\3\7\0\2\7\0\3\1\32\17\2\0\310\24\3\3\2\3\0\22\1\1\n\3\21\1\2\1\0\0\24\2\1\n\0\f\0\3\0\1\0\37\23\1\0\17\31\0\0\0\0\0\24\2\1\n\3\0\2\1\3\0\1\34\3\2\0\322(\0\3\0\3\0\37\20\0\0\17\7\1\2\3\0\0\37\0\0\b\0\7\1\1\6\0\1\37\0\0\b\0\f\1\2\7\0\0\37\0\0\b\0\0\1\1\3\0\1\4\17\1\0\236D\0\6\0\3\0\0\0\0\0\17\177\0\1\0\0\0\0\0\0\0\17\177\0\1\0\0\1\37\0\0\1\0%\0\16\0\0\0\20\0\0\4\0\0\0\5\7\1\1:\17\2\0xx\36\7\2\3\0\37\0\0\0\0\21\0\2\0\2\0\37\0\0\0\0\n\0\1\0\1\0\37\0\0\0\0\35\0\1\0\2\0\f\0\0\4\0\0\0\0\0\0\1\4\17\0\0\334x\0\7\0\3\0\37\0\0\5\0\17\0\0\0\3\0\24\0\0\n\0\7\0\7\0\1\1\f\0\0\5\0/\0\3\0\3\0\20\0\0\b\0\0\0\1\0\0\1\6\17\3\0\321F\0\6\0\3\0\37\0\0\0\0\31\0\f\0\0\0\24\16\0\7\17\7\0\4\0\0\0\24\16\0\7\17\0\0\2\4\3\0\24\16\0\7\17\0\0\2\4\0\0 \17\0\0\0\0\0\0\0\3\0\37\b\0\4\17\r\0\3\0\2\0\n\7\0\4\17\21\3\1\0\1\0\37\0\0\0\0\3\0\1\0\2\0\20\t\0\4\17\0\3\0\0\0\1".getBytes (XEiJ.ISO_8859_1);

  //TONE_DATA_X1
  //  X1の音色(VIP.ZMS)
/*
  public static final byte[] TONE_DATA_X1 = {
    //1:Acoustic Piano 1,アコースティックピアノ 1
    58, 15, 2, 1, 220, 0, 4, 1, 1, 3, 0,
    31, 5, 7, 4, 9, 37, 1, 1, 5, 0, 0,
    22, 0, 4, 5, 4, 62, 1, 5, 2, 0, 0,
    29, 0, 4, 5, 4, 77, 1, 1, 7, 0, 0,
    31, 7, 6, 5, 4, 0, 2, 1, 1, 0, 1,
    //2:Acoustic Piano 2,アコースティックピアノ 2
    28, 15, 2, 0, 180, 0, 1, 0, 1, 3, 0,
    31, 20, 8, 10, 0, 24, 0, 1, 3, 0, 0,
    31, 10, 5, 10, 0, 0, 0, 1, 7, 0, 1,
    31, 20, 8, 10, 0, 45, 0, 3, 7, 0, 0,
    25, 10, 5, 10, 0, 0, 3, 1, 3, 0, 1,
    //3:Acoustic Piano 3,アコースティックピアノ 3
    58, 15, 2, 0, 205, 0, 0, 0, 0, 2, 0,
    19, 2, 1, 4, 3, 33, 3, 5, 4, 0, 0,
    19, 2, 1, 4, 3, 25, 3, 5, 2, 0, 0,
    19, 2, 1, 4, 3, 31, 2, 1, 7, 0, 0,
    19, 2, 1, 4, 3, 0, 3, 1, 4, 0, 1,
    //4:Honky Tonk Piano,ホンキートンクピアノ
    28, 15, 2, 0, 220, 0, 10, 0, 0, 3, 0,
    31, 10, 1, 3, 15, 24, 2, 7, 3, 0, 0,
    29, 12, 9, 7, 10, 0, 0, 7, 7, 0, 1,
    31, 5, 1, 3, 15, 35, 2, 5, 7, 1, 0,
    28, 12, 9, 7, 10, 0, 0, 7, 3, 0, 1,
    //5:Electric Piano 1,エレクトリックピアノ 1
    44, 15, 2, 0, 180, 10, 2, 5, 3, 3, 0,
    25, 20, 0, 6, 7, 67, 2, 10, 3, 1, 0,
    24, 10, 5, 8, 2, 0, 2, 1, 2, 0, 1,
    26, 7, 3, 6, 4, 47, 3, 10, 0, 0, 0,
    24, 12, 5, 8, 2, 0, 1, 1, 0, 0, 1,
    //6:Electric Piano 2,エレクトリックピアノ 2
    28, 15, 2, 0, 200, 2, 2, 2, 1, 3, 0,
    31, 10, 0, 10, 5, 47, 0, 15, 3, 3, 0,
    27, 8, 4, 6, 11, 57, 2, 5, 0, 0, 1,
    30, 6, 11, 6, 15, 33, 2, 1, 3, 0, 0,
    30, 6, 11, 6, 15, 0, 1, 1, 3, 0, 1,
    //7:Electric Piano 3,エレクトリックピアノ 3
    60, 15, 2, 1, 190, 0, 2, 0, 3, 3, 0,
    31, 10, 0, 2, 15, 57, 2, 7, 3, 1, 0,
    31, 10, 5, 5, 2, 27, 2, 1, 2, 0, 1,
    31, 7, 3, 4, 4, 47, 3, 10, 7, 0, 0,
    31, 12, 5, 6, 1, 0, 1, 1, 3, 0, 1,
    //8:Electric Piano 4,エレクトリックピアノ 4
    58, 15, 2, 0, 189, 5, 5, 4, 1, 3, 0,
    28, 4, 3, 7, 1, 38, 2, 1, 3, 0, 0,
    27, 9, 1, 2, 0, 57, 3, 7, 7, 3, 0,
    28, 4, 3, 6, 0, 45, 2, 5, 6, 0, 0,
    26, 2, 0, 5, 15, 0, 3, 2, 3, 0, 1,
    //9:Toy Piano,トイピアノ
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 2, 20, 10, 0, 17, 1, 0, 7, 0, 0,
    31, 10, 2, 3, 0, 27, 2, 2, 3, 0, 1,
    31, 2, 15, 10, 0, 32, 1, 12, 7, 0, 0,
    31, 10, 13, 5, 5, 0, 1, 2, 3, 0, 1,
    //10:Clavinet 1,クラビネット 1
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    28, 4, 3, 7, 1, 35, 2, 1, 3, 0, 0,
    27, 9, 1, 2, 0, 37, 3, 15, 7, 0, 0,
    28, 3, 0, 0, 15, 27, 2, 1, 6, 0, 0,
    26, 6, 0, 10, 15, 0, 3, 10, 0, 0, 1,
    //11:Clavinet 2,クラビネット 2
    58, 15, 2, 0, 130, 10, 0, 3, 3, 3, 0,
    28, 4, 3, 7, 1, 47, 2, 8, 3, 0, 0,
    27, 5, 5, 2, 3, 47, 3, 15, 7, 0, 0,
    31, 5, 5, 0, 15, 17, 2, 2, 6, 0, 0,
    26, 7, 2, 10, 15, 0, 3, 10, 0, 0, 1,
    //12:Clavinet 3,クラビネット 3
    60, 15, 2, 0, 130, 10, 0, 3, 3, 3, 0,
    28, 4, 3, 7, 1, 32, 2, 2, 3, 0, 0,
    27, 5, 5, 10, 3, 0, 3, 15, 7, 0, 1,
    31, 2, 0, 0, 15, 17, 2, 1, 6, 0, 0,
    26, 5, 5, 10, 15, 0, 3, 10, 3, 0, 1,
    //13:Celesta 1,セレスタ 1
    13, 15, 2, 0, 200, 0, 0, 0, 0, 1, 0,
    31, 10, 12, 7, 15, 110, 1, 14, 6, 0, 0,
    31, 10, 12, 7, 15, 32, 1, 4, 6, 0, 0,
    31, 10, 12, 7, 15, 32, 1, 12, 6, 0, 0,
    31, 10, 12, 7, 15, 32, 1, 9, 6, 0, 0,
    //14:Celesta 2,セレスタ 2
    63, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 10, 12, 6, 15, 52, 1, 1, 0, 0, 1,
    31, 8, 12, 6, 15, 37, 1, 0, 0, 0, 1,
    31, 10, 12, 6, 15, 27, 1, 4, 0, 0, 1,
    31, 10, 12, 6, 15, 47, 1, 2, 0, 0, 1,
    //15:Cembalo 1,チェンバロ 1
    4, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 4, 0, 2, 0, 7, 3, 4, 5, 0, 0,
    31, 8, 1, 8, 15, 0, 1, 2, 0, 0, 1,
    31, 4, 0, 2, 0, 6, 0, 3, 5, 0, 0,
    31, 8, 1, 8, 15, 0, 0, 1, 0, 0, 1,
    //16:Cembalo 2,チェンバロ 2
    36, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    0, 0, 0, 0, 0, 127, 0, 0, 4, 0, 0,
    0, 0, 0, 0, 0, 127, 0, 0, 0, 0, 0,
    31, 4, 0, 2, 0, 3, 3, 3, 7, 0, 0,
    31, 13, 12, 8, 15, 0, 0, 1, 0, 0, 0,
    //17:Acoustic Guitar 1,アコースティックギター 1
    2, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    24, 10, 0, 5, 15, 57, 1, 12, 1, 0, 0,
    20, 12, 8, 4, 1, 37, 1, 6, 7, 0, 0,
    29, 10, 4, 4, 1, 37, 1, 3, 4, 0, 0,
    18, 18, 6, 7, 1, 0, 2, 1, 2, 0, 0,
    //18:Acoustic Guitar 2,アコースティックギター 2
    58, 15, 2, 1, 180, 3, 0, 5, 0, 3, 0,
    31, 10, 1, 2, 3, 37, 1, 1, 2, 0, 0,
    31, 10, 31, 3, 10, 32, 1, 14, 1, 1, 0,
    31, 10, 10, 3, 5, 87, 0, 3, 1, 0, 0,
    31, 18, 12, 7, 6, 0, 0, 1, 7, 0, 1,
    //19:Flamenco Guitar,フラメンコギター
    57, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 22, 8, 6, 7, 11, 2, 12, 6, 0, 0,
    31, 6, 0, 6, 3, 33, 1, 3, 3, 0, 0,
    28, 6, 0, 6, 15, 32, 0, 3, 4, 0, 0,
    31, 8, 0, 8, 15, 0, 0, 1, 4, 0, 0,
    //20:Twelve-String Guitar,12 弦ギター
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    29, 8, 0, 6, 15, 27, 1, 3, 7, 0, 0,
    22, 8, 0, 6, 15, 7, 1, 1, 0, 0, 0,
    26, 8, 0, 4, 15, 15, 1, 6, 3, 0, 0,
    24, 10, 0, 7, 15, 0, 1, 8, 2, 0, 0,
    //21:Electric Guitar 1,エレキギター 1
    58, 15, 2, 0, 210, 6, 2, 6, 1, 3, 0,
    31, 13, 1, 4, 15, 37, 2, 1, 3, 0, 0,
    31, 20, 1, 10, 15, 57, 1, 13, 7, 2, 0,
    20, 10, 1, 7, 15, 37, 1, 3, 7, 0, 0,
    23, 5, 1, 7, 15, 0, 0, 1, 3, 0, 1,
    //22:Electric Guitar 2,エレキギター 2
    61, 15, 2, 0, 207, 6, 0, 5, 0, 3, 0,
    28, 2, 1, 10, 15, 23, 2, 2, 0, 0, 0,
    31, 0, 1, 10, 0, 0, 0, 1, 0, 0, 1,
    31, 0, 1, 10, 0, 0, 0, 1, 0, 0, 1,
    6, 0, 1, 10, 0, 0, 0, 8, 0, 0, 1,
    //23:Electric Guitar 3,エレキギター 3
    2, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    30, 20, 0, 10, 15, 45, 0, 6, 0, 0, 0,
    18, 20, 0, 10, 7, 33, 1, 4, 0, 0, 0,
    31, 14, 0, 10, 15, 39, 1, 0, 0, 0, 0,
    28, 14, 0, 7, 15, 0, 2, 1, 4, 0, 0,
    //24:Electric Guitar 4,エレキギター 4
    2, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    28, 0, 0, 10, 0, 57, 0, 2, 7, 0, 0,
    31, 18, 0, 10, 2, 33, 1, 8, 7, 0, 0,
    26, 16, 6, 10, 2, 29, 1, 0, 4, 0, 0,
    28, 6, 0, 8, 15, 0, 1, 1, 4, 0, 0,
    //25:Electric Guitar 5,エレキギター 5
    17, 15, 2, 0, 210, 7, 0, 5, 0, 3, 0,
    31, 0, 4, 2, 0, 3, 0, 3, 3, 0, 0,
    31, 0, 0, 2, 0, 9, 0, 0, 2, 0, 0,
    26, 0, 0, 2, 0, 31, 0, 8, 4, 0, 0,
    20, 0, 4, 6, 0, 0, 1, 0, 4, 0, 1,
    //26:Wood Bass 1,ウッドベース 1
    58, 15, 2, 0, 150, 0, 10, 0, 1, 3, 0,
    31, 12, 1, 4, 15, 33, 1, 0, 7, 0, 0,
    31, 10, 1, 10, 15, 57, 1, 4, 5, 0, 0,
    31, 10, 1, 10, 15, 27, 0, 0, 2, 0, 0,
    31, 10, 1, 8, 15, 9, 1, 0, 3, 0, 1,
    //27:Wood Bass 2,ウッドベース 2
    58, 15, 2, 0, 150, 0, 10, 0, 1, 3, 0,
    27, 18, 1, 4, 15, 29, 1, 0, 7, 0, 0,
    31, 10, 1, 3, 15, 42, 1, 3, 5, 0, 0,
    31, 10, 1, 3, 15, 32, 0, 0, 2, 0, 0,
    29, 12, 1, 6, 15, 0, 1, 0, 3, 0, 1,
    //28:Electric Bass 1,エレキベース 1
    3, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 12, 0, 10, 15, 47, 0, 5, 6, 0, 0,
    31, 0, 0, 10, 0, 23, 0, 0, 4, 0, 0,
    31, 0, 4, 6, 0, 33, 0, 0, 4, 0, 0,
    28, 0, 6, 8, 0, 0, 0, 0, 3, 0, 1,
    //29:Electric Bass 2,エレキベース 2
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 2, 20, 0, 0, 23, 1, 1, 0, 0, 0,
    31, 2, 10, 6, 0, 0, 1, 1, 3, 0, 1,
    31, 2, 10, 4, 0, 15, 2, 0, 0, 0, 0,
    20, 2, 10, 5, 0, 0, 1, 0, 0, 0, 1,
    //30:Electric Bass 3,エレキベース 3
    32, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 7, 7, 9, 2, 29, 3, 6, 4, 0, 0,
    31, 6, 6, 9, 1, 47, 3, 5, 4, 0, 0,
    26, 9, 6, 9, 1, 29, 2, 0, 4, 0, 0,
    31, 8, 4, 9, 3, 0, 2, 1, 4, 0, 1,
    //31:Electric Bass 4,エレキベース 4
    27, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 21, 0, 8, 15, 0, 0, 6, 4, 0, 0,
    31, 15, 0, 8, 15, 35, 0, 9, 7, 0, 0,
    31, 0, 0, 6, 0, 37, 0, 0, 4, 0, 0,
    31, 8, 0, 10, 15, 0, 0, 1, 0, 0, 0,
    //32:Electric Bass 5,エレキベース 5
    17, 15, 2, 0, 220, 5, 0, 5, 0, 3, 0,
    31, 0, 0, 4, 0, 17, 0, 3, 3, 0, 0,
    31, 0, 0, 4, 0, 13, 0, 0, 5, 0, 0,
    26, 0, 0, 4, 0, 31, 0, 2, 4, 0, 0,
    20, 0, 3, 6, 0, 0, 0, 0, 4, 0, 0,
    //33:Mandolin,マンドリン
    3, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    28, 22, 0, 10, 15, 27, 0, 8, 0, 0, 0,
    31, 6, 0, 3, 3, 19, 0, 4, 4, 0, 0,
    31, 8, 0, 4, 3, 23, 0, 5, 6, 0, 0,
    24, 12, 0, 6, 15, 0, 1, 1, 3, 0, 0,
    //34:Ukulele,	ウクレレ
    1, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    26, 16, 0, 6, 15, 51, 1, 9, 4, 0, 0,
    31, 10, 0, 4, 15, 41, 1, 3, 3, 0, 0,
    31, 10, 0, 6, 15, 37, 1, 3, 7, 0, 0,
    24, 12, 0, 7, 15, 0, 1, 1, 6, 0, 0,
    //35:Banjo,バンジョー
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    24, 10, 0, 2, 5, 25, 1, 5, 7, 0, 0,
    26, 16, 0, 8, 11, 29, 0, 15, 0, 0, 0,
    28, 16, 0, 4, 3, 31, 0, 1, 6, 0, 0,
    24, 11, 0, 6, 15, 0, 2, 1, 3, 0, 0,
    //36:Sitar,シタール
    2, 15, 2, 0, 100, 10, 10, 1, 2, 3, 0,
    31, 31, 13, 3, 1, 17, 0, 7, 2, 0, 0,
    31, 15, 1, 10, 3, 27, 1, 9, 3, 0, 0,
    31, 15, 10, 3, 3, 27, 0, 1, 7, 0, 0,
    20, 2, 1, 4, 3, 7, 1, 1, 3, 0, 1,
    //37:Lute,リュート
    57, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    20, 30, 1, 5, 15, 47, 1, 6, 0, 0, 0,
    20, 10, 1, 5, 15, 47, 2, 4, 0, 0, 0,
    20, 5, 1, 5, 15, 57, 1, 2, 7, 0, 0,
    29, 10, 1, 5, 15, 0, 1, 2, 0, 0, 1,
    //38:Harp 1,ハープ 1
    0, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    31, 12, 1, 5, 15, 27, 1, 1, 7, 0, 0,
    31, 5, 0, 3, 15, 35, 1, 1, 0, 0, 0,
    31, 10, 0, 4, 15, 7, 1, 1, 0, 0, 1,
    //39:Harp 2,ハープ 2
    57, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 12, 0, 4, 15, 22, 0, 2, 0, 0, 0,
    31, 13, 0, 6, 1, 38, 0, 1, 4, 0, 0,
    31, 6, 5, 5, 1, 44, 0, 2, 0, 0, 0,
    31, 12, 7, 5, 1, 0, 0, 1, 0, 0, 1,
    //40:Koto,琴
    0, 15, 2, 1, 200, 5, 0, 5, 0, 3, 0,
    31, 10, 2, 5, 13, 27, 0, 3, 7, 0, 0,
    31, 10, 2, 5, 10, 37, 2, 4, 1, 0, 0,
    29, 8, 0, 4, 13, 27, 1, 1, 7, 0, 0,
    29, 9, 10, 5, 10, 0, 0, 1, 3, 0, 1,
    //41:Pipe Organ 1,パイプオルガン 1
    62, 15, 2, 0, 200, 8, 1, 3, 2, 3, 0,
    31, 20, 0, 10, 0, 24, 0, 6, 3, 0, 0,
    20, 2, 1, 10, 3, 0, 0, 2, 7, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 1, 1, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 6, 2, 0, 1,
    //42:Pipe Organ 2,パイプオルガン 2
    63, 15, 2, 0, 190, 0, 3, 0, 1, 3, 0,
    31, 1, 1, 10, 0, 117, 0, 8, 3, 0, 1,
    20, 2, 1, 10, 0, 0, 0, 3, 7, 0, 1,
    20, 2, 1, 10, 0, 0, 0, 1, 0, 0, 1,
    20, 2, 1, 10, 0, 0, 0, 2, 6, 0, 1,
    //43:Pipe Organ 3,パイプオルガン 3
    54, 15, 2, 0, 250, 5, 10, 1, 1, 3, 0,
    31, 21, 0, 15, 0, 42, 3, 3, 7, 0, 0,
    29, 31, 0, 10, 0, 27, 1, 8, 1, 0, 1,
    31, 31, 0, 10, 0, 0, 1, 1, 6, 0, 1,
    18, 31, 0, 10, 0, 0, 2, 4, 3, 0, 1,
    //44:Pipe Organ 4,パイプオルガン 4
    23, 15, 2, 0, 195, 5, 0, 4, 0, 3, 0,
    16, 0, 0, 10, 0, 0, 0, 2, 7, 0, 1,
    18, 2, 1, 10, 3, 37, 0, 5, 3, 0, 1,
    18, 2, 1, 10, 3, 27, 0, 2, 6, 0, 1,
    18, 2, 1, 10, 3, 27, 0, 3, 1, 0, 1,
    //45:Pipe Organ 5,パイプオルガン 5
    62, 15, 2, 0, 200, 8, 1, 3, 2, 3, 0,
    31, 20, 0, 10, 0, 27, 0, 12, 3, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 8, 7, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 0, 1, 0, 1,
    20, 2, 1, 10, 3, 0, 0, 2, 2, 0, 1,
    //46:Electric Organ 1,エレクトリックオルガン 1
    63, 15, 2, 0, 200, 3, 2, 2, 1, 3, 0,
    31, 14, 0, 15, 15, 107, 0, 6, 0, 0, 1,
    31, 2, 1, 15, 0, 0, 0, 1, 2, 0, 1,
    31, 2, 1, 15, 0, 0, 0, 3, 0, 0, 1,
    31, 2, 1, 15, 0, 0, 0, 2, 6, 0, 1,
    //47:Electric Organ 2,エレクトリックオルガン 2
    62, 15, 2, 1, 195, 5, 5, 1, 1, 3, 0,
    31, 19, 0, 10, 15, 47, 0, 3, 7, 0, 0,
    31, 2, 1, 10, 3, 0, 1, 12, 3, 0, 1,
    31, 0, 0, 10, 0, 0, 1, 1, 7, 0, 1,
    31, 0, 0, 10, 0, 0, 1, 3, 3, 0, 1,
    //48:Electric Organ 3,エレクトリックオルガン 3
    7, 15, 2, 0, 190, 10, 2, 2, 1, 3, 0,
    31, 18, 0, 15, 15, 7, 0, 6, 0, 0, 1,
    31, 2, 1, 15, 3, 0, 0, 2, 2, 0, 1,
    31, 2, 1, 15, 3, 0, 0, 3, 0, 0, 1,
    31, 2, 1, 15, 3, 0, 0, 1, 6, 0, 1,
    //49:Electric Organ 4,エレクトリックオルガン 4
    52, 15, 2, 0, 200, 6, 2, 4, 1, 3, 0,
    15, 2, 0, 3, 0, 15, 2, 3, 7, 0, 0,
    16, 2, 0, 6, 0, 8, 2, 5, 7, 0, 1,
    15, 2, 0, 3, 0, 12, 2, 0, 6, 0, 0,
    15, 2, 0, 7, 0, 0, 2, 1, 1, 0, 1,
    //50:Electric Organ 5,エレクトリックオルガン 5
    6, 15, 2, 0, 200, 10, 10, 1, 1, 3, 0,
    31, 0, 0, 15, 0, 17, 0, 3, 2, 0, 1,
    31, 0, 0, 15, 0, 7, 0, 3, 6, 0, 1,
    31, 0, 0, 15, 3, 0, 0, 0, 3, 0, 1,
    31, 0, 0, 15, 0, 7, 0, 2, 7, 0, 1,
    //51:Electric Organ 6,エレクトリックオルガン 6
    62, 15, 2, 1, 190, 10, 0, 1, 1, 3, 0,
    31, 0, 0, 15, 0, 30, 0, 0, 3, 0, 0,
    31, 0, 0, 15, 0, 0, 0, 0, 7, 0, 1,
    31, 0, 0, 15, 0, 0, 0, 3, 2, 0, 1,
    31, 0, 0, 15, 0, 0, 0, 2, 3, 0, 1,
    //52:Electric Organ 7,エレクトリックオルガン 7
    60, 15, 2, 1, 200, 6, 0, 4, 1, 3, 0,
    31, 0, 0, 15, 0, 37, 0, 0, 3, 0, 0,
    31, 0, 0, 15, 0, 0, 0, 0, 7, 0, 1,
    31, 0, 0, 15, 0, 24, 0, 3, 1, 0, 0,
    31, 0, 0, 15, 0, 0, 0, 2, 3, 0, 1,
    //53:School Organ,スクールオルガン
    60, 15, 2, 0, 200, 6, 1, 3, 1, 3, 0,
    20, 2, 0, 6, 0, 32, 3, 2, 3, 0, 0,
    9, 2, 1, 10, 3, 0, 3, 2, 3, 0, 1,
    18, 10, 0, 6, 0, 22, 3, 2, 3, 0, 0,
    9, 0, 0, 8, 0, 0, 3, 2, 0, 0, 1,
    //54:Street Organ,手回しオルガン
    60, 15, 2, 0, 180, 6, 0, 5, 0, 3, 0,
    18, 0, 0, 2, 0, 25, 1, 1, 3, 0, 0,
    15, 2, 0, 10, 0, 0, 1, 1, 7, 0, 1,
    31, 2, 0, 6, 0, 27, 1, 3, 7, 0, 0,
    15, 2, 0, 10, 0, 0, 1, 3, 2, 0, 1,
    //55:Accordion 1,アコーディオン 1
    60, 15, 2, 0, 180, 5, 0, 5, 0, 3, 0,
    18, 2, 1, 2, 0, 32, 1, 1, 3, 0, 0,
    15, 2, 1, 10, 0, 0, 1, 1, 7, 0, 1,
    31, 2, 1, 6, 0, 17, 1, 1, 7, 0, 0,
    20, 2, 1, 10, 0, 17, 1, 1, 2, 0, 1,
    //56:Accordion 2,アコーディオン 2
    1, 15, 2, 0, 210, 6, 0, 5, 0, 3, 0,
    31, 0, 0, 6, 0, 57, 0, 3, 7, 0, 0,
    31, 0, 0, 6, 0, 49, 0, 4, 6, 0, 0,
    31, 0, 0, 6, 0, 19, 0, 0, 2, 0, 0,
    14, 0, 0, 10, 0, 0, 0, 1, 0, 0, 0,
    //57:Violin 1,バイオリン 1
    58, 15, 2, 0, 202, 10, 3, 5, 0, 3, 0,
    20, 2, 0, 5, 1, 35, 1, 1, 0, 0, 0,
    25, 6, 0, 8, 3, 32, 1, 5, 7, 0, 0,
    28, 3, 0, 6, 1, 47, 1, 1, 0, 0, 0,
    12, 4, 0, 6, 0, 12, 1, 1, 4, 0, 1,
    //58:Violin 2,バイオリン 2
    24, 15, 2, 0, 200, 6, 0, 6, 0, 3, 0,
    17, 10, 18, 10, 0, 42, 1, 15, 7, 3, 0,
    18, 2, 9, 10, 0, 37, 1, 6, 7, 0, 0,
    18, 5, 1, 3, 0, 17, 2, 1, 7, 0, 0,
    12, 2, 1, 7, 1, 0, 1, 1, 3, 0, 1,
    //59:Violin 3,バイオリン 3
    58, 15, 2, 0, 204, 5, 0, 6, 0, 3, 0,
    20, 10, 0, 8, 1, 29, 0, 2, 4, 0, 0,
    30, 17, 0, 10, 10, 29, 0, 10, 7, 1, 0,
    18, 9, 0, 6, 2, 21, 0, 3, 3, 0, 0,
    13, 12, 0, 8, 1, 0, 0, 1, 1, 0, 0,
    //60:Cello 1,チェロ 1
    56, 15, 2, 0, 200, 5, 0, 7, 0, 3, 0,
    18, 31, 20, 10, 0, 17, 1, 15, 7, 3, 0,
    31, 17, 12, 10, 0, 37, 1, 6, 7, 0, 0,
    13, 18, 1, 3, 0, 17, 2, 1, 7, 0, 0,
    12, 2, 1, 10, 1, 0, 1, 1, 3, 0, 1,
    //61:Cello 2,チェロ 2
    56, 15, 2, 0, 190, 5, 0, 6, 0, 3, 0,
    15, 31, 31, 10, 2, 22, 1, 15, 7, 3, 0,
    21, 28, 12, 10, 2, 22, 1, 6, 4, 0, 0,
    15, 18, 0, 3, 0, 22, 2, 1, 7, 0, 0,
    10, 2, 1, 8, 0, 0, 0, 1, 3, 0, 0,
    //62:Contrabass,コントラバス
    56, 15, 2, 0, 200, 6, 0, 6, 0, 3, 0,
    18, 31, 20, 10, 0, 27, 1, 15, 7, 3, 0,
    15, 17, 12, 10, 0, 47, 1, 6, 7, 0, 0,
    15, 18, 1, 3, 0, 17, 2, 1, 7, 0, 0,
    12, 2, 1, 9, 1, 0, 1, 1, 3, 0, 1,
    //63:Strings 1,ストリングス 1
    58, 15, 2, 0, 205, 10, 0, 5, 0, 3, 0,
    30, 1, 0, 1, 1, 22, 3, 0, 2, 0, 0,
    31, 1, 0, 5, 1, 47, 3, 2, 3, 0, 0,
    30, 1, 0, 5, 1, 57, 1, 1, 3, 0, 0,
    13, 2, 0, 6, 0, 0, 1, 1, 7, 0, 1,
    //64:Strings 2,ストリングス 2
    58, 15, 2, 0, 200, 6, 0, 6, 0, 3, 0,
    30, 1, 0, 1, 1, 29, 3, 0, 2, 0, 0,
    31, 1, 0, 5, 1, 107, 3, 2, 3, 0, 0,
    30, 1, 0, 5, 1, 97, 1, 1, 3, 0, 0,
    13, 2, 0, 6, 0, 0, 1, 1, 7, 0, 1,
    //65:Strings 3,ストリングス 3
    60, 15, 2, 0, 200, 3, 0, 7, 0, 3, 0,
    31, 31, 0, 5, 0, 30, 0, 2, 3, 0, 1,
    13, 31, 0, 6, 0, 0, 0, 2, 7, 0, 1,
    31, 31, 0, 5, 0, 34, 1, 4, 2, 0, 1,
    13, 31, 0, 6, 0, 12, 1, 4, 3, 0, 1,
    //66:Strings 4,ストリングス 4
    61, 15, 2, 0, 202, 6, 0, 7, 0, 3, 0,
    31, 0, 0, 4, 0, 29, 0, 1, 4, 0, 0,
    10, 0, 0, 6, 0, 37, 0, 2, 4, 0, 0,
    10, 0, 0, 6, 0, 35, 0, 1, 7, 0, 0,
    10, 0, 0, 6, 0, 0, 0, 1, 4, 0, 0,
    //67:Pizzicato 1,ピチカート 1
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 20, 1, 3, 15, 27, 0, 1, 3, 0, 0,
    18, 15, 1, 5, 14, 7, 1, 1, 7, 0, 1,
    31, 10, 0, 3, 15, 37, 1, 1, 3, 0, 0,
    31, 15, 1, 5, 14, 7, 1, 1, 3, 0, 1,
    //68:Pizzicato 2,ピチカート 2
    56, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 20, 1, 3, 15, 27, 0, 0, 3, 0, 0,
    18, 15, 1, 6, 14, 38, 1, 0, 7, 0, 0,
    31, 10, 0, 3, 15, 37, 1, 1, 3, 0, 0,
    31, 15, 1, 6, 14, 7, 1, 1, 3, 0, 1,
    //69:Female Voice 1,女声 1
    6, 15, 2, 0, 200, 9, 0, 5, 0, 3, 0,
    10, 0, 1, 3, 0, 77, 0, 1, 0, 0, 0,
    10, 0, 0, 5, 0, 0, 2, 3, 3, 0, 1,
    10, 0, 1, 6, 2, 0, 1, 2, 7, 0, 1,
    10, 0, 0, 6, 0, 0, 1, 1, 3, 0, 1,
    //70:Female Voice 2,女声 2
    6, 15, 2, 0, 196, 5, 0, 7, 0, 3, 0,
    20, 0, 0, 6, 0, 57, 0, 1, 4, 0, 0,
    14, 0, 0, 8, 0, 0, 0, 2, 4, 0, 0,
    14, 0, 0, 8, 0, 123, 0, 5, 4, 2, 0,
    14, 0, 0, 8, 0, 0, 0, 3, 4, 0, 0,
    //71:Male Voice 1,男声 1
    36, 15, 2, 0, 200, 6, 0, 7, 0, 3, 0,
    20, 0, 0, 4, 0, 25, 0, 1, 4, 0, 0,
    14, 0, 0, 8, 0, 0, 0, 2, 7, 0, 0,
    20, 0, 0, 10, 0, 32, 0, 1, 4, 0, 0,
    14, 0, 0, 10, 0, 47, 0, 11, 0, 3, 0,
    //72:Male Voice 2,男声 2
    3, 15, 2, 0, 202, 5, 0, 7, 0, 3, 0,
    14, 10, 0, 5, 1, 51, 0, 15, 3, 3, 0,
    16, 10, 0, 5, 2, 29, 0, 1, 3, 0, 0,
    15, 10, 0, 5, 1, 49, 1, 6, 4, 2, 0,
    15, 0, 0, 8, 0, 0, 0, 3, 4, 0, 0,
    //73:Female Chorus,女声コーラス
    41, 15, 2, 0, 203, 5, 0, 6, 0, 3, 0,
    19, 18, 4, 4, 5, 68, 0, 6, 3, 3, 0,
    21, 14, 6, 10, 6, 57, 0, 4, 7, 3, 0,
    11, 31, 3, 10, 0, 47, 0, 1, 7, 0, 0,
    14, 31, 1, 8, 0, 0, 0, 1, 3, 0, 1,
    //74:Male Chorus,男声コーラス
    3, 15, 2, 0, 200, 5, 0, 7, 0, 3, 0,
    16, 0, 0, 4, 0, 63, 0, 15, 4, 2, 0,
    16, 0, 0, 4, 0, 29, 0, 1, 7, 0, 0,
    16, 0, 0, 4, 0, 59, 0, 4, 7, 0, 0,
    15, 0, 0, 8, 0, 0, 0, 1, 7, 0, 0,
    //75:Chorus 1,コーラス 1
    60, 15, 2, 0, 200, 10, 0, 5, 0, 3, 0,
    15, 2, 1, 3, 0, 47, 2, 7, 7, 3, 0,
    10, 2, 1, 5, 3, 7, 2, 7, 3, 3, 1,
    20, 2, 1, 3, 3, 20, 1, 4, 3, 0, 0,
    10, 2, 1, 5, 3, 0, 2, 4, 7, 0, 1,
    //76:Chorus 2,コーラス 2
    4, 15, 2, 0, 198, 5, 0, 7, 0, 3, 0,
    20, 0, 0, 2, 0, 39, 0, 1, 4, 0, 0,
    10, 0, 0, 6, 0, 0, 0, 1, 4, 0, 0,
    20, 0, 0, 2, 0, 51, 0, 2, 6, 0, 0,
    10, 0, 0, 6, 0, 0, 0, 2, 6, 0, 0,
    //77:Vocoder,ボコーダ
    4, 15, 2, 0, 206, 6, 0, 6, 0, 3, 0,
    20, 0, 0, 10, 0, 40, 0, 1, 4, 0, 0,
    18, 0, 0, 10, 0, 0, 0, 3, 4, 0, 0,
    20, 0, 0, 10, 0, 41, 0, 1, 4, 0, 0,
    20, 0, 0, 10, 0, 17, 0, 5, 4, 3, 0,
    //78:Glassharp 1,グラスハープ 1
    36, 15, 0, 0, 80, 1, 2, 1, 1, 3, 0,
    20, 2, 1, 5, 3, 32, 1, 4, 0, 0, 0,
    8, 7, 7, 5, 0, 0, 1, 0, 0, 1, 1,
    20, 2, 1, 5, 3, 32, 3, 4, 6, 0, 0,
    8, 7, 7, 5, 0, 0, 0, 0, 2, 1, 1,
    //79:Glassharp 2,グラスハープ 2
    60, 15, 0, 0, 80, 1, 2, 1, 1, 3, 0,
    20, 2, 1, 5, 3, 29, 1, 5, 0, 0, 0,
    8, 7, 7, 5, 0, 0, 1, 0, 0, 1, 1,
    20, 2, 1, 5, 3, 17, 2, 5, 6, 0, 0,
    8, 7, 7, 5, 0, 0, 0, 0, 2, 1, 1,
    //80:Whistle,ホイッスル
    7, 15, 2, 0, 200, 5, 0, 7, 0, 3, 0,
    0, 0, 0, 0, 0, 127, 0, 0, 4, 0, 0,
    0, 0, 0, 0, 0, 127, 0, 0, 4, 0, 0,
    14, 12, 0, 8, 0, 0, 0, 5, 7, 2, 0,
    14, 12, 0, 8, 0, 0, 0, 8, 7, 0, 1,
    //81:Piccolo,ピッコロ
    4, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    20, 10, 1, 10, 3, 67, 1, 1, 4, 0, 0,
    20, 11, 3, 9, 2, 10, 0, 1, 4, 0, 0,
    20, 10, 1, 10, 5, 82, 1, 3, 4, 3, 0,
    20, 11, 3, 9, 2, 17, 0, 1, 4, 0, 0,
    //82:Flute 1,フルート 1
    59, 15, 2, 0, 196, 5, 11, 6, 3, 3, 0,
    31, 5, 3, 5, 14, 55, 3, 2, 7, 1, 0,
    12, 7, 0, 5, 15, 57, 1, 2, 0, 0, 0,
    15, 2, 0, 4, 2, 55, 3, 1, 3, 0, 0,
    12, 16, 0, 6, 1, 0, 2, 1, 0, 0, 1,
    //83:Flute 2,フルート 2
    59, 15, 2, 0, 203, 10, 38, 5, 0, 3, 0,
    31, 20, 19, 9, 5, 28, 1, 3, 4, 1, 0,
    31, 17, 0, 6, 2, 47, 0, 4, 4, 0, 0,
    25, 20, 0, 5, 7, 45, 0, 2, 4, 0, 0,
    16, 31, 0, 11, 0, 0, 1, 2, 4, 0, 1,
    //84:Flute 3,フルート 3
    59, 15, 2, 0, 196, 9, 20, 5, 1, 3, 0,
    31, 0, 0, 10, 0, 0, 0, 15, 0, 3, 0,
    10, 6, 0, 10, 2, 81, 2, 12, 0, 3, 0,
    20, 0, 0, 6, 0, 39, 1, 1, 3, 0, 0,
    10, 6, 0, 6, 1, 0, 2, 1, 5, 0, 1,
    //85:Oboe 1,オーボエ 1
    58, 15, 2, 0, 198, 7, 8, 6, 1, 3, 0,
    31, 0, 0, 6, 0, 39, 3, 1, 3, 0, 0,
    28, 12, 12, 11, 5, 39, 3, 9, 3, 0, 0,
    28, 16, 0, 5, 2, 57, 1, 2, 3, 0, 0,
    14, 16, 0, 8, 1, 0, 1, 4, 3, 0, 1,
    //86:Oboe 2,オーボエ 2
    18, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 20, 0, 10, 9, 47, 0, 6, 4, 0, 0,
    20, 0, 0, 6, 0, 43, 0, 2, 4, 0, 0,
    20, 0, 0, 6, 0, 27, 0, 1, 4, 0, 0,
    18, 0, 0, 10, 0, 0, 0, 4, 4, 0, 0,
    //87:English Horn,イングリッシュホルン
    2, 15, 2, 0, 200, 8, 8, 5, 1, 3, 0,
    22, 20, 0, 10, 11, 31, 0, 4, 4, 0, 0,
    20, 0, 0, 6, 0, 31, 0, 2, 4, 0, 0,
    20, 0, 0, 6, 0, 31, 0, 1, 4, 0, 0,
    17, 16, 0, 9, 1, 0, 0, 2, 4, 0, 1,
    //88:Clarinet 1,クラリネット 1
    58, 15, 2, 0, 198, 9, 20, 4, 1, 3, 0,
    19, 25, 0, 10, 2, 35, 2, 2, 0, 0, 0,
    29, 19, 0, 8, 3, 29, 2, 9, 0, 0, 0,
    29, 20, 0, 7, 1, 53, 0, 1, 0, 0, 0,
    17, 31, 0, 9, 0, 17, 1, 1, 0, 0, 1,
    //89:Clarinet 2,クラリネット 2
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 20, 0, 10, 9, 71, 0, 9, 4, 2, 0,
    20, 0, 0, 6, 0, 39, 0, 2, 4, 0, 0,
    20, 0, 0, 6, 0, 25, 0, 2, 4, 0, 0,
    18, 0, 0, 10, 0, 0, 0, 1, 4, 0, 0,
    //90:Bass Clarinet,バスクラリネット
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 20, 0, 10, 9, 43, 0, 9, 0, 2, 0,
    20, 0, 0, 6, 0, 29, 0, 2, 0, 0, 0,
    20, 0, 0, 6, 0, 17, 0, 2, 0, 0, 0,
    16, 0, 0, 10, 0, 0, 0, 1, 0, 0, 1,
    //91:Bassoon 1,バスーン 1
    44, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 0, 0, 10, 0, 37, 0, 1, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 1, 2, 4, 0, 0,
    19, 14, 0, 10, 1, 37, 0, 1, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 0, 5, 4, 0, 0,
    //92:Bassoon 2,バスーン 2
    2, 15, 2, 0, 198, 10, 0, 5, 0, 3, 0,
    18, 0, 0, 8, 0, 57, 0, 3, 4, 0, 0,
    31, 0, 0, 8, 0, 107, 0, 8, 4, 0, 0,
    18, 0, 0, 8, 0, 37, 0, 1, 4, 0, 0,
    20, 0, 0, 11, 0, 0, 3, 2, 4, 0, 0,
    //93:Saxophone 1,サクソフォン 1
    58, 15, 2, 0, 200, 9, 0, 5, 0, 3, 0,
    18, 0, 0, 6, 0, 37, 0, 0, 0, 0, 0,
    18, 0, 0, 6, 3, 73, 0, 4, 0, 1, 0,
    18, 0, 0, 6, 0, 41, 0, 0, 0, 0, 0,
    16, 8, 0, 8, 1, 6, 0, 1, 7, 0, 1,
    //94:Saxophone 2,サクソフォン 2
    56, 15, 2, 0, 204, 10, 0, 5, 0, 3, 0,
    20, 0, 0, 6, 0, 27, 0, 1, 0, 0, 0,
    20, 0, 0, 6, 0, 29, 0, 1, 0, 0, 0,
    20, 0, 0, 6, 0, 37, 0, 2, 0, 0, 0,
    16, 0, 0, 8, 0, 0, 1, 1, 0, 0, 1,
    //95:Saxophone 3,サクソフォン 3
    58, 15, 2, 0, 204, 10, 0, 5, 0, 3, 0,
    20, 4, 0, 6, 1, 27, 1, 1, 2, 0, 0,
    20, 14, 0, 8, 3, 45, 1, 6, 0, 2, 0,
    20, 0, 0, 6, 0, 39, 1, 1, 7, 0, 0,
    16, 0, 0, 10, 0, 0, 1, 2, 0, 0, 1,
    //96:Piccolo Trumpet,ピッコロトランペット
    50, 15, 2, 0, 204, 10, 0, 5, 0, 3, 0,
    16, 12, 0, 6, 1, 27, 0, 1, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    20, 0, 0, 6, 0, 51, 0, 1, 0, 0, 0,
    18, 0, 0, 10, 0, 0, 0, 1, 0, 0, 1,
    //97:Trumpet 1,トランペット 1
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    16, 5, 10, 0, 9, 30, 1, 1, 3, 0, 0,
    13, 10, 1, 10, 10, 37, 3, 2, 2, 0, 0,
    15, 10, 0, 10, 1, 37, 1, 1, 4, 0, 0,
    20, 10, 0, 10, 0, 13, 1, 1, 6, 0, 1,
    //98:Trumpet 2,トランペット 2
    58, 15, 2, 0, 204, 9, 10, 5, 1, 3, 0,
    16, 14, 0, 8, 0, 27, 1, 1, 0, 0, 0,
    15, 12, 0, 10, 15, 63, 1, 2, 0, 2, 0,
    20, 0, 0, 10, 0, 47, 0, 1, 0, 0, 0,
    16, 0, 0, 10, 0, 0, 1, 1, 0, 0, 1,
    //99:Flugelhorn,フリューゲルホルン
    50, 15, 2, 0, 204, 4, 0, 6, 0, 3, 0,
    14, 0, 0, 8, 0, 30, 1, 1, 0, 0, 0,
    14, 15, 0, 10, 11, 67, 0, 4, 0, 2, 0,
    20, 0, 0, 8, 0, 39, 0, 2, 0, 0, 0,
    18, 10, 0, 10, 1, 0, 0, 1, 0, 0, 1,
    //100:Mute Trumpet,ミュートトランペット
    59, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 14, 0, 10, 1, 23, 0, 4, 6, 0, 0,
    16, 10, 0, 10, 1, 25, 1, 1, 4, 0, 0,
    20, 0, 0, 6, 0, 30, 0, 1, 4, 0, 0,
    18, 0, 0, 10, 0, 27, 1, 1, 4, 0, 0,
    //101:Horn 1,ホルン 1
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    14, 9, 0, 9, 2, 35, 0, 1, 4, 0, 0,
    31, 17, 0, 15, 12, 57, 1, 5, 4, 2, 0,
    13, 11, 0, 8, 1, 46, 0, 1, 4, 0, 0,
    15, 31, 0, 10, 0, 1, 0, 1, 4, 0, 1,
    //102:Horn 2,ホルン 2
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    12, 8, 0, 10, 2, 33, 0, 1, 0, 0, 0,
    16, 12, 0, 10, 1, 59, 0, 2, 7, 2, 0,
    14, 12, 0, 10, 5, 37, 0, 1, 0, 0, 0,
    15, 12, 0, 8, 2, 0, 1, 1, 0, 0, 1,
    //103:Mute Horn,ミュートホルン
    57, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    14, 10, 0, 10, 3, 47, 1, 2, 4, 0, 0,
    11, 9, 0, 8, 9, 63, 0, 6, 7, 2, 0,
    16, 0, 0, 8, 0, 35, 0, 1, 4, 0, 0,
    16, 12, 0, 9, 1, 0, 0, 2, 4, 0, 0,
    //104:Trombone 1,トロンボーン 1
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    16, 12, 0, 8, 0, 29, 0, 1, 0, 0, 0,
    14, 14, 0, 10, 15, 41, 0, 2, 0, 2, 0,
    20, 14, 0, 10, 7, 47, 0, 1, 0, 0, 0,
    16, 14, 0, 8, 1, 0, 0, 1, 0, 0, 1,
    //105:Trombone 2,トロンボーン 2
    58, 15, 2, 0, 200, 8, 0, 6, 0, 3, 0,
    13, 0, 0, 8, 0, 32, 1, 1, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    12, 0, 0, 6, 0, 52, 0, 1, 0, 0, 0,
    16, 8, 0, 8, 1, 0, 0, 1, 0, 0, 1,
    //106:Mute Trombone,ミュートトロンボーン
    59, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    16, 14, 0, 10, 1, 27, 0, 1, 6, 0, 0,
    16, 10, 0, 10, 1, 43, 1, 1, 4, 0, 0,
    20, 0, 0, 6, 0, 33, 0, 1, 4, 0, 0,
    18, 0, 0, 10, 0, 0, 1, 1, 4, 0, 0,
    //107:Tuba,チューバ
    54, 15, 2, 0, 203, 2, 2, 1, 1, 3, 0,
    14, 10, 1, 5, 5, 24, 1, 0, 1, 0, 0,
    18, 2, 1, 8, 3, 0, 0, 1, 3, 0, 1,
    31, 2, 18, 10, 5, 0, 2, 1, 7, 0, 1,
    15, 2, 1, 10, 5, 0, 2, 0, 3, 0, 1,
    //108:Brass 1,ブラス 1
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 12, 1, 10, 2, 32, 1, 1, 0, 0, 0,
    18, 10, 1, 10, 3, 0, 0, 1, 1, 0, 1,
    15, 10, 1, 10, 5, 23, 1, 1, 2, 0, 0,
    20, 2, 1, 10, 3, 7, 0, 1, 6, 0, 1,
    //109:Brass 2,ブラス 2
    58, 15, 2, 0, 206, 7, 0, 5, 0, 3, 0,
    16, 15, 0, 8, 1, 25, 0, 1, 7, 0, 0,
    16, 12, 0, 4, 1, 63, 0, 8, 0, 2, 0,
    18, 0, 0, 4, 0, 53, 0, 1, 0, 0, 0,
    16, 0, 0, 10, 0, 0, 0, 2, 0, 0, 1,
    //110:Brass 3,ブラス 3
    60, 15, 2, 0, 200, 8, 0, 5, 0, 3, 0,
    14, 12, 0, 4, 1, 25, 1, 1, 3, 0, 0,
    18, 0, 0, 8, 0, 0, 1, 1, 3, 0, 1,
    15, 0, 0, 6, 0, 15, 1, 0, 7, 0, 0,
    16, 0, 0, 9, 0, 33, 1, 1, 7, 0, 1,
    //111:Harmonica 1,ハーモニカ 1
    59, 15, 2, 0, 201, 6, 14, 7, 1, 2, 0,
    31, 31, 0, 6, 0, 35, 0, 5, 4, 0, 0,
    31, 31, 0, 5, 0, 56, 0, 6, 7, 0, 0,
    31, 31, 0, 5, 0, 31, 0, 1, 4, 0, 0,
    13, 31, 0, 9, 0, 1, 0, 1, 4, 0, 1,
    //112:Harmonica 2,ハーモニカ 2
    0, 15, 2, 0, 189, 10, 11, 4, 1, 3, 0,
    31, 31, 0, 9, 0, 71, 0, 9, 4, 2, 1,
    10, 31, 0, 9, 0, 36, 0, 10, 4, 0, 0,
    31, 31, 0, 3, 0, 48, 0, 1, 4, 0, 0,
    13, 31, 0, 8, 0, 0, 0, 2, 4, 0, 1,
    //113:Ocarina,オカリナ
    59, 15, 2, 0, 204, 8, 0, 6, 0, 3, 0,
    31, 16, 0, 10, 15, 37, 0, 4, 0, 0, 0,
    24, 0, 0, 10, 0, 77, 0, 2, 0, 0, 0,
    20, 0, 0, 10, 0, 77, 0, 3, 7, 1, 0,
    16, 5, 0, 10, 7, 0, 0, 4, 0, 0, 1,
    //114:Recoder 1,リコーダー 1
    59, 15, 2, 0, 196, 10, 0, 5, 0, 2, 0,
    14, 17, 16, 6, 3, 59, 0, 4, 4, 1, 0,
    16, 31, 0, 0, 0, 72, 0, 2, 4, 0, 0,
    25, 31, 0, 7, 0, 34, 0, 2, 7, 0, 0,
    17, 31, 0, 9, 0, 0, 0, 1, 4, 0, 1,
    //115:Recoder 2,リコーダー 2
    28, 15, 2, 0, 200, 10, 20, 4, 1, 3, 0,
    15, 20, 0, 10, 2, 29, 1, 4, 7, 0, 0,
    18, 2, 1, 10, 0, 13, 2, 2, 3, 0, 1,
    20, 31, 15, 10, 3, 24, 0, 4, 7, 0, 0,
    16, 2, 1, 10, 0, 0, 1, 2, 3, 0, 1,
    //116:Pan Flute,パンフルート
    59, 15, 2, 0, 200, 10, 0, 5, 0, 3, 0,
    20, 0, 0, 10, 0, 0, 0, 4, 0, 0, 0,
    14, 16, 0, 10, 5, 59, 0, 2, 3, 0, 0,
    18, 18, 0, 10, 9, 39, 0, 3, 0, 1, 0,
    14, 12, 0, 10, 2, 0, 1, 1, 0, 0, 1,
    //117:Bagpipe,バグパイプ
    3, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    20, 0, 0, 4, 0, 27, 0, 2, 0, 0, 0,
    20, 0, 0, 4, 0, 21, 0, 1, 0, 0, 0,
    24, 20, 0, 10, 15, 17, 0, 2, 0, 0, 0,
    17, 0, 0, 10, 0, 0, 0, 3, 0, 0, 1,
    //118:Apito,サンバホイッスル
    2, 15, 2, 0, 236, 50, 0, 7, 0, 3, 0,
    31, 0, 0, 10, 0, 47, 0, 6, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    31, 0, 0, 10, 0, 43, 0, 10, 0, 0, 0,
    20, 8, 0, 10, 1, 0, 0, 2, 0, 0, 1,
    //119:Shakuhachi,尺八
    59, 15, 2, 0, 194, 0, 40, 0, 1, 3, 0,
    31, 0, 0, 10, 0, 0, 0, 10, 0, 0, 0,
    24, 14, 0, 10, 3, 59, 0, 2, 6, 0, 0,
    24, 10, 0, 10, 7, 57, 0, 2, 0, 0, 0,
    14, 11, 0, 8, 3, 0, 1, 1, 3, 0, 1,
    //120:Shou,笙
    3, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 4, 3, 23, 0, 2, 0, 0, 0,
    31, 0, 0, 4, 0, 25, 0, 1, 0, 0, 0,
    20, 0, 0, 2, 0, 25, 0, 1, 0, 0, 0,
    10, 0, 0, 10, 0, 0, 0, 2, 0, 0, 1,
    //121:Snare Drum 1,スネアドラム 1
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 1, 0, 0, 0, 12, 0, 3, 0,
    28, 17, 0, 8, 15, 7, 1, 1, 0, 0, 1,
    30, 17, 0, 9, 13, 37, 1, 0, 0, 2, 0,
    28, 15, 0, 7, 15, 0, 2, 1, 0, 0, 1,
    //122:Snare Drum 2,スネアドラム 2
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    30, 0, 0, 1, 0, 0, 0, 2, 0, 2, 0,
    28, 16, 0, 8, 15, 67, 0, 1, 0, 0, 1,
    28, 18, 0, 9, 15, 33, 0, 0, 0, 3, 0,
    30, 16, 0, 8, 15, 0, 0, 0, 0, 2, 1,
    //123:Snare Drum Rimshot,スネアドラム リムショット
    2, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    30, 16, 1, 10, 15, 45, 0, 3, 0, 3, 0,
    30, 10, 0, 10, 15, 41, 0, 0, 7, 1, 0,
    30, 20, 0, 10, 15, 17, 0, 0, 3, 3, 0,
    30, 20, 0, 10, 15, 0, 0, 1, 0, 0, 1,
    //124:Snare Drum Brush,スネアドラム ブラシ
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 2, 0, 0, 0, 14, 0, 0, 0,
    31, 0, 0, 2, 0, 0, 0, 9, 0, 0, 0,
    31, 0, 0, 2, 0, 0, 0, 5, 0, 0, 0,
    8, 8, 0, 4, 15, 0, 3, 1, 0, 0, 1,
    //125:Bass Drum 1,バスドラム 1
    0, 15, 2, 0, 200, 0, 0, 0, 0, 2, 0,
    30, 26, 0, 13, 15, 21, 0, 1, 0, 1, 0,
    30, 28, 0, 14, 15, 47, 0, 14, 0, 3, 0,
    30, 16, 0, 8, 15, 7, 0, 0, 0, 1, 0,
    29, 16, 0, 8, 15, 0, 0, 0, 0, 0, 1,
    //126:Bass Drum 2,バスドラム 2
    43, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 20, 0, 10, 15, 11, 0, 1, 0, 0, 0,
    31, 12, 0, 6, 15, 31, 0, 0, 0, 3, 0,
    31, 22, 0, 10, 3, 17, 0, 0, 0, 0, 0,
    31, 18, 0, 9, 15, 0, 0, 1, 0, 0, 1,
    //127:Tom-Tom 1,トムトム 1
    50, 15, 2, 1, 140, 127, 0, 5, 0, 3, 0,
    24, 20, 0, 10, 15, 21, 1, 2, 0, 0, 0,
    26, 12, 0, 6, 15, 23, 1, 1, 0, 2, 0,
    31, 10, 0, 4, 15, 37, 1, 1, 3, 1, 0,
    26, 11, 0, 5, 15, 0, 2, 1, 0, 0, 1,
    //128:Tom-Tom 2,トムトム 2
    33, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    26, 22, 0, 10, 10, 7, 0, 3, 0, 0, 0,
    28, 20, 0, 10, 15, 15, 0, 2, 0, 1, 0,
    28, 22, 0, 10, 15, 53, 0, 1, 0, 3, 0,
    26, 12, 0, 6, 15, 0, 2, 1, 0, 0, 1,
    //129:Timpani 1,ティンパニ 1
    2, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    28, 12, 0, 4, 15, 37, 1, 0, 0, 1, 0,
    20, 8, 0, 4, 15, 39, 1, 0, 0, 2, 0,
    28, 10, 0, 5, 15, 37, 0, 0, 0, 0, 0,
    16, 5, 0, 2, 15, 0, 3, 0, 0, 0, 1,
    //130:Timpani 2,ティンパニ 2
    50, 15, 2, 0, 200, 14, 0, 7, 0, 3, 0,
    30, 10, 0, 2, 15, 33, 1, 0, 0, 0, 0,
    30, 10, 0, 4, 15, 31, 0, 0, 5, 3, 0,
    30, 10, 0, 4, 5, 33, 1, 0, 3, 1, 0,
    26, 8, 0, 4, 15, 0, 2, 0, 0, 0, 1,
    //131:Bongo,ボンゴ
    59, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    24, 23, 0, 11, 15, 3, 0, 3, 0, 3, 0,
    26, 14, 0, 7, 15, 43, 0, 2, 0, 2, 0,
    26, 10, 0, 5, 15, 59, 0, 2, 0, 3, 0,
    22, 16, 0, 8, 15, 0, 2, 6, 0, 0, 1,
    //132:Conga,コンガ
    51, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    25, 23, 0, 12, 15, 7, 0, 3, 0, 3, 0,
    26, 14, 0, 7, 15, 51, 0, 3, 0, 0, 0,
    26, 8, 0, 5, 5, 57, 0, 4, 0, 0, 0,
    24, 16, 0, 8, 15, 0, 2, 6, 0, 0, 1,
    //133:Timbales,ティンバレス
    50, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    28, 15, 0, 6, 15, 21, 1, 2, 3, 3, 0,
    24, 16, 0, 7, 15, 33, 0, 8, 7, 2, 0,
    26, 15, 0, 7, 15, 31, 1, 5, 3, 0, 0,
    24, 11, 0, 5, 15, 0, 2, 2, 7, 3, 1,
    //134:Cuica,クイーカ
    4, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    0, 0, 0, 0, 0, 127, 0, 1, 0, 0, 0,
    0, 0, 0, 0, 0, 127, 0, 1, 0, 0, 0,
    16, 26, 0, 10, 15, 47, 0, 1, 0, 0, 0,
    14, 24, 0, 10, 15, 0, 0, 1, 0, 0, 1,
    //135:Triangle,トライアングル
    3, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 6, 0, 4, 15, 51, 0, 1, 0, 3, 0,
    31, 0, 0, 2, 0, 21, 0, 8, 7, 2, 0,
    31, 8, 0, 6, 5, 67, 0, 9, 3, 1, 0,
    31, 10, 0, 5, 15, 0, 0, 10, 3, 2, 1,
    //136:Tambourine,タンバリン
    58, 15, 2, 0, 227, 29, 0, 7, 0, 3, 0,
    31, 21, 0, 4, 5, 11, 0, 15, 0, 3, 0,
    31, 0, 0, 3, 0, 51, 0, 0, 0, 3, 0,
    31, 0, 0, 4, 0, 19, 0, 0, 0, 0, 0,
    20, 16, 0, 8, 15, 0, 0, 13, 0, 0, 1,
    //137:Sleigh Bell,スレイベル
    58, 15, 2, 0, 227, 29, 0, 7, 0, 3, 0,
    24, 18, 0, 4, 1, 29, 0, 15, 0, 3, 0,
    31, 0, 0, 3, 0, 73, 0, 2, 0, 1, 0,
    31, 0, 0, 3, 0, 21, 0, 0, 0, 0, 0,
    18, 14, 0, 7, 15, 0, 0, 13, 0, 0, 1,
    //138:Agogo Bell,アゴゴベル
    34, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 24, 0, 10, 15, 17, 0, 8, 3, 3, 0,
    31, 10, 0, 2, 7, 23, 0, 11, 3, 3, 0,
    31, 16, 0, 8, 7, 33, 0, 5, 3, 0, 0,
    31, 14, 0, 6, 15, 0, 0, 3, 0, 3, 1,
    //139:Cow Bell,カウベル
    59, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    30, 20, 0, 10, 15, 27, 0, 15, 2, 0, 0,
    30, 17, 0, 8, 15, 27, 1, 4, 0, 1, 0,
    28, 12, 0, 6, 15, 43, 1, 2, 3, 2, 0,
    26, 16, 0, 8, 15, 0, 1, 2, 0, 3, 1,
    //140:Hand Bell,ハンドベル
    42, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 22, 0, 12, 15, 19, 0, 15, 0, 3, 0,
    31, 16, 0, 10, 15, 41, 0, 14, 7, 0, 0,
    31, 8, 0, 4, 15, 25, 0, 7, 0, 0, 0,
    31, 10, 0, 4, 15, 17, 1, 2, 3, 0, 1,
    //141:Tubular Bells,チューブラーベル
    46, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 6, 0, 3, 15, 29, 0, 13, 0, 0, 0,
    30, 8, 0, 4, 15, 17, 1, 5, 0, 0, 1,
    30, 8, 0, 4, 15, 17, 1, 12, 0, 0, 1,
    31, 21, 0, 10, 15, 17, 0, 14, 0, 3, 1,
    //142:Antique Cymbal,アンティークシンバル
    2, 15, 2, 0, 208, 5, 0, 5, 0, 3, 0,
    31, 20, 0, 10, 15, 53, 0, 2, 0, 2, 0,
    31, 16, 0, 9, 15, 57, 0, 5, 0, 3, 0,
    31, 8, 0, 4, 15, 25, 0, 9, 7, 0, 0,
    31, 8, 0, 4, 15, 11, 1, 2, 3, 0, 1,
    //143:Steel Drum,スチールドラム
    4, 15, 2, 0, 208, 6, 0, 6, 0, 3, 0,
    13, 8, 0, 4, 15, 25, 1, 3, 0, 0, 0,
    18, 8, 0, 4, 15, 3, 2, 1, 7, 0, 1,
    14, 8, 0, 4, 15, 31, 1, 2, 7, 0, 0,
    16, 8, 0, 4, 15, 17, 2, 2, 3, 0, 1,
    //144:Glockenspiel,グロッケンシュピール
    19, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 24, 0, 12, 15, 67, 0, 10, 2, 1, 0,
    31, 16, 0, 8, 15, 27, 0, 6, 0, 3, 0,
    31, 12, 0, 4, 15, 51, 0, 4, 0, 0, 0,
    31, 10, 0, 5, 15, 0, 0, 2, 0, 0, 1,
    //145:Vibraphone 1,ビブラフォン 1
    44, 15, 2, 0, 196, 6, 16, 5, 3, 3, 0,
    24, 14, 0, 7, 15, 57, 1, 12, 3, 0, 0,
    24, 10, 0, 7, 15, 0, 1, 4, 0, 0, 1,
    26, 14, 0, 6, 15, 57, 1, 4, 0, 0, 0,
    26, 8, 0, 6, 15, 5, 2, 1, 0, 0, 1,
    //146:Vibraphone 2,ビブラフォン 2
    3, 15, 2, 0, 200, 0, 24, 0, 1, 3, 0,
    26, 14, 0, 7, 15, 47, 0, 10, 0, 0, 0,
    24, 14, 0, 4, 15, 57, 2, 9, 0, 0, 0,
    30, 10, 0, 0, 0, 71, 0, 3, 3, 0, 0,
    24, 8, 0, 5, 15, 0, 2, 1, 0, 0, 1,
    //147:Claves,クラベス
    2, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 20, 0, 10, 15, 51, 0, 2, 0, 3, 0,
    31, 24, 0, 10, 15, 17, 0, 4, 0, 1, 0,
    31, 16, 0, 10, 15, 67, 0, 4, 0, 0, 0,
    30, 18, 0, 10, 15, 0, 0, 2, 0, 0, 1,
    //148:Wood Block,ウッドブロック
    27, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 18, 0, 10, 15, 19, 0, 2, 0, 2, 0,
    31, 26, 0, 10, 15, 31, 0, 2, 3, 0, 0,
    31, 22, 0, 10, 15, 47, 0, 2, 0, 3, 0,
    30, 20, 0, 10, 15, 0, 0, 2, 7, 0, 1,
    //149:Mokugyo,木魚
    60, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    30, 26, 0, 10, 15, 41, 0, 6, 0, 2, 0,
    28, 20, 0, 10, 15, 0, 0, 8, 0, 0, 1,
    28, 20, 0, 10, 15, 57, 0, 15, 0, 0, 0,
    24, 19, 0, 9, 15, 47, 1, 9, 7, 0, 1,
    //150:Castanets,カスタネット
    4, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    30, 24, 0, 10, 15, 9, 0, 5, 0, 3, 0,
    28, 20, 0, 10, 15, 0, 0, 3, 0, 0, 1,
    21, 26, 0, 10, 15, 3, 0, 9, 0, 3, 0,
    27, 20, 0, 10, 15, 0, 0, 2, 0, 2, 1,
    //151:Guiro,ギロ
    2, 15, 1, 0, 248, 0, 80, 0, 3, 3, 0,
    20, 0, 0, 10, 0, 27, 0, 2, 0, 2, 0,
    31, 0, 0, 10, 0, 17, 0, 1, 0, 0, 0,
    31, 0, 0, 10, 0, 33, 0, 3, 0, 3, 0,
    16, 8, 20, 12, 1, 0, 0, 15, 0, 1, 1,
    //152:Xylophone,シロホン
    4, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 18, 0, 10, 15, 33, 0, 6, 0, 0, 0,
    31, 22, 0, 10, 15, 0, 0, 3, 0, 0, 1,
    31, 24, 0, 8, 15, 47, 0, 4, 0, 0, 0,
    31, 16, 0, 8, 15, 7, 0, 1, 0, 0, 1,
    //153:Marimba,マリンバ
    44, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    24, 14, 0, 7, 15, 33, 1, 4, 3, 0, 0,
    24, 4, 0, 2, 15, 0, 3, 0, 3, 0, 1,
    24, 20, 0, 10, 15, 47, 1, 6, 7, 0, 0,
    24, 12, 0, 6, 15, 11, 2, 2, 7, 0, 1,
    //154:Maracas,マラカス
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 2, 0, 0, 0, 14, 0, 0, 0,
    31, 0, 0, 2, 0, 0, 0, 12, 0, 0, 0,
    31, 0, 0, 2, 0, 0, 0, 10, 0, 0, 0,
    16, 20, 0, 10, 15, 3, 2, 1, 0, 0, 1,
    //155:Shaker,シェイカー
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 2, 0, 0, 0, 14, 0, 0, 0,
    31, 0, 0, 2, 0, 0, 0, 9, 0, 0, 0,
    31, 0, 0, 2, 0, 0, 0, 5, 0, 0, 0,
    16, 18, 0, 9, 15, 9, 0, 1, 0, 0, 1,
    //156:Hand Clap,手拍子
    56, 15, 2, 1, 244, 3, 0, 7, 0, 3, 0,
    26, 10, 0, 5, 0, 41, 0, 0, 0, 0, 0,
    28, 18, 0, 10, 15, 17, 0, 0, 0, 0, 0,
    22, 10, 0, 6, 15, 9, 0, 1, 0, 0, 0,
    26, 20, 0, 10, 15, 0, 0, 8, 0, 0, 1,
    //157:Closed Hi-Hat,クローズハイハット
    59, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    26, 4, 0, 2, 3, 15, 0, 14, 0, 1, 0,
    26, 8, 0, 2, 7, 27, 0, 6, 0, 1, 0,
    26, 22, 0, 10, 11, 17, 0, 7, 0, 2, 0,
    22, 18, 0, 8, 15, 5, 0, 0, 0, 0, 1,
    //158:Open Hi-Hat,オープンハイハット
    51, 15, 2, 0, 200, 80, 0, 3, 0, 3, 0,
    26, 0, 0, 10, 0, 17, 0, 1, 7, 1, 0,
    26, 4, 0, 12, 2, 8, 0, 4, 0, 3, 0,
    20, 18, 1, 12, 3, 21, 0, 1, 0, 2, 0,
    23, 11, 12, 14, 4, 17, 0, 1, 3, 0, 1,
    //159:Ride Cymbal,ライドシンバル
    59, 15, 2, 0, 200, 60, 0, 3, 0, 3, 0,
    30, 4, 0, 2, 15, 22, 0, 1, 7, 1, 0,
    30, 2, 0, 1, 15, 25, 0, 4, 0, 2, 0,
    31, 8, 0, 4, 15, 35, 0, 9, 0, 2, 0,
    28, 12, 0, 6, 15, 17, 0, 1, 3, 0, 1,
    //160:Gong,ゴング
    2, 15, 2, 0, 200, 7, 0, 7, 0, 3, 0,
    9, 6, 0, 1, 15, 37, 2, 1, 7, 1, 0,
    10, 0, 0, 1, 0, 35, 1, 3, 3, 1, 0,
    2, 0, 0, 1, 0, 17, 2, 1, 0, 2, 0,
    12, 2, 4, 2, 0, 0, 2, 0, 0, 0, 1,
    //161:Synth Lead 1,シンセリード 1
    26, 15, 2, 0, 200, 9, 3, 5, 2, 3, 0,
    20, 1, 1, 10, 3, 7, 1, 2, 1, 0, 0,
    20, 2, 1, 10, 0, 0, 0, 3, 0, 1, 0,
    31, 20, 1, 0, 15, 27, 0, 0, 0, 0, 0,
    20, 2, 1, 10, 3, 16, 2, 1, 3, 0, 1,
    //162:Synth Lead 2,シンセリード 2
    28, 15, 2, 1, 210, 5, 0, 6, 0, 3, 0,
    31, 16, 0, 0, 15, 0, 1, 2, 3, 0, 0,
    31, 0, 0, 8, 0, 16, 1, 1, 6, 0, 1,
    31, 0, 0, 8, 0, 17, 1, 2, 7, 0, 0,
    31, 0, 0, 8, 0, 16, 1, 1, 3, 0, 1,
    //163:Synth Lead 3,シンセリード 3
    60, 15, 2, 0, 203, 8, 0, 6, 0, 3, 0,
    31, 8, 0, 10, 2, 22, 0, 1, 0, 0, 0,
    20, 0, 0, 10, 0, 9, 0, 0, 0, 0, 1,
    17, 0, 0, 10, 0, 27, 0, 1, 0, 0, 0,
    20, 2, 1, 10, 3, 0, 0, 0, 0, 0, 1,
    //164:Synth Lead 4,シンセリード 4
    61, 15, 2, 0, 195, 4, 0, 6, 0, 3, 0,
    25, 13, 0, 10, 5, 24, 0, 12, 0, 0, 0,
    28, 0, 0, 10, 0, 0, 0, 6, 0, 0, 1,
    28, 2, 1, 10, 3, 7, 0, 8, 0, 0, 1,
    28, 0, 0, 10, 0, 7, 0, 9, 0, 0, 1,
    //165:Synth Lead 5,シンセリード 5
    56, 15, 2, 0, 204, 5, 0, 6, 0, 3, 0,
    28, 0, 0, 4, 0, 29, 0, 7, 6, 0, 0,
    28, 0, 4, 4, 0, 31, 0, 3, 4, 0, 0,
    28, 0, 6, 4, 0, 27, 0, 1, 4, 0, 0,
    24, 14, 4, 8, 1, 0, 0, 2, 4, 0, 0,
    //166:Synth Lead 6,シンセリード 6
    62, 15, 2, 0, 201, 6, 0, 6, 0, 3, 0,
    13, 15, 0, 10, 2, 19, 0, 2, 3, 0, 0,
    31, 10, 0, 8, 5, 27, 0, 2, 3, 0, 0,
    20, 0, 0, 8, 0, 47, 0, 1, 6, 0, 0,
    20, 0, 4, 8, 0, 0, 0, 2, 4, 0, 1,
    //167:Synth Lead 7,シンセリード 7
    60, 15, 2, 0, 204, 6, 0, 5, 0, 3, 0,
    31, 10, 6, 3, 5, 29, 0, 8, 3, 0, 0,
    26, 12, 6, 6, 3, 0, 0, 2, 7, 0, 0,
    31, 0, 0, 3, 3, 31, 0, 2, 3, 3, 0,
    20, 12, 6, 8, 2, 0, 0, 0, 3, 0, 1,
    //168:Synth Brass 1,シンセブラス 1
    59, 15, 2, 0, 200, 0, 0, 3, 0, 3, 0,
    20, 5, 1, 12, 3, 29, 1, 1, 1, 0, 0,
    20, 24, 0, 8, 2, 17, 1, 1, 2, 0, 0,
    31, 24, 0, 0, 0, 32, 0, 0, 0, 0, 0,
    20, 0, 0, 9, 0, 0, 0, 1, 7, 0, 0,
    //169:Synth Brass 2,シンセブラス 2
    61, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    18, 6, 6, 15, 6, 22, 0, 1, 0, 0, 0,
    20, 12, 12, 15, 6, 0, 0, 1, 2, 0, 0,
    20, 12, 12, 15, 6, 0, 0, 3, 0, 0, 0,
    20, 12, 12, 15, 5, 0, 0, 2, 6, 0, 0,
    //170:Synth Brass 3,シンセブラス 3
    28, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    11, 8, 1, 10, 5, 18, 1, 0, 3, 0, 0,
    15, 10, 10, 10, 5, 0, 0, 1, 3, 0, 1,
    15, 10, 1, 10, 5, 27, 1, 0, 7, 0, 0,
    15, 2, 10, 10, 5, 7, 0, 0, 3, 0, 1,
    //171:Synth Clavinet 1,シンセクラビネット 1
    61, 15, 2, 0, 200, 3, 2, 2, 1, 3, 0,
    31, 10, 0, 15, 13, 22, 0, 6, 0, 0, 0,
    31, 12, 5, 15, 13, 0, 0, 1, 2, 0, 1,
    31, 12, 5, 15, 13, 0, 0, 3, 0, 0, 1,
    31, 12, 5, 15, 13, 0, 0, 2, 6, 0, 1,
    //172:Synth Clavinet 2,シンセクラビネット 2
    56, 15, 2, 1, 200, 4, 0, 6, 0, 3, 0,
    31, 31, 10, 5, 15, 0, 0, 1, 7, 0, 0,
    31, 5, 10, 5, 5, 22, 2, 1, 1, 0, 0,
    29, 4, 0, 5, 5, 18, 1, 1, 7, 0, 0,
    25, 10, 5, 8, 5, 0, 0, 1, 3, 0, 1,
    //173:Synth Bass 1,シンセベース 1
    44, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    21, 10, 0, 4, 15, 22, 1, 0, 7, 0, 0,
    31, 10, 0, 8, 3, 0, 1, 0, 1, 0, 1,
    21, 14, 0, 4, 15, 7, 2, 0, 7, 0, 0,
    31, 10, 0, 8, 3, 0, 0, 0, 0, 0, 1,
    //174:Synth Bass 2,シンセベース 2
    61, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    20, 10, 0, 10, 2, 35, 0, 3, 4, 0, 0,
    26, 0, 0, 10, 0, 0, 0, 1, 7, 0, 0,
    26, 0, 0, 10, 0, 0, 0, 1, 4, 0, 0,
    26, 0, 0, 10, 0, 0, 0, 2, 4, 0, 0,
    //175:Synth Bass 3,シンセベース 3
    3, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    24, 8, 0, 10, 3, 27, 0, 0, 3, 0, 0,
    26, 8, 0, 10, 15, 7, 0, 0, 4, 0, 0,
    26, 8, 0, 10, 15, 37, 0, 8, 7, 0, 0,
    26, 0, 4, 10, 0, 0, 0, 1, 4, 0, 0,
    //176:Synth Bass 4,シンセベース 4
    61, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    27, 17, 0, 10, 15, 35, 0, 10, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 0, 2, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 0, 1, 4, 0, 0,
    20, 0, 0, 10, 0, 0, 0, 0, 4, 0, 0,
    //177:Synth Drum 1,シンセドラム 1
    59, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    22, 0, 0, 10, 0, 13, 0, 10, 0, 0, 0,
    26, 26, 0, 10, 15, 19, 0, 13, 0, 3, 0,
    26, 22, 0, 11, 15, 11, 0, 0, 0, 1, 0,
    30, 14, 0, 7, 15, 0, 1, 1, 0, 0, 1,
    //178:Synth Drum 2,シンセドラム 2
    59, 15, 0, 1, 176, 80, 0, 7, 0, 3, 0,
    31, 0, 0, 5, 0, 12, 0, 14, 0, 0, 0,
    31, 10, 0, 5, 15, 49, 0, 0, 0, 3, 0,
    27, 27, 0, 10, 15, 37, 0, 10, 0, 2, 0,
    28, 14, 0, 7, 15, 0, 1, 0, 0, 1, 1,
    //179:Bell and Flute,ベルとフルート
    36, 15, 2, 0, 198, 6, 20, 7, 1, 3, 0,
    31, 16, 0, 8, 15, 27, 1, 12, 0, 3, 0,
    31, 10, 0, 5, 15, 17, 1, 4, 0, 0, 1,
    20, 0, 0, 10, 0, 37, 0, 1, 0, 0, 0,
    14, 8, 0, 8, 1, 0, 1, 1, 0, 0, 1,
    //180:Bell and Brass,ベルとブラス
    60, 15, 2, 0, 194, 4, 0, 6, 0, 3, 0,
    16, 12, 0, 10, 1, 29, 1, 1, 0, 0, 0,
    18, 10, 0, 8, 2, 0, 1, 1, 0, 0, 1,
    31, 16, 0, 8, 15, 17, 1, 12, 0, 3, 0,
    31, 10, 0, 5, 15, 9, 1, 4, 0, 0, 1,
    //181:Electric Piano and Strings,エレクトリックピアノとストリングス
    60, 15, 2, 0, 198, 7, 0, 6, 0, 3, 0,
    20, 0, 0, 2, 0, 32, 0, 1, 6, 0, 0,
    8, 0, 0, 6, 0, 17, 1, 1, 3, 0, 1,
    28, 12, 1, 4, 12, 51, 1, 12, 0, 0, 0,
    26, 1, 1, 7, 2, 0, 2, 1, 3, 0, 1,
    //182:Bird 1,鳥 1
    4, 15, 2, 1, 210, 120, 0, 6, 0, 3, 0,
    17, 0, 0, 10, 0, 57, 0, 2, 0, 0, 0,
    16, 18, 0, 10, 15, 10, 0, 2, 0, 0, 1,
    15, 0, 0, 10, 0, 57, 0, 2, 2, 0, 0,
    17, 17, 0, 10, 15, 10, 0, 2, 7, 0, 1,
    //183:Bird 2,鳥 2
    3, 15, 2, 1, 214, 127, 0, 7, 0, 3, 0,
    20, 24, 0, 10, 15, 37, 0, 8, 0, 0, 0,
    18, 24, 0, 10, 15, 43, 0, 3, 0, 0, 0,
    20, 10, 0, 5, 15, 19, 0, 0, 0, 0, 0,
    18, 19, 0, 9, 15, 0, 0, 12, 0, 0, 1,
    //184:Bell Cricket,鈴虫
    7, 15, 2, 0, 252, 125, 60, 5, 2, 3, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 1,
    0, 0, 0, 0, 15, 127, 0, 5, 0, 0, 1,
    16, 0, 0, 10, 0, 0, 0, 1, 3, 0, 1,
    16, 0, 0, 10, 0, 0, 0, 1, 5, 0, 1,
    //185:Cicada,セミ
    4, 15, 2, 0, 224, 100, 0, 5, 0, 3, 0,
    20, 0, 0, 10, 0, 17, 0, 0, 0, 3, 0,
    20, 0, 0, 10, 0, 0, 0, 12, 0, 2, 1,
    20, 0, 0, 10, 0, 10, 0, 0, 0, 1, 0,
    20, 0, 0, 10, 0, 0, 0, 14, 0, 3, 1,
    //186:Telephone,電話
    4, 15, 1, 0, 134, 0, 100, 0, 3, 3, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 1,
    31, 0, 0, 10, 0, 35, 0, 14, 7, 0, 0,
    20, 0, 0, 10, 0, 0, 0, 5, 3, 1, 1,
    //187:Alarm,目覚まし
    60, 15, 1, 0, 230, 0, 18, 0, 3, 3, 0,
    31, 0, 0, 15, 0, 37, 0, 5, 0, 0, 0,
    31, 0, 0, 15, 0, 17, 0, 1, 0, 0, 1,
    31, 0, 0, 15, 2, 24, 0, 5, 0, 0, 0,
    31, 0, 0, 15, 2, 10, 0, 1, 0, 0, 1,
    //188:Ambulance,救急車
    4, 15, 1, 0, 153, 68, 0, 6, 0, 3, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 1,
    31, 0, 0, 1, 0, 37, 0, 14, 0, 0, 0,
    16, 0, 0, 4, 0, 0, 0, 5, 7, 1, 1,
    //189:Patrol Car,パトカー
    0, 15, 2, 0, 130, 120, 0, 7, 0, 3, 0,
    0, 0, 0, 0, 15, 127, 0, 1, 0, 0, 0,
    31, 0, 0, 1, 0, 31, 0, 1, 0, 0, 0,
    31, 0, 0, 1, 0, 23, 0, 1, 0, 0, 0,
    16, 0, 0, 8, 0, 0, 0, 1, 0, 0, 1,
    //190:Storm,嵐
    58, 15, 2, 0, 120, 120, 30, 7, 2, 3, 0,
    31, 0, 0, 0, 0, 17, 0, 2, 0, 2, 0,
    31, 0, 0, 0, 0, 10, 0, 1, 0, 1, 0,
    31, 0, 0, 0, 0, 29, 0, 1, 0, 2, 0,
    12, 0, 0, 4, 0, 0, 0, 0, 0, 0, 1,
    //191:Wave,波
    58, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 0, 0, 0, 0, 12, 0, 0, 0, 1, 0,
    31, 0, 0, 0, 0, 17, 0, 12, 0, 2, 0,
    31, 0, 0, 0, 0, 12, 0, 5, 0, 3, 0,
    1, 0, 4, 2, 0, 0, 3, 4, 0, 0, 1,
    //192:Laser Gun,レーザーガン
    4, 15, 0, 1, 212, 120, 0, 7, 0, 3, 0,
    31, 0, 0, 5, 0, 15, 0, 0, 0, 3, 0,
    20, 0, 0, 10, 0, 7, 0, 7, 0, 1, 1,
    12, 0, 0, 5, 0, 47, 0, 3, 0, 3, 0,
    16, 0, 0, 8, 0, 0, 0, 1, 0, 0, 1,
    //193:Foot Step,足音
    3, 15, 3, 0, 210, 80, 0, 7, 0, 3, 0,
    24, 22, 0, 11, 15, 10, 0, 1, 0, 1, 0,
    31, 10, 0, 5, 15, 37, 0, 6, 0, 3, 0,
    31, 0, 0, 0, 0, 51, 0, 13, 0, 3, 0,
    28, 13, 0, 6, 15, 0, 0, 3, 0, 2, 1,
    //194:Game Sound Effect 1,ゲーム効果音 1
    6, 15, 3, 0, 200, 80, 0, 6, 0, 3, 0,
    31, 0, 0, 0, 0, 67, 0, 12, 0, 0, 0,
    20, 14, 0, 7, 15, 7, 0, 4, 0, 0, 0,
    20, 14, 0, 7, 15, 17, 0, 2, 4, 3, 0,
    20, 14, 0, 7, 15, 0, 0, 2, 4, 0, 0,
    //195:Game Sound Effect 2,ゲーム効果音 2
    32, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    31, 8, 0, 4, 15, 13, 0, 3, 0, 2, 0,
    10, 10, 0, 4, 15, 17, 3, 1, 0, 1, 0,
    31, 0, 0, 0, 0, 3, 0, 1, 0, 2, 0,
    16, 10, 0, 5, 15, 0, 3, 0, 0, 0, 1,
    //196:Game Sound Effect 3,ゲーム効果音 3
    4, 15, 3, 0, 130, 120, 0, 7, 0, 3, 0,
    31, 0, 0, 4, 0, 17, 0, 14, 0, 2, 0,
    16, 0, 0, 8, 0, 0, 0, 8, 0, 0, 1,
    31, 0, 0, 4, 0, 37, 0, 3, 0, 2, 0,
    16, 0, 0, 7, 0, 0, 0, 1, 0, 0, 1,
    //197:Picnic,ピクニック
    4, 15, 2, 1, 190, 20, 0, 2, 0, 3, 0,
    18, 14, 0, 7, 15, 19, 0, 12, 2, 0, 0,
    21, 12, 10, 6, 7, 17, 0, 1, 4, 3, 0,
    12, 14, 0, 7, 15, 17, 0, 10, 7, 0, 0,
    26, 12, 10, 6, 7, 0, 0, 2, 4, 0, 1,
    //198:Mandara,マンダラ
    4, 15, 3, 0, 216, 0, 0, 1, 0, 3, 0,
    31, 11, 6, 0, 1, 14, 0, 4, 4, 3, 0,
    31, 13, 9, 3, 5, 36, 1, 1, 5, 0, 1,
    11, 5, 3, 0, 1, 13, 1, 15, 4, 2, 0,
    8, 7, 6, 3, 3, 29, 1, 5, 4, 0, 1,
    //199:Asphalt,アスファルト
    4, 15, 2, 0, 250, 100, 100, 1, 2, 3, 0,
    10, 10, 0, 15, 0, 37, 0, 1, 4, 0, 0,
    8, 5, 10, 10, 1, 0, 0, 15, 4, 3, 0,
    31, 31, 0, 1, 0, 12, 3, 0, 7, 0, 1,
    31, 31, 0, 2, 0, 0, 3, 0, 3, 0, 1,
    //200:Sine Wave,サイン波
    7, 15, 2, 0, 200, 0, 0, 0, 0, 3, 0,
    0, 0, 0, 0, 0, 127, 0, 1, 4, 0, 0,
    0, 0, 0, 0, 0, 127, 0, 1, 4, 0, 0,
    0, 0, 0, 0, 0, 127, 0, 1, 4, 0, 0,
    31, 31, 0, 15, 0, 0, 0, 1, 4, 0, 0,
  };
*/
  //perl ../misc/itob.pl MMLCompiler.java TONE_DATA_X1
  public static final byte[] TONE_DATA_X1 = ":\17\2\1\334\0\4\1\1\3\0\37\5\7\4\t%\1\1\5\0\0\26\0\4\5\4>\1\5\2\0\0\35\0\4\5\4M\1\1\7\0\0\37\7\6\5\4\0\2\1\1\0\1\34\17\2\0\264\0\1\0\1\3\0\37\24\b\n\0\30\0\1\3\0\0\37\n\5\n\0\0\0\1\7\0\1\37\24\b\n\0-\0\3\7\0\0\31\n\5\n\0\0\3\1\3\0\1:\17\2\0\315\0\0\0\0\2\0\23\2\1\4\3!\3\5\4\0\0\23\2\1\4\3\31\3\5\2\0\0\23\2\1\4\3\37\2\1\7\0\0\23\2\1\4\3\0\3\1\4\0\1\34\17\2\0\334\0\n\0\0\3\0\37\n\1\3\17\30\2\7\3\0\0\35\f\t\7\n\0\0\7\7\0\1\37\5\1\3\17#\2\5\7\1\0\34\f\t\7\n\0\0\7\3\0\1,\17\2\0\264\n\2\5\3\3\0\31\24\0\6\7C\2\n\3\1\0\30\n\5\b\2\0\2\1\2\0\1\32\7\3\6\4/\3\n\0\0\0\30\f\5\b\2\0\1\1\0\0\1\34\17\2\0\310\2\2\2\1\3\0\37\n\0\n\5/\0\17\3\3\0\33\b\4\6\139\2\5\0\0\1\36\6\13\6\17!\2\1\3\0\0\36\6\13\6\17\0\1\1\3\0\1<\17\2\1\276\0\2\0\3\3\0\37\n\0\2\179\2\7\3\1\0\37\n\5\5\2\33\2\1\2\0\1\37\7\3\4\4/\3\n\7\0\0\37\f\5\6\1\0\1\1\3\0\1:\17\2\0\275\5\5\4\1\3\0\34\4\3\7\1&\2\1\3\0\0\33\t\1\2\09\3\7\7\3\0\34\4\3\6\0-\2\5\6\0\0\32\2\0\5\17\0\3\2\3\0\1<\17\2\0\310\0\0\0\0\3\0\37\2\24\n\0\21\1\0\7\0\0\37\n\2\3\0\33\2\2\3\0\1\37\2\17\n\0 \1\f\7\0\0\37\n\r\5\5\0\1\2\3\0\1:\17\2\0\310\0\0\0\0\3\0\34\4\3\7\1#\2\1\3\0\0\33\t\1\2\0%\3\17\7\0\0\34\3\0\0\17\33\2\1\6\0\0\32\6\0\n\17\0\3\n\0\0\1:\17\2\0\202\n\0\3\3\3\0\34\4\3\7\1/\2\b\3\0\0\33\5\5\2\3/\3\17\7\0\0\37\5\5\0\17\21\2\2\6\0\0\32\7\2\n\17\0\3\n\0\0\1<\17\2\0\202\n\0\3\3\3\0\34\4\3\7\1 \2\2\3\0\0\33\5\5\n\3\0\3\17\7\0\1\37\2\0\0\17\21\2\1\6\0\0\32\5\5\n\17\0\3\n\3\0\1\r\17\2\0\310\0\0\0\0\1\0\37\n\f\7\17n\1\16\6\0\0\37\n\f\7\17 \1\4\6\0\0\37\n\f\7\17 \1\f\6\0\0\37\n\f\7\17 \1\t\6\0\0?\17\2\0\310\0\0\0\0\3\0\37\n\f\6\0174\1\1\0\0\1\37\b\f\6\17%\1\0\0\0\1\37\n\f\6\17\33\1\4\0\0\1\37\n\f\6\17/\1\2\0\0\1\4\17\2\0\310\0\0\0\0\3\0\37\4\0\2\0\7\3\4\5\0\0\37\b\1\b\17\0\1\2\0\0\1\37\4\0\2\0\6\0\3\5\0\0\37\b\1\b\17\0\0\1\0\0\1$\17\2\0\310\0\0\0\0\3\0\0\0\0\0\0\177\0\0\4\0\0\0\0\0\0\0\177\0\0\0\0\0\37\4\0\2\0\3\3\3\7\0\0\37\r\f\b\17\0\0\1\0\0\0\2\17\2\0\310\0\0\0\0\3\0\30\n\0\5\179\1\f\1\0\0\24\f\b\4\1%\1\6\7\0\0\35\n\4\4\1%\1\3\4\0\0\22\22\6\7\1\0\2\1\2\0\0:\17\2\1\264\3\0\5\0\3\0\37\n\1\2\3%\1\1\2\0\0\37\n\37\3\n \1\16\1\1\0\37\n\n\3\5W\0\3\1\0\0\37\22\f\7\6\0\0\1\7\0\19\17\2\0\310\0\0\0\0\3\0\37\26\b\6\7\13\2\f\6\0\0\37\6\0\6\3!\1\3\3\0\0\34\6\0\6\17 \0\3\4\0\0\37\b\0\b\17\0\0\1\4\0\0<\17\2\0\310\0\0\0\0\3\0\35\b\0\6\17\33\1\3\7\0\0\26\b\0\6\17\7\1\1\0\0\0\32\b\0\4\17\17\1\6\3\0\0\30\n\0\7\17\0\1\b\2\0\0:\17\2\0\322\6\2\6\1\3\0\37\r\1\4\17%\2\1\3\0\0\37\24\1\n\179\1\r\7\2\0\24\n\1\7\17%\1\3\7\0\0\27\5\1\7\17\0\0\1\3\0\1=\17\2\0\317\6\0\5\0\3\0\34\2\1\n\17\27\2\2\0\0\0\37\0\1\n\0\0\0\1\0\0\1\37\0\1\n\0\0\0\1\0\0\1\6\0\1\n\0\0\0\b\0\0\1\2\17\2\0\310\0\0\0\0\3\0\36\24\0\n\17-\0\6\0\0\0\22\24\0\n\7!\1\4\0\0\0\37\16\0\n\17\'\1\0\0\0\0\34\16\0\7\17\0\2\1\4\0\0\2\17\2\0\310\0\0\0\0\3\0\34\0\0\n\09\0\2\7\0\0\37\22\0\n\2!\1\b\7\0\0\32\20\6\n\2\35\1\0\4\0\0\34\6\0\b\17\0\1\1\4\0\0\21\17\2\0\322\7\0\5\0\3\0\37\0\4\2\0\3\0\3\3\0\0\37\0\0\2\0\t\0\0\2\0\0\32\0\0\2\0\37\0\b\4\0\0\24\0\4\6\0\0\1\0\4\0\1:\17\2\0\226\0\n\0\1\3\0\37\f\1\4\17!\1\0\7\0\0\37\n\1\n\179\1\4\5\0\0\37\n\1\n\17\33\0\0\2\0\0\37\n\1\b\17\t\1\0\3\0\1:\17\2\0\226\0\n\0\1\3\0\33\22\1\4\17\35\1\0\7\0\0\37\n\1\3\17*\1\3\5\0\0\37\n\1\3\17 \0\0\2\0\0\35\f\1\6\17\0\1\0\3\0\1\3\17\2\0\310\0\0\0\0\3\0\37\f\0\n\17/\0\5\6\0\0\37\0\0\n\0\27\0\0\4\0\0\37\0\4\6\0!\0\0\4\0\0\34\0\6\b\0\0\0\0\3\0\1<\17\2\0\310\0\0\0\0\3\0\37\2\24\0\0\27\1\1\0\0\0\37\2\n\6\0\0\1\1\3\0\1\37\2\n\4\0\17\2\0\0\0\0\24\2\n\5\0\0\1\0\0\0\1 \17\2\0\310\0\0\0\0\3\0\37\7\7\t\2\35\3\6\4\0\0\37\6\6\t\1/\3\5\4\0\0\32\t\6\t\1\35\2\0\4\0\0\37\b\4\t\3\0\2\1\4\0\1\33\17\2\0\310\0\0\0\0\3\0\37\25\0\b\17\0\0\6\4\0\0\37\17\0\b\17#\0\t\7\0\0\37\0\0\6\0%\0\0\4\0\0\37\b\0\n\17\0\0\1\0\0\0\21\17\2\0\334\5\0\5\0\3\0\37\0\0\4\0\21\0\3\3\0\0\37\0\0\4\0\r\0\0\5\0\0\32\0\0\4\0\37\0\2\4\0\0\24\0\3\6\0\0\0\0\4\0\0\3\17\2\0\310\0\0\0\0\3\0\34\26\0\n\17\33\0\b\0\0\0\37\6\0\3\3\23\0\4\4\0\0\37\b\0\4\3\27\0\5\6\0\0\30\f\0\6\17\0\1\1\3\0\0\1\17\2\0\310\0\0\0\0\3\0\32\20\0\6\0173\1\t\4\0\0\37\n\0\4\17)\1\3\3\0\0\37\n\0\6\17%\1\3\7\0\0\30\f\0\7\17\0\1\1\6\0\0:\17\2\0\310\0\0\0\0\3\0\30\n\0\2\5\31\1\5\7\0\0\32\20\0\b\13\35\0\17\0\0\0\34\20\0\4\3\37\0\1\6\0\0\30\13\0\6\17\0\2\1\3\0\0\2\17\2\0d\n\n\1\2\3\0\37\37\r\3\1\21\0\7\2\0\0\37\17\1\n\3\33\1\t\3\0\0\37\17\n\3\3\33\0\1\7\0\0\24\2\1\4\3\7\1\1\3\0\19\17\2\0\310\0\0\0\0\3\0\24\36\1\5\17/\1\6\0\0\0\24\n\1\5\17/\2\4\0\0\0\24\5\1\5\179\1\2\7\0\0\35\n\1\5\17\0\1\2\0\0\1\0\17\2\0\310\0\0\0\0\3\0\0\0\0\0\17\177\0\1\0\0\0\37\f\1\5\17\33\1\1\7\0\0\37\5\0\3\17#\1\1\0\0\0\37\n\0\4\17\7\1\1\0\0\19\17\2\0\310\0\0\0\0\3\0\37\f\0\4\17\26\0\2\0\0\0\37\r\0\6\1&\0\1\4\0\0\37\6\5\5\1,\0\2\0\0\0\37\f\7\5\1\0\0\1\0\0\1\0\17\2\1\310\5\0\5\0\3\0\37\n\2\5\r\33\0\3\7\0\0\37\n\2\5\n%\2\4\1\0\0\35\b\0\4\r\33\1\1\7\0\0\35\t\n\5\n\0\0\1\3\0\1>\17\2\0\310\b\1\3\2\3\0\37\24\0\n\0\30\0\6\3\0\0\24\2\1\n\3\0\0\2\7\0\1\24\2\1\n\3\0\0\1\1\0\1\24\2\1\n\3\0\0\6\2\0\1?\17\2\0\276\0\3\0\1\3\0\37\1\1\n\0u\0\b\3\0\1\24\2\1\n\0\0\0\3\7\0\1\24\2\1\n\0\0\0\1\0\0\1\24\2\1\n\0\0\0\2\6\0\0016\17\2\0\372\5\n\1\1\3\0\37\25\0\17\0*\3\3\7\0\0\35\37\0\n\0\33\1\b\1\0\1\37\37\0\n\0\0\1\1\6\0\1\22\37\0\n\0\0\2\4\3\0\1\27\17\2\0\303\5\0\4\0\3\0\20\0\0\n\0\0\0\2\7\0\1\22\2\1\n\3%\0\5\3\0\1\22\2\1\n\3\33\0\2\6\0\1\22\2\1\n\3\33\0\3\1\0\1>\17\2\0\310\b\1\3\2\3\0\37\24\0\n\0\33\0\f\3\0\1\24\2\1\n\3\0\0\b\7\0\1\24\2\1\n\3\0\0\0\1\0\1\24\2\1\n\3\0\0\2\2\0\1?\17\2\0\310\3\2\2\1\3\0\37\16\0\17\17k\0\6\0\0\1\37\2\1\17\0\0\0\1\2\0\1\37\2\1\17\0\0\0\3\0\0\1\37\2\1\17\0\0\0\2\6\0\1>\17\2\1\303\5\5\1\1\3\0\37\23\0\n\17/\0\3\7\0\0\37\2\1\n\3\0\1\f\3\0\1\37\0\0\n\0\0\1\1\7\0\1\37\0\0\n\0\0\1\3\3\0\1\7\17\2\0\276\n\2\2\1\3\0\37\22\0\17\17\7\0\6\0\0\1\37\2\1\17\3\0\0\2\2\0\1\37\2\1\17\3\0\0\3\0\0\1\37\2\1\17\3\0\0\1\6\0\0014\17\2\0\310\6\2\4\1\3\0\17\2\0\3\0\17\2\3\7\0\0\20\2\0\6\0\b\2\5\7\0\1\17\2\0\3\0\f\2\0\6\0\0\17\2\0\7\0\0\2\1\1\0\1\6\17\2\0\310\n\n\1\1\3\0\37\0\0\17\0\21\0\3\2\0\1\37\0\0\17\0\7\0\3\6\0\1\37\0\0\17\3\0\0\0\3\0\1\37\0\0\17\0\7\0\2\7\0\1>\17\2\1\276\n\0\1\1\3\0\37\0\0\17\0\36\0\0\3\0\0\37\0\0\17\0\0\0\0\7\0\1\37\0\0\17\0\0\0\3\2\0\1\37\0\0\17\0\0\0\2\3\0\1<\17\2\1\310\6\0\4\1\3\0\37\0\0\17\0%\0\0\3\0\0\37\0\0\17\0\0\0\0\7\0\1\37\0\0\17\0\30\0\3\1\0\0\37\0\0\17\0\0\0\2\3\0\1<\17\2\0\310\6\1\3\1\3\0\24\2\0\6\0 \3\2\3\0\0\t\2\1\n\3\0\3\2\3\0\1\22\n\0\6\0\26\3\2\3\0\0\t\0\0\b\0\0\3\2\0\0\1<\17\2\0\264\6\0\5\0\3\0\22\0\0\2\0\31\1\1\3\0\0\17\2\0\n\0\0\1\1\7\0\1\37\2\0\6\0\33\1\3\7\0\0\17\2\0\n\0\0\1\3\2\0\1<\17\2\0\264\5\0\5\0\3\0\22\2\1\2\0 \1\1\3\0\0\17\2\1\n\0\0\1\1\7\0\1\37\2\1\6\0\21\1\1\7\0\0\24\2\1\n\0\21\1\1\2\0\1\1\17\2\0\322\6\0\5\0\3\0\37\0\0\6\09\0\3\7\0\0\37\0\0\6\0001\0\4\6\0\0\37\0\0\6\0\23\0\0\2\0\0\16\0\0\n\0\0\0\1\0\0\0:\17\2\0\312\n\3\5\0\3\0\24\2\0\5\1#\1\1\0\0\0\31\6\0\b\3 \1\5\7\0\0\34\3\0\6\1/\1\1\0\0\0\f\4\0\6\0\f\1\1\4\0\1\30\17\2\0\310\6\0\6\0\3\0\21\n\22\n\0*\1\17\7\3\0\22\2\t\n\0%\1\6\7\0\0\22\5\1\3\0\21\2\1\7\0\0\f\2\1\7\1\0\1\1\3\0\1:\17\2\0\314\5\0\6\0\3\0\24\n\0\b\1\35\0\2\4\0\0\36\21\0\n\n\35\0\n\7\1\0\22\t\0\6\2\25\0\3\3\0\0\r\f\0\b\1\0\0\1\1\0\08\17\2\0\310\5\0\7\0\3\0\22\37\24\n\0\21\1\17\7\3\0\37\21\f\n\0%\1\6\7\0\0\r\22\1\3\0\21\2\1\7\0\0\f\2\1\n\1\0\1\1\3\0\18\17\2\0\276\5\0\6\0\3\0\17\37\37\n\2\26\1\17\7\3\0\25\34\f\n\2\26\1\6\4\0\0\17\22\0\3\0\26\2\1\7\0\0\n\2\1\b\0\0\0\1\3\0\08\17\2\0\310\6\0\6\0\3\0\22\37\24\n\0\33\1\17\7\3\0\17\21\f\n\0/\1\6\7\0\0\17\22\1\3\0\21\2\1\7\0\0\f\2\1\t\1\0\1\1\3\0\1:\17\2\0\315\n\0\5\0\3\0\36\1\0\1\1\26\3\0\2\0\0\37\1\0\5\1/\3\2\3\0\0\36\1\0\5\19\1\1\3\0\0\r\2\0\6\0\0\1\1\7\0\1:\17\2\0\310\6\0\6\0\3\0\36\1\0\1\1\35\3\0\2\0\0\37\1\0\5\1k\3\2\3\0\0\36\1\0\5\1a\1\1\3\0\0\r\2\0\6\0\0\1\1\7\0\1<\17\2\0\310\3\0\7\0\3\0\37\37\0\5\0\36\0\2\3\0\1\r\37\0\6\0\0\0\2\7\0\1\37\37\0\5\0\"\1\4\2\0\1\r\37\0\6\0\f\1\4\3\0\1=\17\2\0\312\6\0\7\0\3\0\37\0\0\4\0\35\0\1\4\0\0\n\0\0\6\0%\0\2\4\0\0\n\0\0\6\0#\0\1\7\0\0\n\0\0\6\0\0\0\1\4\0\0<\17\2\0\310\0\0\0\0\3\0\37\24\1\3\17\33\0\1\3\0\0\22\17\1\5\16\7\1\1\7\0\1\37\n\0\3\17%\1\1\3\0\0\37\17\1\5\16\7\1\1\3\0\18\17\2\0\310\0\0\0\0\3\0\37\24\1\3\17\33\0\0\3\0\0\22\17\1\6\16&\1\0\7\0\0\37\n\0\3\17%\1\1\3\0\0\37\17\1\6\16\7\1\1\3\0\1\6\17\2\0\310\t\0\5\0\3\0\n\0\1\3\0M\0\1\0\0\0\n\0\0\5\0\0\2\3\3\0\1\n\0\1\6\2\0\1\2\7\0\1\n\0\0\6\0\0\1\1\3\0\1\6\17\2\0\304\5\0\7\0\3\0\24\0\0\6\09\0\1\4\0\0\16\0\0\b\0\0\0\2\4\0\0\16\0\0\b\0{\0\5\4\2\0\16\0\0\b\0\0\0\3\4\0\0$\17\2\0\310\6\0\7\0\3\0\24\0\0\4\0\31\0\1\4\0\0\16\0\0\b\0\0\0\2\7\0\0\24\0\0\n\0 \0\1\4\0\0\16\0\0\n\0/\0\13\0\3\0\3\17\2\0\312\5\0\7\0\3\0\16\n\0\5\0013\0\17\3\3\0\20\n\0\5\2\35\0\1\3\0\0\17\n\0\5\0011\1\6\4\2\0\17\0\0\b\0\0\0\3\4\0\0)\17\2\0\313\5\0\6\0\3\0\23\22\4\4\5D\0\6\3\3\0\25\16\6\n\69\0\4\7\3\0\13\37\3\n\0/\0\1\7\0\0\16\37\1\b\0\0\0\1\3\0\1\3\17\2\0\310\5\0\7\0\3\0\20\0\0\4\0?\0\17\4\2\0\20\0\0\4\0\35\0\1\7\0\0\20\0\0\4\0;\0\4\7\0\0\17\0\0\b\0\0\0\1\7\0\0<\17\2\0\310\n\0\5\0\3\0\17\2\1\3\0/\2\7\7\3\0\n\2\1\5\3\7\2\7\3\3\1\24\2\1\3\3\24\1\4\3\0\0\n\2\1\5\3\0\2\4\7\0\1\4\17\2\0\306\5\0\7\0\3\0\24\0\0\2\0\'\0\1\4\0\0\n\0\0\6\0\0\0\1\4\0\0\24\0\0\2\0003\0\2\6\0\0\n\0\0\6\0\0\0\2\6\0\0\4\17\2\0\316\6\0\6\0\3\0\24\0\0\n\0(\0\1\4\0\0\22\0\0\n\0\0\0\3\4\0\0\24\0\0\n\0)\0\1\4\0\0\24\0\0\n\0\21\0\5\4\3\0$\17\0\0P\1\2\1\1\3\0\24\2\1\5\3 \1\4\0\0\0\b\7\7\5\0\0\1\0\0\1\1\24\2\1\5\3 \3\4\6\0\0\b\7\7\5\0\0\0\0\2\1\1<\17\0\0P\1\2\1\1\3\0\24\2\1\5\3\35\1\5\0\0\0\b\7\7\5\0\0\1\0\0\1\1\24\2\1\5\3\21\2\5\6\0\0\b\7\7\5\0\0\0\0\2\1\1\7\17\2\0\310\5\0\7\0\3\0\0\0\0\0\0\177\0\0\4\0\0\0\0\0\0\0\177\0\0\4\0\0\16\f\0\b\0\0\0\5\7\2\0\16\f\0\b\0\0\0\b\7\0\1\4\17\2\0\310\0\0\0\0\3\0\24\n\1\n\3C\1\1\4\0\0\24\13\3\t\2\n\0\1\4\0\0\24\n\1\n\5R\1\3\4\3\0\24\13\3\t\2\21\0\1\4\0\0;\17\2\0\304\5\13\6\3\3\0\37\5\3\5\0167\3\2\7\1\0\f\7\0\5\179\1\2\0\0\0\17\2\0\4\0027\3\1\3\0\0\f\20\0\6\1\0\2\1\0\0\1;\17\2\0\313\n&\5\0\3\0\37\24\23\t\5\34\1\3\4\1\0\37\21\0\6\2/\0\4\4\0\0\31\24\0\5\7-\0\2\4\0\0\20\37\0\13\0\0\1\2\4\0\1;\17\2\0\304\t\24\5\1\3\0\37\0\0\n\0\0\0\17\0\3\0\n\6\0\n\2Q\2\f\0\3\0\24\0\0\6\0\'\1\1\3\0\0\n\6\0\6\1\0\2\1\5\0\1:\17\2\0\306\7\b\6\1\3\0\37\0\0\6\0\'\3\1\3\0\0\34\f\f\13\5\'\3\t\3\0\0\34\20\0\5\29\1\2\3\0\0\16\20\0\b\1\0\1\4\3\0\1\22\17\2\0\310\0\0\0\0\3\0\22\24\0\n\t/\0\6\4\0\0\24\0\0\6\0+\0\2\4\0\0\24\0\0\6\0\33\0\1\4\0\0\22\0\0\n\0\0\0\4\4\0\0\2\17\2\0\310\b\b\5\1\3\0\26\24\0\n\13\37\0\4\4\0\0\24\0\0\6\0\37\0\2\4\0\0\24\0\0\6\0\37\0\1\4\0\0\21\20\0\t\1\0\0\2\4\0\1:\17\2\0\306\t\24\4\1\3\0\23\31\0\n\2#\2\2\0\0\0\35\23\0\b\3\35\2\t\0\0\0\35\24\0\7\0015\0\1\0\0\0\21\37\0\t\0\21\1\1\0\0\1:\17\2\0\310\0\0\0\0\3\0\22\24\0\n\tG\0\t\4\2\0\24\0\0\6\0\'\0\2\4\0\0\24\0\0\6\0\31\0\2\4\0\0\22\0\0\n\0\0\0\1\4\0\0:\17\2\0\310\0\0\0\0\3\0\22\24\0\n\t+\0\t\0\2\0\24\0\0\6\0\35\0\2\0\0\0\24\0\0\6\0\21\0\2\0\0\0\20\0\0\n\0\0\0\1\0\0\1,\17\2\0\310\0\0\0\0\3\0\22\0\0\n\0%\0\1\4\0\0\24\0\0\n\0\0\1\2\4\0\0\23\16\0\n\1%\0\1\4\0\0\24\0\0\n\0\0\0\5\4\0\0\2\17\2\0\306\n\0\5\0\3\0\22\0\0\b\09\0\3\4\0\0\37\0\0\b\0k\0\b\4\0\0\22\0\0\b\0%\0\1\4\0\0\24\0\0\13\0\0\3\2\4\0\0:\17\2\0\310\t\0\5\0\3\0\22\0\0\6\0%\0\0\0\0\0\22\0\0\6\3I\0\4\0\1\0\22\0\0\6\0)\0\0\0\0\0\20\b\0\b\1\6\0\1\7\0\18\17\2\0\314\n\0\5\0\3\0\24\0\0\6\0\33\0\1\0\0\0\24\0\0\6\0\35\0\1\0\0\0\24\0\0\6\0%\0\2\0\0\0\20\0\0\b\0\0\1\1\0\0\1:\17\2\0\314\n\0\5\0\3\0\24\4\0\6\1\33\1\1\2\0\0\24\16\0\b\3-\1\6\0\2\0\24\0\0\6\0\'\1\1\7\0\0\20\0\0\n\0\0\1\2\0\0\0012\17\2\0\314\n\0\5\0\3\0\20\f\0\6\1\33\0\1\0\0\0\0\0\0\0\17\177\0\1\0\0\0\24\0\0\6\0003\0\1\0\0\0\22\0\0\n\0\0\0\1\0\0\1:\17\2\0\310\0\0\0\0\3\0\20\5\n\0\t\36\1\1\3\0\0\r\n\1\n\n%\3\2\2\0\0\17\n\0\n\1%\1\1\4\0\0\24\n\0\n\0\r\1\1\6\0\1:\17\2\0\314\t\n\5\1\3\0\20\16\0\b\0\33\1\1\0\0\0\17\f\0\n\17?\1\2\0\2\0\24\0\0\n\0/\0\1\0\0\0\20\0\0\n\0\0\1\1\0\0\0012\17\2\0\314\4\0\6\0\3\0\16\0\0\b\0\36\1\1\0\0\0\16\17\0\n\13C\0\4\0\2\0\24\0\0\b\0\'\0\2\0\0\0\22\n\0\n\1\0\0\1\0\0\1;\17\2\0\310\0\0\0\0\3\0\37\16\0\n\1\27\0\4\6\0\0\20\n\0\n\1\31\1\1\4\0\0\24\0\0\6\0\36\0\1\4\0\0\22\0\0\n\0\33\1\1\4\0\0:\17\2\0\310\0\0\0\0\3\0\16\t\0\t\2#\0\1\4\0\0\37\21\0\17\f9\1\5\4\2\0\r\13\0\b\1.\0\1\4\0\0\17\37\0\n\0\1\0\1\4\0\1:\17\2\0\310\0\0\0\0\3\0\f\b\0\n\2!\0\1\0\0\0\20\f\0\n\1;\0\2\7\2\0\16\f\0\n\5%\0\1\0\0\0\17\f\0\b\2\0\1\1\0\0\19\17\2\0\310\0\0\0\0\3\0\16\n\0\n\3/\1\2\4\0\0\13\t\0\b\t?\0\6\7\2\0\20\0\0\b\0#\0\1\4\0\0\20\f\0\t\1\0\0\2\4\0\0:\17\2\0\310\0\0\0\0\3\0\20\f\0\b\0\35\0\1\0\0\0\16\16\0\n\17)\0\2\0\2\0\24\16\0\n\7/\0\1\0\0\0\20\16\0\b\1\0\0\1\0\0\1:\17\2\0\310\b\0\6\0\3\0\r\0\0\b\0 \1\1\0\0\0\0\0\0\0\17\177\0\1\0\0\0\f\0\0\6\0004\0\1\0\0\0\20\b\0\b\1\0\0\1\0\0\1;\17\2\0\310\0\0\0\0\3\0\20\16\0\n\1\33\0\1\6\0\0\20\n\0\n\1+\1\1\4\0\0\24\0\0\6\0!\0\1\4\0\0\22\0\0\n\0\0\1\1\4\0\0006\17\2\0\313\2\2\1\1\3\0\16\n\1\5\5\30\1\0\1\0\0\22\2\1\b\3\0\0\1\3\0\1\37\2\22\n\5\0\2\1\7\0\1\17\2\1\n\5\0\2\0\3\0\1<\17\2\0\310\0\0\0\0\3\0\22\f\1\n\2 \1\1\0\0\0\22\n\1\n\3\0\0\1\1\0\1\17\n\1\n\5\27\1\1\2\0\0\24\2\1\n\3\7\0\1\6\0\1:\17\2\0\316\7\0\5\0\3\0\20\17\0\b\1\31\0\1\7\0\0\20\f\0\4\1?\0\b\0\2\0\22\0\0\4\0005\0\1\0\0\0\20\0\0\n\0\0\0\2\0\0\1<\17\2\0\310\b\0\5\0\3\0\16\f\0\4\1\31\1\1\3\0\0\22\0\0\b\0\0\1\1\3\0\1\17\0\0\6\0\17\1\0\7\0\0\20\0\0\t\0!\1\1\7\0\1;\17\2\0\311\6\16\7\1\2\0\37\37\0\6\0#\0\5\4\0\0\37\37\0\5\08\0\6\7\0\0\37\37\0\5\0\37\0\1\4\0\0\r\37\0\t\0\1\0\1\4\0\1\0\17\2\0\275\n\13\4\1\3\0\37\37\0\t\0G\0\t\4\2\1\n\37\0\t\0$\0\n\4\0\0\37\37\0\3\0000\0\1\4\0\0\r\37\0\b\0\0\0\2\4\0\1;\17\2\0\314\b\0\6\0\3\0\37\20\0\n\17%\0\4\0\0\0\30\0\0\n\0M\0\2\0\0\0\24\0\0\n\0M\0\3\7\1\0\20\5\0\n\7\0\0\4\0\0\1;\17\2\0\304\n\0\5\0\2\0\16\21\20\6\3;\0\4\4\1\0\20\37\0\0\0H\0\2\4\0\0\31\37\0\7\0\"\0\2\7\0\0\21\37\0\t\0\0\0\1\4\0\1\34\17\2\0\310\n\24\4\1\3\0\17\24\0\n\2\35\1\4\7\0\0\22\2\1\n\0\r\2\2\3\0\1\24\37\17\n\3\30\0\4\7\0\0\20\2\1\n\0\0\1\2\3\0\1;\17\2\0\310\n\0\5\0\3\0\24\0\0\n\0\0\0\4\0\0\0\16\20\0\n\5;\0\2\3\0\0\22\22\0\n\t\'\0\3\0\1\0\16\f\0\n\2\0\1\1\0\0\1\3\17\2\0\310\0\0\0\0\3\0\24\0\0\4\0\33\0\2\0\0\0\24\0\0\4\0\25\0\1\0\0\0\30\24\0\n\17\21\0\2\0\0\0\21\0\0\n\0\0\0\3\0\0\1\2\17\2\0\3542\0\7\0\3\0\37\0\0\n\0/\0\6\0\0\0\0\0\0\0\17\177\0\1\0\0\0\37\0\0\n\0+\0\n\0\0\0\24\b\0\n\1\0\0\2\0\0\1;\17\2\0\302\0(\0\1\3\0\37\0\0\n\0\0\0\n\0\0\0\30\16\0\n\3;\0\2\6\0\0\30\n\0\n\79\0\2\0\0\0\16\13\0\b\3\0\1\1\3\0\1\3\17\2\0\310\0\0\0\0\3\0\37\0\0\4\3\27\0\2\0\0\0\37\0\0\4\0\31\0\1\0\0\0\24\0\0\2\0\31\0\1\0\0\0\n\0\0\n\0\0\0\2\0\0\1<\17\2\0\310\0\0\0\0\3\0\37\0\0\1\0\0\0\f\0\3\0\34\21\0\b\17\7\1\1\0\0\1\36\21\0\t\r%\1\0\0\2\0\34\17\0\7\17\0\2\1\0\0\1<\17\2\0\310\0\0\0\0\3\0\36\0\0\1\0\0\0\2\0\2\0\34\20\0\b\17C\0\1\0\0\1\34\22\0\t\17!\0\0\0\3\0\36\20\0\b\17\0\0\0\0\2\1\2\17\2\0\310\0\0\0\0\3\0\36\20\1\n\17-\0\3\0\3\0\36\n\0\n\17)\0\0\7\1\0\36\24\0\n\17\21\0\0\3\3\0\36\24\0\n\17\0\0\1\0\0\1:\17\2\0\310\0\0\0\0\3\0\37\0\0\2\0\0\0\16\0\0\0\37\0\0\2\0\0\0\t\0\0\0\37\0\0\2\0\0\0\5\0\0\0\b\b\0\4\17\0\3\1\0\0\1\0\17\2\0\310\0\0\0\0\2\0\36\32\0\r\17\25\0\1\0\1\0\36\34\0\16\17/\0\16\0\3\0\36\20\0\b\17\7\0\0\0\1\0\35\20\0\b\17\0\0\0\0\0\1+\17\2\0\310\0\0\0\0\3\0\37\24\0\n\17\13\0\1\0\0\0\37\f\0\6\17\37\0\0\0\3\0\37\26\0\n\3\21\0\0\0\0\0\37\22\0\t\17\0\0\1\0\0\0012\17\2\1\214\177\0\5\0\3\0\30\24\0\n\17\25\1\2\0\0\0\32\f\0\6\17\27\1\1\0\2\0\37\n\0\4\17%\1\1\3\1\0\32\13\0\5\17\0\2\1\0\0\1!\17\2\0\310\0\0\0\0\3\0\32\26\0\n\n\7\0\3\0\0\0\34\24\0\n\17\17\0\2\0\1\0\34\26\0\n\0175\0\1\0\3\0\32\f\0\6\17\0\2\1\0\0\1\2\17\2\0\310\0\0\0\0\3\0\34\f\0\4\17%\1\0\0\1\0\24\b\0\4\17\'\1\0\0\2\0\34\n\0\5\17%\0\0\0\0\0\20\5\0\2\17\0\3\0\0\0\0012\17\2\0\310\16\0\7\0\3\0\36\n\0\2\17!\1\0\0\0\0\36\n\0\4\17\37\0\0\5\3\0\36\n\0\4\5!\1\0\3\1\0\32\b\0\4\17\0\2\0\0\0\1;\17\2\0\310\0\0\0\0\3\0\30\27\0\13\17\3\0\3\0\3\0\32\16\0\7\17+\0\2\0\2\0\32\n\0\5\17;\0\2\0\3\0\26\20\0\b\17\0\2\6\0\0\0013\17\2\0\310\0\0\0\0\3\0\31\27\0\f\17\7\0\3\0\3\0\32\16\0\7\0173\0\3\0\0\0\32\b\0\5\59\0\4\0\0\0\30\20\0\b\17\0\2\6\0\0\0012\17\2\0\310\0\0\0\0\3\0\34\17\0\6\17\25\1\2\3\3\0\30\20\0\7\17!\0\b\7\2\0\32\17\0\7\17\37\1\5\3\0\0\30\13\0\5\17\0\2\2\7\3\1\4\17\2\0\310\0\0\0\0\3\0\0\0\0\0\0\177\0\1\0\0\0\0\0\0\0\0\177\0\1\0\0\0\20\32\0\n\17/\0\1\0\0\0\16\30\0\n\17\0\0\1\0\0\1\3\17\2\0\310\0\0\0\0\3\0\37\6\0\4\0173\0\1\0\3\0\37\0\0\2\0\25\0\b\7\2\0\37\b\0\6\5C\0\t\3\1\0\37\n\0\5\17\0\0\n\3\2\1:\17\2\0\343\35\0\7\0\3\0\37\25\0\4\5\13\0\17\0\3\0\37\0\0\3\0003\0\0\0\3\0\37\0\0\4\0\23\0\0\0\0\0\24\20\0\b\17\0\0\r\0\0\1:\17\2\0\343\35\0\7\0\3\0\30\22\0\4\1\35\0\17\0\3\0\37\0\0\3\0I\0\2\0\1\0\37\0\0\3\0\25\0\0\0\0\0\22\16\0\7\17\0\0\r\0\0\1\"\17\2\0\310\0\0\0\0\3\0\37\30\0\n\17\21\0\b\3\3\0\37\n\0\2\7\27\0\13\3\3\0\37\20\0\b\7!\0\5\3\0\0\37\16\0\6\17\0\0\3\0\3\1;\17\2\0\310\0\0\0\0\3\0\36\24\0\n\17\33\0\17\2\0\0\36\21\0\b\17\33\1\4\0\1\0\34\f\0\6\17+\1\2\3\2\0\32\20\0\b\17\0\1\2\0\3\1*\17\2\0\310\0\0\0\0\3\0\37\26\0\f\17\23\0\17\0\3\0\37\20\0\n\17)\0\16\7\0\0\37\b\0\4\17\31\0\7\0\0\0\37\n\0\4\17\21\1\2\3\0\1.\17\2\0\310\0\0\0\0\3\0\37\6\0\3\17\35\0\r\0\0\0\36\b\0\4\17\21\1\5\0\0\1\36\b\0\4\17\21\1\f\0\0\1\37\25\0\n\17\21\0\16\0\3\1\2\17\2\0\320\5\0\5\0\3\0\37\24\0\n\0175\0\2\0\2\0\37\20\0\t\179\0\5\0\3\0\37\b\0\4\17\31\0\t\7\0\0\37\b\0\4\17\13\1\2\3\0\1\4\17\2\0\320\6\0\6\0\3\0\r\b\0\4\17\31\1\3\0\0\0\22\b\0\4\17\3\2\1\7\0\1\16\b\0\4\17\37\1\2\7\0\0\20\b\0\4\17\21\2\2\3\0\1\23\17\2\0\310\0\0\0\0\3\0\37\30\0\f\17C\0\n\2\1\0\37\20\0\b\17\33\0\6\0\3\0\37\f\0\4\0173\0\4\0\0\0\37\n\0\5\17\0\0\2\0\0\1,\17\2\0\304\6\20\5\3\3\0\30\16\0\7\179\1\f\3\0\0\30\n\0\7\17\0\1\4\0\0\1\32\16\0\6\179\1\4\0\0\0\32\b\0\6\17\5\2\1\0\0\1\3\17\2\0\310\0\30\0\1\3\0\32\16\0\7\17/\0\n\0\0\0\30\16\0\4\179\2\t\0\0\0\36\n\0\0\0G\0\3\3\0\0\30\b\0\5\17\0\2\1\0\0\1\2\17\2\0\310\0\0\0\0\3\0\37\24\0\n\0173\0\2\0\3\0\37\30\0\n\17\21\0\4\0\1\0\37\20\0\n\17C\0\4\0\0\0\36\22\0\n\17\0\0\2\0\0\1\33\17\2\0\310\0\0\0\0\3\0\37\22\0\n\17\23\0\2\0\2\0\37\32\0\n\17\37\0\2\3\0\0\37\26\0\n\17/\0\2\0\3\0\36\24\0\n\17\0\0\2\7\0\1<\17\2\0\310\0\0\0\0\3\0\36\32\0\n\17)\0\6\0\2\0\34\24\0\n\17\0\0\b\0\0\1\34\24\0\n\179\0\17\0\0\0\30\23\0\t\17/\1\t\7\0\1\4\17\2\0\310\0\0\0\0\3\0\36\30\0\n\17\t\0\5\0\3\0\34\24\0\n\17\0\0\3\0\0\1\25\32\0\n\17\3\0\t\0\3\0\33\24\0\n\17\0\0\2\0\2\1\2\17\1\0\370\0P\0\3\3\0\24\0\0\n\0\33\0\2\0\2\0\37\0\0\n\0\21\0\1\0\0\0\37\0\0\n\0!\0\3\0\3\0\20\b\24\f\1\0\0\17\0\1\1\4\17\2\0\310\0\0\0\0\3\0\37\22\0\n\17!\0\6\0\0\0\37\26\0\n\17\0\0\3\0\0\1\37\30\0\b\17/\0\4\0\0\0\37\20\0\b\17\7\0\1\0\0\1,\17\2\0\310\0\0\0\0\3\0\30\16\0\7\17!\1\4\3\0\0\30\4\0\2\17\0\3\0\3\0\1\30\24\0\n\17/\1\6\7\0\0\30\f\0\6\17\13\2\2\7\0\1:\17\2\0\310\0\0\0\0\3\0\37\0\0\2\0\0\0\16\0\0\0\37\0\0\2\0\0\0\f\0\0\0\37\0\0\2\0\0\0\n\0\0\0\20\24\0\n\17\3\2\1\0\0\1:\17\2\0\310\0\0\0\0\3\0\37\0\0\2\0\0\0\16\0\0\0\37\0\0\2\0\0\0\t\0\0\0\37\0\0\2\0\0\0\5\0\0\0\20\22\0\t\17\t\0\1\0\0\18\17\2\1\364\3\0\7\0\3\0\32\n\0\5\0)\0\0\0\0\0\34\22\0\n\17\21\0\0\0\0\0\26\n\0\6\17\t\0\1\0\0\0\32\24\0\n\17\0\0\b\0\0\1;\17\2\0\310\0\0\0\0\3\0\32\4\0\2\3\17\0\16\0\1\0\32\b\0\2\7\33\0\6\0\1\0\32\26\0\n\13\21\0\7\0\2\0\26\22\0\b\17\5\0\0\0\0\0013\17\2\0\310P\0\3\0\3\0\32\0\0\n\0\21\0\1\7\1\0\32\4\0\f\2\b\0\4\0\3\0\24\22\1\f\3\25\0\1\0\2\0\27\13\f\16\4\21\0\1\3\0\1;\17\2\0\310<\0\3\0\3\0\36\4\0\2\17\26\0\1\7\1\0\36\2\0\1\17\31\0\4\0\2\0\37\b\0\4\17#\0\t\0\2\0\34\f\0\6\17\21\0\1\3\0\1\2\17\2\0\310\7\0\7\0\3\0\t\6\0\1\17%\2\1\7\1\0\n\0\0\1\0#\1\3\3\1\0\2\0\0\1\0\21\2\1\0\2\0\f\2\4\2\0\0\2\0\0\0\1\32\17\2\0\310\t\3\5\2\3\0\24\1\1\n\3\7\1\2\1\0\0\24\2\1\n\0\0\0\3\0\1\0\37\24\1\0\17\33\0\0\0\0\0\24\2\1\n\3\20\2\1\3\0\1\34\17\2\1\322\5\0\6\0\3\0\37\20\0\0\17\0\1\2\3\0\0\37\0\0\b\0\20\1\1\6\0\1\37\0\0\b\0\21\1\2\7\0\0\37\0\0\b\0\20\1\1\3\0\1<\17\2\0\313\b\0\6\0\3\0\37\b\0\n\2\26\0\1\0\0\0\24\0\0\n\0\t\0\0\0\0\1\21\0\0\n\0\33\0\1\0\0\0\24\2\1\n\3\0\0\0\0\0\1=\17\2\0\303\4\0\6\0\3\0\31\r\0\n\5\30\0\f\0\0\0\34\0\0\n\0\0\0\6\0\0\1\34\2\1\n\3\7\0\b\0\0\1\34\0\0\n\0\7\0\t\0\0\18\17\2\0\314\5\0\6\0\3\0\34\0\0\4\0\35\0\7\6\0\0\34\0\4\4\0\37\0\3\4\0\0\34\0\6\4\0\33\0\1\4\0\0\30\16\4\b\1\0\0\2\4\0\0>\17\2\0\311\6\0\6\0\3\0\r\17\0\n\2\23\0\2\3\0\0\37\n\0\b\5\33\0\2\3\0\0\24\0\0\b\0/\0\1\6\0\0\24\0\4\b\0\0\0\2\4\0\1<\17\2\0\314\6\0\5\0\3\0\37\n\6\3\5\35\0\b\3\0\0\32\f\6\6\3\0\0\2\7\0\0\37\0\0\3\3\37\0\2\3\3\0\24\f\6\b\2\0\0\0\3\0\1;\17\2\0\310\0\0\3\0\3\0\24\5\1\f\3\35\1\1\1\0\0\24\30\0\b\2\21\1\1\2\0\0\37\30\0\0\0 \0\0\0\0\0\24\0\0\t\0\0\0\1\7\0\0=\17\2\0\310\0\0\0\0\3\0\22\6\6\17\6\26\0\1\0\0\0\24\f\f\17\6\0\0\1\2\0\0\24\f\f\17\6\0\0\3\0\0\0\24\f\f\17\5\0\0\2\6\0\0\34\17\2\0\310\0\0\0\0\3\0\13\b\1\n\5\22\1\0\3\0\0\17\n\n\n\5\0\0\1\3\0\1\17\n\1\n\5\33\1\0\7\0\0\17\2\n\n\5\7\0\0\3\0\1=\17\2\0\310\3\2\2\1\3\0\37\n\0\17\r\26\0\6\0\0\0\37\f\5\17\r\0\0\1\2\0\1\37\f\5\17\r\0\0\3\0\0\1\37\f\5\17\r\0\0\2\6\0\18\17\2\1\310\4\0\6\0\3\0\37\37\n\5\17\0\0\1\7\0\0\37\5\n\5\5\26\2\1\1\0\0\35\4\0\5\5\22\1\1\7\0\0\31\n\5\b\5\0\0\1\3\0\1,\17\2\0\310\0\0\0\0\3\0\25\n\0\4\17\26\1\0\7\0\0\37\n\0\b\3\0\1\0\1\0\1\25\16\0\4\17\7\2\0\7\0\0\37\n\0\b\3\0\0\0\0\0\1=\17\2\0\310\0\0\0\0\3\0\24\n\0\n\2#\0\3\4\0\0\32\0\0\n\0\0\0\1\7\0\0\32\0\0\n\0\0\0\1\4\0\0\32\0\0\n\0\0\0\2\4\0\0\3\17\2\0\310\0\0\0\0\3\0\30\b\0\n\3\33\0\0\3\0\0\32\b\0\n\17\7\0\0\4\0\0\32\b\0\n\17%\0\b\7\0\0\32\0\4\n\0\0\0\1\4\0\0=\17\2\0\310\0\0\0\0\3\0\33\21\0\n\17#\0\n\4\0\0\24\0\0\n\0\0\0\2\4\0\0\24\0\0\n\0\0\0\1\4\0\0\24\0\0\n\0\0\0\0\4\0\0;\17\2\0\310\0\0\0\0\3\0\26\0\0\n\0\r\0\n\0\0\0\32\32\0\n\17\23\0\r\0\3\0\32\26\0\13\17\13\0\0\0\1\0\36\16\0\7\17\0\1\1\0\0\1;\17\0\1\260P\0\7\0\3\0\37\0\0\5\0\f\0\16\0\0\0\37\n\0\5\0171\0\0\0\3\0\33\33\0\n\17%\0\n\0\2\0\34\16\0\7\17\0\1\0\0\1\1$\17\2\0\306\6\24\7\1\3\0\37\20\0\b\17\33\1\f\0\3\0\37\n\0\5\17\21\1\4\0\0\1\24\0\0\n\0%\0\1\0\0\0\16\b\0\b\1\0\1\1\0\0\1<\17\2\0\302\4\0\6\0\3\0\20\f\0\n\1\35\1\1\0\0\0\22\n\0\b\2\0\1\1\0\0\1\37\20\0\b\17\21\1\f\0\3\0\37\n\0\5\17\t\1\4\0\0\1<\17\2\0\306\7\0\6\0\3\0\24\0\0\2\0 \0\1\6\0\0\b\0\0\6\0\21\1\1\3\0\1\34\f\1\4\f3\1\f\0\0\0\32\1\1\7\2\0\2\1\3\0\1\4\17\2\1\322x\0\6\0\3\0\21\0\0\n\09\0\2\0\0\0\20\22\0\n\17\n\0\2\0\0\1\17\0\0\n\09\0\2\2\0\0\21\21\0\n\17\n\0\2\7\0\1\3\17\2\1\326\177\0\7\0\3\0\24\30\0\n\17%\0\b\0\0\0\22\30\0\n\17+\0\3\0\0\0\24\n\0\5\17\23\0\0\0\0\0\22\23\0\t\17\0\0\f\0\0\1\7\17\2\0\374}<\5\2\3\0\0\0\0\0\17\177\0\1\0\0\1\0\0\0\0\17\177\0\5\0\0\1\20\0\0\n\0\0\0\1\3\0\1\20\0\0\n\0\0\0\1\5\0\1\4\17\2\0\340d\0\5\0\3\0\24\0\0\n\0\21\0\0\0\3\0\24\0\0\n\0\0\0\f\0\2\1\24\0\0\n\0\n\0\0\0\1\0\24\0\0\n\0\0\0\16\0\3\1\4\17\1\0\206\0d\0\3\3\0\0\0\0\0\17\177\0\1\0\0\0\0\0\0\0\17\177\0\1\0\0\1\37\0\0\n\0#\0\16\7\0\0\24\0\0\n\0\0\0\5\3\1\1<\17\1\0\346\0\22\0\3\3\0\37\0\0\17\0%\0\5\0\0\0\37\0\0\17\0\21\0\1\0\0\1\37\0\0\17\2\30\0\5\0\0\0\37\0\0\17\2\n\0\1\0\0\1\4\17\1\0\231D\0\6\0\3\0\0\0\0\0\17\177\0\1\0\0\0\0\0\0\0\17\177\0\1\0\0\1\37\0\0\1\0%\0\16\0\0\0\20\0\0\4\0\0\0\5\7\1\1\0\17\2\0\202x\0\7\0\3\0\0\0\0\0\17\177\0\1\0\0\0\37\0\0\1\0\37\0\1\0\0\0\37\0\0\1\0\27\0\1\0\0\0\20\0\0\b\0\0\0\1\0\0\1:\17\2\0xx\36\7\2\3\0\37\0\0\0\0\21\0\2\0\2\0\37\0\0\0\0\n\0\1\0\1\0\37\0\0\0\0\35\0\1\0\2\0\f\0\0\4\0\0\0\0\0\0\1:\17\2\0\310\0\0\0\0\3\0\37\0\0\0\0\f\0\0\0\1\0\37\0\0\0\0\21\0\f\0\2\0\37\0\0\0\0\f\0\5\0\3\0\1\0\4\2\0\0\3\4\0\0\1\4\17\0\1\324x\0\7\0\3\0\37\0\0\5\0\17\0\0\0\3\0\24\0\0\n\0\7\0\7\0\1\1\f\0\0\5\0/\0\3\0\3\0\20\0\0\b\0\0\0\1\0\0\1\3\17\3\0\322P\0\7\0\3\0\30\26\0\13\17\n\0\1\0\1\0\37\n\0\5\17%\0\6\0\3\0\37\0\0\0\0003\0\r\0\3\0\34\r\0\6\17\0\0\3\0\2\1\6\17\3\0\310P\0\6\0\3\0\37\0\0\0\0C\0\f\0\0\0\24\16\0\7\17\7\0\4\0\0\0\24\16\0\7\17\21\0\2\4\3\0\24\16\0\7\17\0\0\2\4\0\0 \17\2\0\310\0\0\0\0\3\0\37\b\0\4\17\r\0\3\0\2\0\n\n\0\4\17\21\3\1\0\1\0\37\0\0\0\0\3\0\1\0\2\0\20\n\0\5\17\0\3\0\0\0\1\4\17\3\0\202x\0\7\0\3\0\37\0\0\4\0\21\0\16\0\2\0\20\0\0\b\0\0\0\b\0\0\1\37\0\0\4\0%\0\3\0\2\0\20\0\0\7\0\0\0\1\0\0\1\4\17\2\1\276\24\0\2\0\3\0\22\16\0\7\17\23\0\f\2\0\0\25\f\n\6\7\21\0\1\4\3\0\f\16\0\7\17\21\0\n\7\0\0\32\f\n\6\7\0\0\2\4\0\1\4\17\3\0\330\0\0\1\0\3\0\37\13\6\0\1\16\0\4\4\3\0\37\r\t\3\5$\1\1\5\0\1\13\5\3\0\1\r\1\17\4\2\0\b\7\6\3\3\35\1\5\4\0\1\4\17\2\0\372dd\1\2\3\0\n\n\0\17\0%\0\1\4\0\0\b\5\n\n\1\0\0\17\4\3\0\37\37\0\1\0\f\3\0\7\0\1\37\37\0\2\0\0\3\0\3\0\1\7\17\2\0\310\0\0\0\0\3\0\0\0\0\0\0\177\0\1\4\0\0\0\0\0\0\0\177\0\1\4\0\0\0\0\0\0\0\177\0\1\4\0\0\37\37\0\17\0\0\0\1\4\0\0".getBytes (XEiJ.ISO_8859_1);

  //TONE_MASK
  //  音色データのマスク兼最大値
/*
  public static final byte[] TONE_MASK = {
    //FC SL WA SY   SP   PD   AD PS AS PN
    63, 15, 3,  1, 255, 127, 127, 7, 3, 3, 0,
    //AR 1R 2R  RR  1L   TL KS  ML T1 T2 AE
    31, 31, 31, 15, 15, 127, 3, 15, 7, 3, 1,  //M1
    31, 31, 31, 15, 15, 127, 3, 15, 7, 3, 1,  //C1
    31, 31, 31, 15, 15, 127, 3, 15, 7, 3, 1,  //M2
    31, 31, 31, 15, 15, 127, 3, 15, 7, 3, 1,  //C2
  };
*/
  //perl ../misc/itob.pl MMLCompiler.java TONE_MASK
  public static final byte[] TONE_MASK = "?\17\3\1\377\177\177\7\3\3\0\37\37\37\17\17\177\3\17\7\3\1\37\37\37\17\17\177\3\17\7\3\1\37\37\37\17\17\177\3\17\7\3\1\37\37\37\17\17\177\3\17\7\3\1".getBytes (XEiJ.ISO_8859_1);

  //  一般的なピアノは88鍵
  //      A#  C#D#  F#G#A#  C#D#  F#G#A#  C#D#  F#G#A#  C#D#  F#G#A#  C#D#  F#G#A#  C#D#  F#G#A#  C#D#  F#G#A#
  //      B   B B   B B B   B B   B B B   B B   B B B   B B   B B B   B B   B B B   B B   B B B   B B   B B B
  //     W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W
  //     A B C D E F G A B C D E F G A B C D E F G A B C D E F G A B C D E F G A B C D E F G A B C D E F G A B C
  //     000 00000 0011111 11111 2222222 22233 3333333 34444 4444445 55555 5555666 66666 6677777 77777 8888888 8
  //     012 34567 8901234 56789 0123456 78901 2345678 90123 4567890 12345 6789012 34567 8901234 56789 0123456 7  kn
  //     000 11111 1111111 22222 2222222 33333 3333333 44444 4444444 55555 5555555 66666 6666666 77777 7777777 8  oct
  //     001 -0000 0000001 -0000 0000001 -0000 0000001 -0000 0000001 -0000 0000001 -0000 0000001 -0000 0000001 -
  //     890 10123 4567890 10123 4567890 10123 4567890 10123 4567890 10123 4567890 10123 4567890 10123 4567890 1  note12
  //                                                             |
  //                                                            440
  //
  //  ピアノの右端の白鍵は、3.58MHzのときはKC=128になるので出せないが、4MHzのときはKC=125になるので出せる

  //    0   1   2   3   4   5   6   7   8   9  10  11  12  13  -2  -1    note16
  //    0   1   2       3   4   5       6   7   8       9  10  -1        note12
  //    C#  D   D# (E)  E   F   F# (G)  G   G#  A  (A#) A#  B   C  (C#)  3.58MHz
  //    D#  E   F  (F#) F#  G   G# (A)  A   A#  B  (C)  C   C#  D  (D#)  4.00MHz

}
