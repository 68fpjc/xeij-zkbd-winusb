//========================================================================================
//  DnT.java
//    en:Date and time
//    ja:日時
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  データの種類
//    cday     int                  暦通日      1970年1月1日0時0分0.000秒から数えた日数
//    cmil     long                 暦通ミリ秒  1970年1月1日0時0分0.000秒から数えたミリ秒数
//    csec     long                 暦通秒      1970年1月1日0時0分0.000秒から数えた秒数
//    date     int                  日付        西暦年<<10|月<<6|月通日
//    dmil     int     0..86400000  日通ミリ秒  0時0分0.000秒から数えたミリ秒数
//    dsec     int     0..86400     日通秒      0時0分0.000秒から数えた秒数
//    dttm     long                 日時        日付<<32|時刻=西暦年<<42|月<<38|月通日<<32|時<<22|分<<16|秒<<10|ミリ秒
//    hour     int     0..23        時          0=0時,…,23=23時
//    mday     int     0..32        月通日      1=1日,…,31=31日。1月0日と12月32日が必要になることがある
//    mill     int     0..999       ミリ秒      0=0ミリ秒,…,999=999ミリ秒
//    minu     int     0..59        分          0=0分,…,59=59分
//    mont     int     1..12        月          1=1月,…,12=12月
//    now                           現在
//    sdt      String               日付文字列(year-mo-md)
//    sdw      String               日付文字列(year-mo-md (Www))
//    sdttl    String               日時文字列(year-mo-md ho:mi:se)
//    sdttm    String               日時文字列(year-mo-md ho:mi:se.mil)
//    sdwtl    String               日時文字列(year-mo-md (Www) ho:mi:se)
//    sdwtm    String               日時文字列(year-mo-md (Www) ho:mi:se.mil)
//    seco     int     0..61        秒          0=0秒,…,59=59秒。閏秒として60秒と61秒が必要になることがある
//    stl      String               時刻文字列(ho:mi:se)
//    stm      String               時刻文字列(ho:mi:se.mil)
//    time     int                  時刻        時<<22|分<<16|秒<<10|ミリ秒
//    wday     int     0..6         曜日        0=日曜日,1=月曜日,2=火曜日,3=水曜日,4=木曜日,5=金曜日,6=土曜日
//    year     int                  西暦年      -1=BC(紀元前)2年,0=BC(紀元前)1年,1=AD(紀元後)1年
//
//  組み合わせ
//    X  あり
//    -  なし
//                                   デスティネーション                               ソース
//         Cmil  Date  Dsec  Hour  Mill  Mont  Sdt  Sdttl Sdwtl  Seco  Stm  Wday
//      Cday| Csec| Dmil| Dttm| Mday| Minu| Now | Sdw |Sdttm|Sdwtm| Stl | Time| Year
//       |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
//       -  X  X  X  -  -  X  -  X  -  -  X  -  X  X  X  X  X  X  -  -  -  -  X  X    Cday
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    CdayDmil
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    CdayDsec
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    CdayHourMinuSeco
//       -  X  -  -  -  -  X  -  -  -  -  -  -  -  -  -  X  -  X  -  -  -  -  -  -    CdayHourMinuSecoMill
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    CdayTime
//       X  -  X  X  X  X  X  X  X  X  X  X  -  X  X  X  X  X  X  X  X  X  X  X  X    Cmil
//       X  X  -  X  X  X  X  X  X  -  X  X  -  X  X  X  X  X  X  X  X  X  X  X  X    Csec
//       X  X  X  -  -  -  X  -  X  -  -  X  -  X  X  X  X  X  X  -  -  -  -  X  X    Date
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    DateDmil
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    DateDsec
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    DateHourMinuSeco
//       -  X  -  -  -  -  X  -  -  -  -  -  -  -  -  -  X  -  X  -  -  -  -  -  -    DateHourMinuSecoMill
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    DateTime
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  -  -  X  X  -  -  -  -  -  -    DateWday
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    DateWdayDmil
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    DateWdayDsec
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    DateWdayHourMinuSeco
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  -  -  -  -  -  -    DateWdayHourMinuSecoMill
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    DateWdayTime
//       -  -  -  -  -  X  -  X  -  X  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -    Dmil
//       -  -  -  -  X  -  -  X  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -    Dsec
//       X  X  X  X  X  X  -  X  X  X  X  X  -  X  X  X  X  X  X  X  X  X  X  X  X    Dttm
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  -  -  X  X  -  -  -  -  -  -    DttmWday
//       -  -  -  -  X  X  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  X  -  -    HourMinuSeco
//       -  -  -  -  X  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -    HourMinuSecoMill
//       X  X  X  X  X  X  X  X  X  X  X  X  -  X  X  X  X  X  X  X  X  X  X  X  X    Now
//       -  -  -  -  X  X  -  X  -  X  X  -  -  -  -  -  -  -  -  X  X  X  -  -  -    Time
//       X  X  X  X  -  -  X  -  -  -  -  -  -  X  X  X  X  X  X  -  -  -  -  X  -    YearMontMday
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    YearMontMdayDmil
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    YearMontMdayDsec
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    YearMontMdayHourMinuSeco
//       -  X  -  -  -  -  X  -  -  -  -  -  -  -  -  -  X  -  X  -  -  -  -  -  -    YearMontMdayHourMinuSecoMill
//       -  X  X  -  -  -  X  -  -  -  -  -  -  -  -  X  X  X  X  -  -  -  -  -  -    YearMontMdayTime
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  -  -  X  X  -  -  -  -  -  -    YearMontMdayWday
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    YearMontMdayWdayDmil
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    YearMontMdayWdayDsec
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    YearMontMdayWdayHourMinuSeco
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  -  -  -  -  -  -    YearMontMdayWdayHourMinuSecoMill
//       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  X  X  -  -  -  -  -  -    YearMontMdayWdayTime
//
//    enmont3  String               月の名前(英語3文字)
//    enmont4  String               月の名前(英語)
//    enwday3  String               曜日の名前(英語3文字)
//    enwday6  String               曜日の名前(英語)
//    jaholi   String               日本の祝日の名前(日本語)
//    jalcmont String               旧暦の月の名前(日本語)
//    jawday1  String               曜日の名前(日本語1文字)
//    jawday3  String               曜日の名前(日本語)
//
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class DnT {

  protected static final String[] DNT_ENMON3_MONT = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
  protected static final String[] DNT_ENMON4_MONT = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
  protected static final String[] DNT_ENWDAY3_WDAY = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
  protected static final String[] DNT_ENWDAY6_WDAY = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
  protected static final String[] DNT_JAWDAY1_WDAY = { "日", "月", "火", "水", "木", "金", "土" };
  protected static final String[] DNT_JAWDAY3_WDAY = { "日曜日", "月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日" };
  protected static final String[] DNT_JALCMONT_MONT = { "睦月", "如月", "弥生", "卯月", "皐月", "水無月", "文月", "葉月", "長月", "神無月", "霜月", "師走" };

  //q = dntFdiv (x, y)
  //  床除算
  //  q=floor(x/y)
  //  xとyの符号が同じまたはx/yが割り切れるとき  q=x/y
  //  xとyの符号が違いかつx/yが割り切れないとき  q=x/y-1
  public static int dntFdiv (int x, int y) {
    if (true) {
      return (int) Math.floor ((double) x / (double) y);  //0.026ns。20倍くらい速い
    } else {
      int q = x / y;
      return 0 <= (x ^ y) || x - q * y == 0 ? q : q - 1;  //0.540ns。0<=x*yだと溢れる場合がある。x==0&&y<0のとき(x^y)<0だがx-q*y==0なので問題ない
    }
  }  //dntFdiv(int,int)
  public static long dntFdiv (long x, long y) {
    //return (long) Math.floor ((double) x / (double) y);  //bit数が足りない
    long q = x / y;
    return 0L <= (x ^ y) || x - q * y == 0L ? q : q - 1L;  //0L<=x*yだと溢れる場合がある。x==0L&&y<0Lのとき(x^y)<0Lだがx-q*y==0Lなので問題ない
  }  //dntFdiv(long,long)
  public static double dntFdiv (double x, double y) {
    return Math.floor (x / y);
  }  //dntFdiv(double,double)

  //r = dntFrem (x, y)
  //  床剰余
  //  r=x-floor(x/y)*y=mod(x,y)
  //  xとyの符号が同じまたはx/yが割り切れるとき  r=x%y
  //  xとyの符号が違いかつx/yが割り切れないとき  r=x%y+y
  public static int dntFrem (int x, int y) {
    if (false) {
      return (int) ((double) x - Math.floor ((double) x / (double) y) * (double) y);  //0.026ns。20倍くらい速い
    } else if (false) {
      double u = (double) x;
      double v = (double) y;
      return (int) (u - Math.floor (u / v) * v);  //0.026ns
    } else {
      int r = x % y;
      return 0 <= (x ^ y) || r == 0 ? r : r + y;  //0.540ns。0<=x*yだと溢れる場合がある。x==0&&y<0のとき(x^y)<0だがr==0なので問題ない
    }
  }  //dntFrem(int,int)
  public static long dntFrem (long x, long y) {
    //return (long) ((double) x - Math.floor ((double) x / (double) y) * (double) y);  //bit数が足りない
    long r = x % y;
    return 0L <= (x ^ y) || r == 0L ? r : r + y;  //0L<=x*yだと溢れる場合がある。x==0L&&y<0Lのとき(x^y)<0Lだがr==0Lなので問題ない
  }  //dntFrem(long,long)
  public static double dntFrem (double x, double y) {
    return x - Math.floor (x / y) * y;
  }  //dntFrem(double,double)


  //--------------------------------------------------------------------------------
  //cday 暦通日

  //cday = dntCdayCmil (cmil)
  //  暦通ミリ秒を暦通日に変換する
  public static int dntCdayCmil (long cmil) {
    return (int) dntFdiv (cmil, 86400000L);
  }  //dntCdayCmil(long)

  //cday = dntCdayCsec (csec)
  //  暦通秒を暦通日に変換する
  public static int dntCdayCsec (long csec) {
    return (int) dntFdiv (csec, 86400L);
  }  //dntCdayCsec(long)

  //cday = dntCdayDate (date)
  //  日付から暦通日を求める
  public static int dntCdayDate (int date) {
    //return dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntCdayDate(int)

  //cday = dntCdayDttm (dttm)
  //  日時から暦通日を求める
  public static int dntCdayDttm (long dttm) {
    //return dntCdayDate (dntDateDttm (dttm));
    int date = (int) (dttm >> 32);
    //return dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntCdayDttm(long)

  //cday = dntCdayNow ()
  //  現在の暦通日を返す
  public static int dntCdayNow () {
    //return dntCdayCmil (dntCmilNow ());
    return (int) dntFdiv (dntCmilNow (), 86400000L);
  }  //dntCdayNow()

  //cday = dntCdayYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から暦通日を求める
  //  日付から年通日を求めるときに1月0日の暦通日を使うので0日の入力をエラーにしないこと
  public static int dntCdayYearMontMday (int year, int mont, int mday) {
    if (mont < 3) {  //1月と2月を前年の13月と14月として処理する
      year--;
      mont += 12;
    }
    int cday = ((int) Math.floor ((double) year * 365.25) +
                (int) Math.floor ((double) (mont - 2) * 30.59) +
                mday - 719501);  //ユリウス暦と見なして暦通日に変換する
    if (-141417 <= cday) {  //グレゴリオ暦のとき
      cday += dntFdiv (year, 400) - dntFdiv (year, 100) + 2;  //補正を加える
    }
    return cday;
  }  //dntCdayYearMontMday(int,int,int)


  //--------------------------------------------------------------------------------
  //cmil 暦通ミリ秒

  //cmil = dntCmilCday (cday)
  //  暦通日を暦通ミリ秒に変換する
  public static long dntCmilCday (int cday) {
    return (long) cday * 86400000L;
  }  //dntCmilCday(int)

  //cmil = dntCmilCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を暦通ミリ秒に変換する
  public static long dntCmilCdayDmil (int cday,
                                      int dmil) {
    //return dntCmilCday (cday) + (long) dmil;
    return (long) cday * 86400000L + (long) dmil;
  }  //dntCmilCdayDmil(int,int)

  //cmil = dntCmilCdayDsec (cday, dsec)
  //  暦通日と日通秒を暦通ミリ秒に変換する
  public static long dntCmilCdayDsec (int cday,
                                      int dsec) {
    //return dntCmilCday (cday) + (long) dntDmilDsec (dsec);
    return (long) cday * 86400000L + (long) (dsec * 1000);
  }  //dntCmilCdayDsec(int,int)

  //cmil = dntCmilCdayHourMinuSeco (cday, hour, minu, seco)
  //  歴通日と時と分と秒から暦通ミリ秒を求める
  public static long dntCmilCdayHourMinuSeco (int cday,
                                              int hour, int minu, int seco) {
    //return (dntCmilCday (cday) +
    //        (long) dntDmilHourMinuSecoMill (hour, minu, seco));
    return ((long) cday * 86400000L +
            (long) (hour * 3600000 + minu * 60000 + seco * 1000));
  }  //dntCmilCdayHourMinuSeco(int,int,int,int)

  //cmil = dntCmilCdayHourMinuSecoMill (cday, hour, minu, seco, mill)
  //  歴通日と時と分と秒とミリ秒から暦通ミリ秒を求める
  public static long dntCmilCdayHourMinuSecoMill (int cday,
                                                  int hour, int minu, int seco, int mill) {
    //return (dntCmilCday (cday) +
    //        (long) dntDmilHourMinuSecoMill (hour, minu, seco, mill));
    return ((long) cday * 86400000L +
            (long) (hour * 3600000 + minu * 60000 + seco * 1000 + mill));
  }  //dntCmilCdayHourMinuSecoMill(int,int,int,int,int)

  //cmil = dntCmilCdayTime (cday, time)
  //  歴通日と時刻から暦通ミリ秒を求める
  public static long dntCmilCdayTime (int cday,
                                      int time) {
    //return (dntCmilCday (cday) +
    //        (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time)));
    return ((long) cday * 86400000L +
            (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023)));
  }  //dntCmilCdayTime(int,int)

  //cmil = dntCmilCsec (csec)
  //  暦通秒を暦通ミリ秒に変換する
  public static long dntCmilCsec (long csec) {
    return csec * 1000L;
  }  //dntCmilCsec(long)

  //cmil = dntCmilDate (date)
  //  日付から暦通ミリ秒を求める
  public static long dntCmilDate (int date) {
    //return dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L;
  }  //dntCmilDate(int)

  //cmil = dntCmilDateDmil (date, dmil)
  //  日付と日通ミリ秒から暦通ミリ秒を求める
  public static long dntCmilDateDmil (int date,
                                      int dmil) {
    //return (dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dmil);
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L +
            (long) dmil);
  }  //dntCmilDateDmil(int,int)

  //cmil = dntCmilDateDsec (date, dsec)
  //  日付と日通秒から暦通ミリ秒を求める
  public static long dntCmilDateDsec (int date,
                                      int dsec) {
    //return (dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dntDmilDsec (dsec));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L +
            (long) (dsec * 1000));
  }  //dntCmilDateDsec(int,int)

  //cmil = dntCmilDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒から暦通ミリ秒を求める
  public static long dntCmilDateHourMinuSeco (int date,
                                              int hour, int minu, int seco) {
    //return (dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dmilHourMinuSeco (hour, minu, seco));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L +
            (long) (hour * 3600000 + minu * 60000 + seco * 1000));
  }  //dntCmilDateHourMinuSeco(int,int,int,int)

  //cmil = dntCmilDateHourMinuSecoMill (date, hour, minu, seco, mill)
  //  日付と時と分と秒とミリ秒から暦通ミリ秒を求める
  public static long dntCmilDateHourMinuSecoMill (int date,
                                                  int hour, int minu, int seco, int mill) {
    //return (dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dntDmilHourMinuSecoMill (hour, minu, seco, mill));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L +
            (long) (hour * 3600000 + minu * 60000 + seco * 1000 + mill));
  }  //dntCmilDateHourMinuSecoMill(int,int,int,int,int)

  //cmil = dntCmilDateTime (date, time)
  //  日付と時刻から暦通ミリ秒を求める
  public static long dntCmilDateTime (int date,
                                      int time) {
    //return (dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time)));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L +
            (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023)));
  }  //dntCmilDateTime(int,int)

  //cmil = dntCmilDttm (dttm)
  //  日時から暦通ミリ秒を求める
  public static long dntCmilDttm (long dttm) {
    //return dntCmilDateTime (dntDateDttm (dttm),
    //                        dntTimeDttm (dttm));
    int date = (int) (dttm >> 32);
    int time = (int) dttm;
    //return (dntCmilCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time)));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400000L +
            (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023)));
  }  //dntCmilDttm(long)

  //cmil = dntCmilNow ()
  //  現在の暦通ミリ秒を返す
  public static long dntCmilNow () {
    return System.currentTimeMillis ();
  }  //dntCmilNow()

  //cmil = dntCmilYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から暦通ミリ秒を求める
  public static long dntCmilYearMontMday (int year, int mont, int mday) {
    //return dntCmilCday (dntCdayYearMontMday (year, mont, mday));
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400000L;
  }  //dntCmilYearMontMday(int,int,int)

  //cmil = dntCmilYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayDmil (int year, int mont, int mday,
                                              int dmil) {
    //return (dntCmilCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dmil);
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400000L +
            (long) dmil);
  }  //dntCmilYearMontMdayDmil(int,int,int,int)

  //cmil = dntCmilYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayDsec (int year, int mont, int mday,
                                              int dsec) {
    //return (dntCmilCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dntDmilDsec (dsec));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400000L +
            (long) (dsec * 1000));
  }  //dntCmilYearMontMdayDsec(int,int,int,int)

  //cmil = dntCmilYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                      int hour, int minu, int seco) {
    //return (dntCmilCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dmilHourMinuSeco (hour, minu, seco));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400000L +
            (long) (hour * 3600000 + minu * 60000 + seco * 1000));
  }  //dntCmilYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //cmil = dntCmilYearMontMdayHourMinuSecoMill (year, mont, mday, hour, minu, seco, mill)
  //  西暦年と月と月通日と時と分と秒とミリ秒から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayHourMinuSecoMill (int year, int mont, int mday,
                                                          int hour, int minu, int seco, int mill) {
    //return (dntCmilCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dntDmilHourMinuSecoMill (hour, minu, seco, mill));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400000L +
            (long) (hour * 3600000 + minu * 60000 + seco * 1000 + mill));
  }  //dntCmilYearMontMdayHourMinuSecoMill(int,int,int,int,int,int,int)

  //cmil = dntCmilYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻から暦通ミリ秒を求める
  public static long dntCmilYearMontMdayTime (int year, int mont, int mday,
                                              int time) {
    //return (dntCmilCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time)));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400000L +
            (long) ((time >> 22) * 3600000 + (time >> 16 & 63) * 60000 + (time >> 10 & 63) * 1000 + (time & 1023)));
  }  //dntCmilYearMontMdayTime(int,int,int,int)


  //--------------------------------------------------------------------------------
  //csec 暦通秒

  //csec = dntCsecCday (cday)
  //  暦通日を暦通秒に変換する
  public static long dntCsecCday (int cday) {
    return (long) cday * 86400L;
  }  //dntCsecCday(int)

  //csec = dntCsecCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を暦通秒に変換する
  public static long dntCsecCdayDmil (int cday,
                                      int dmil) {
    //return (dntCsecCday (cday) +
    //        (long) dntDsecDmil (dmil));
    return ((long) cday * 86400L +
            (long) dntFdiv (dmil, 1000));
  }  //dntCsecCdayDmil(int,int)

  //csec = dntCsecCdayDsec (cday, dsec)
  //  暦通日と日通秒を暦通秒に変換する
  public static long dntCsecCdayDsec (int cday,
                                      int dsec) {
    //return (dntCsecCday (cday) +
    //        (long) dsec);
    return ((long) cday * 86400L +
            (long) dsec);
  }  //dntCsecCdayDsec(int,int)

  //csec = dntCsecCdayHourMinuSeco (cday, hour, minu, seco)
  //  歴通日と時と分と秒から暦通秒を求める
  public static long dntCsecCdayHourMinuSeco (int cday,
                                              int hour, int minu, int seco) {
    //return (dntCsecCday (cday) +
    //        (long) dntDsecHourMinuSeco (hour, minu, seco));
    return ((long) cday * 86400L +
            (long) (hour * 3600 + minu * 60 + seco));
  }  //dntCsecCdayHourMinuSeco(int,int,int,int)

  //csec = dntCsecCdayTime (cday, time)
  //  歴通日と時刻から暦通秒を求める
  public static long dntCsecCdayTime (int cday,
                                      int time) {
    //return (dntCsecCday (cday) +
    //        (long) dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time)));
    return ((long) cday * 86400L +
            (long) ((time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63)));
  }  //dntCsecCdayTime(int,int)

  //csec = dntCsecCmil (cmil)
  //  暦通ミリ秒を暦通秒に変換する
  public static long dntCsecCmil (long cmil) {
    return dntFdiv (cmil, 1000L);
  }  //dntCsecCmil(long)

  //csec = dntCsecDate (date)
  //  日付から暦通秒を求める
  public static long dntCsecDate (int date) {
    //return dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return (long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400L;
  }  //dntCsecDate(int)

  //csec = dntCsecDateDmil (date, dmil)
  //  日付と日通ミリ秒から暦通秒を求める
  public static long dntCsecDateDmil (int date,
                                      int dmil) {
    //return (dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dntDsecDmil (dmil));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400L +
            (long) dntFdiv (dmil, 1000));
  }  //dntCsecDateDmil(int,int)

  //csec = dntCsecDateDsec (date, dsec)
  //  日付と日通秒から暦通秒を求める
  public static long dntCsecDateDsec (int date,
                                      int dsec) {
    //return (dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        (long) dsec);
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400L +
            (long) dsec);
  }  //dntCsecDateDsec(int,int)

  //csec = dntCsecDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒から暦通秒を求める
  public static long dntCsecDateHourMinuSeco (int date,
                                              int hour, int minu, int seco) {
    //return (dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        dntDsecHourMinuSeco (hour, minu, seco));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400L +
            (long) (hour * 3600 + minu * 60 + seco));
  }  //dntCsecDateHourMinuSeco(int,int,int,int)

  //csec = dntCsecDateTime (date, time)
  //  日付と時刻から暦通秒を求める
  public static long dntCsecDateTime (int date,
                                      int time) {
    //return (dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time)));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400L +
            (long) ((time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63)));
  }  //dntCsecDateTime(int,int)

  //csec = dntCsecDttm (dttm)
  //  日時から暦通秒を求める
  public static long dntCsecDttm (long dttm) {
    //return dntCsecDateTime (dntDateDttm (dttm),
    //                        dntTimeDttm (dttm));
    int date = (int) (dttm >> 32);
    int time = (int) dttm;
    //return (dntCsecCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date))) +
    //        dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time)));
    return ((long) dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) * 86400L +
            (long) ((time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63)));
  }  //dntCsecDttm(long)

  //csec = dntCsecNow ()
  //  現在の暦通秒を返す
  public static long dntCsecNow () {
    //return dntCsecCmil (dntCmilNow ());
    return dntFdiv (System.currentTimeMillis (), 1000L);
  }  //dntCsecNow()

  //csec = dntCsecYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から暦通秒を求める
  public static long dntCsecYearMontMday (int year, int mont, int mday) {
    //return dntCsecCday (dntCdayYearMontMday (year, mont, mday));
    return (long) dntCdayYearMontMday (year, mont, mday) * 86400L;
  }  //dntCsecYearMontMday(int,int,int)

  //csec = dntCsecYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒から暦通秒を求める
  public static long dntCsecYearMontMdayDmil (int year, int mont, int mday,
                                              int dmil) {
    //return (dntCsecCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dntDsecDmil (dmil));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400L +
            (long) dntFdiv (dmil, 1000));
  }  //dntCsecYearMontMdayDmil(int,int,int,int)

  //csec = dntCsecYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒から暦通秒を求める
  public static long dntCsecYearMontMdayDsec (int year, int mont, int mday,
                                              int dsec) {
    //return (dntCsecCday (dntCdayYearMontMday (year, mont, mday)) +
    //        (long) dsec);
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400L +
            (long) dsec);
  }  //dntCsecYearMontMdayDsec(int,int,int,int)

  //csec = dntCsecYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒から暦通秒を求める
  public static long dntCsecYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                      int hour, int minu, int seco) {
    //return (dntCsecCday (dntCdayYearMontMday (year, mont, mday)) +
    //        dntDsecHourMinuSeco (hour, minu, seco));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400L +
            (long) (hour * 3600 + minu * 60 + seco));
  }  //dntCsecYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //csec = dntCsecYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻から暦通秒を求める
  public static long dntCsecYearMontMdayTime (int year, int mont, int mday,
                                              int time) {
    //return (dntCsecCday (dntCdayYearMontMday (year, mont, mday)) +
    //        dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time)));
    return ((long) dntCdayYearMontMday (year, mont, mday) * 86400L +
            (long) ((time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63)));
  }  //dntCsecYearMontMdayTime(int,int,int,int)


  //--------------------------------------------------------------------------------
  //date 日付

  //date = dntDateCday (cday)
  //  暦通日から日付を求める
  public static int dntDateCday (int cday) {
    int y400;
    int y100;
    if (cday < -141427) {  //1582年10月4日までユリウス暦
      cday += 719470;
      y400 = 0;
      y100 = 0;
    } else {  //1582年10月15日からグレゴリオ暦
      cday += 719468;
      y400 = dntFdiv (cday, 146097);
      cday -= y400 * 146097;
      y100 = dntFdiv (cday, 36524);
      if (3 < y100) {  //2000年2月29日の処理に必要
        y100 = 3;
      }
      cday -= y100 * 36524;
    }
    int y4 = dntFdiv (cday, 1461);
    cday -= y4 * 1461;
    int y1 = dntFdiv (cday, 365);
    if (3 < y1) {  //2004年2月29日の処理に必要
      y1 = 3;
    }
    cday = (cday - 365 * y1) * 10 + 922;
    int year = y400 * 400 + y100 * 100 + y4 * 4 + y1;
    int mont = dntFdiv (cday, 306);
    int mday = dntFdiv (cday - mont * 306, 10) + 1;
    if (12 < mont) {
      year++;
      mont -= 12;
    }
    return dntDateYearMontMday (year, mont, mday);
  }  //dntDateCday(int)

  //date = dntDateCmil (cmil)
  //  歴通ミリ秒から日付を求める
  public static int dntDateCmil (long cmil) {
    //return dntDateCday (dntCdayCmil (cmil));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L));
  }  //dntDateCmil(long)

  //date = dntDateCsec (csec)
  //  歴通秒から日付を求める
  public static int dntDateCsec (long csec) {
    //return dntDateCday (dntCdayCsec (csec));
    return dntDateCday ((int) dntFdiv (csec, 86400L));
  }  //dntDateCsec(long)

  //date = dntDateDttm (dttm)
  //  日時から日付を取り出す
  public static int dntDateDttm (long dttm) {
    return (int) (dttm >> 32);
  }  //dntDateDttm(long)

  //date = dntDateNow ()
  //  現在の日付を返す
  public static int dntDateNow () {
    //return dntDateCday (dntCdayCmil (dntCmilNow ()));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L));
  }  //dntDateNow()

  //date = dntDateYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から日付を作る
  public static int dntDateYearMontMday (int year, int mont, int mday) {
    return year << 10 | (mont & 15) << 6 | mday & 63;
  }  //dntDateYearMontMday(int,int,int)


  //--------------------------------------------------------------------------------
  //dmil 日通ミリ秒

  //dmil = dntDmilCmil (cmil)
  //  暦通ミリ秒から日通ミリ秒を取り出す
  public static int dntDmilCmil (long cmil) {
    return (int) dntFrem (cmil, 86400000L);
  }  //dntDmilCmil(long)

  //dmil = dntDmilCsec (csec)
  //  暦通秒から日通ミリ秒を求める
  public static int dntDmilCsec (long csec) {
    //return dntDmilDsec (dntDsecCsec (csec));
    return (int) dntFrem (csec, 86400L) * 1000;
  }  //dntDmilCsec(long)

  //dmil = dntDmilDsec (dsec)
  //  日通秒を日通ミリ秒に変換する
  public static int dntDmilDsec (int dsec) {
    return dsec * 1000;
  }  //dntDmilDsec(int)

  //dmil = dntDmilDttm (dttm)
  //  日時から日通ミリ秒を求める
  public static int dntDmilDttm (long dttm) {
    //return dntDmilHourMinuSecoMill (dntHourDttm (dttm), dntMinuDttm (dttm), dntSecoDttm (dttm), dntMillDttm (dttm));
    return ((int) dttm >> 22) * 3600 + ((int) dttm >> 16 & 63) * 60 + ((int) dttm >> 10 & 63) + ((int) dttm & 1023);
  }  //dntDmilDttm(long)

  //dmil = dntDmilHourMinuSeco (hour, minu, seco)
  //  時と分と秒を日通ミリ秒に変換する
  public static int dntDmilHourMinuSeco (int hour, int minu, int seco) {
    return hour * 3600000 + minu * 60000 + seco * 1000;
  }  //dntDmilHourMinuSeco(int,int,int)

  //dmil = dntDmilHourMinuSecoMill (hour, minu, seco, mill)
  //  時と分と秒とミリ秒を日通ミリ秒に変換する
  public static int dntDmilHourMinuSecoMill (int hour, int minu, int seco, int mill) {
    return hour * 3600000 + minu * 60000 + seco * 1000 + mill;
  }  //dntDmilHourMinuSecoMill(int,int,int,int)

  //dmil = dntDmilNow ()
  //  現在の日通ミリ秒を返す
  public static int dntDmilNow () {
    //return dntDmilCmil (dntCmilNow ());
    return (int) dntFrem (System.currentTimeMillis (), 86400000L);
  }  //dntDmilNow()

  //dmil = dntDmilTime (time)
  //  時刻を日通ミリ秒に変換する
  public static int dntDmilTime (int time) {
    //return dntDmilHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63) + (time & 1023);
  }  //dntDmilTime(int)


  //--------------------------------------------------------------------------------
  //dsec 日通秒

  //dsec = dntDsecCmil (cmil)
  //  暦通ミリ秒から日通秒を求める
  public static int dntDsecCmil (long cmil) {
    //return dntDsecDmil (dntDmilCmil (cmil));
    return dntFdiv ((int) dntFrem (cmil, 86400000L), 1000);
  }  //dntDsecCmil(long)

  //dsec = dntDsecCsec (csec)
  //  暦通秒から日通秒を求める
  public static int dntDsecCsec (long csec) {
    return (int) dntFrem (csec, 86400L);
  }  //dntDsecCsec(long)

  //dsec = dntDsecDmil (dmil)
  //  日通ミリ秒を日通秒に変換する
  public static int dntDsecDmil (int dmil) {
    return dntFdiv (dmil, 1000);
  }  //dntDsecDmil(int)

  //dsec = dntDsecDttm (dttm)
  //  日時から日通秒を求める
  public static int dntDsecDttm (long dttm) {
    //return dntDsecHourMinuSeco (dntHourDttm (dttm), dntMinuDttm (dttm), dntSecoDttm (dttm));
    return ((int) dttm >> 22) * 3600 + ((int) dttm >> 16 & 63) * 60 + ((int) dttm >> 10 & 63);
  }  //dntDsecDttm(long)

  //dsec = dntDsecHourMinuSeco (hour, minu, seco)
  //  時と分と秒を日通秒に変換する
  public static int dntDsecHourMinuSeco (int hour, int minu, int seco) {
    return hour * 3600 + minu * 60 + seco;
  }  //dntDsecHourMinuSeco(int,int,int)

  //dsec = dntDsecNow ()
  //  現在の日通秒を返す
  public static int dntDsecNow () {
    //return dntDsecDmil (dntDmilCmil (dntCmilNow ()));
    return dntFdiv ((int) dntFrem (System.currentTimeMillis (), 86400000L), 1000);
  }  //dntDsecNow()

  //dsec = dntDsecTime (time)
  //  時刻を日通秒に変換する
  public static int dntDsecTime (int time) {
    //return dntDsecHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return (time >> 22) * 3600 + (time >> 16 & 63) * 60 + (time >> 10 & 63);
  }  //dntDsecTime(int)


  //--------------------------------------------------------------------------------
  //dttm 日時

  //dttm = dntDttmCday (cday)
  //  歴通日から日時を作る
  public static long dntDttmCday (int cday) {
    //return dntDttmDate (dntDateCday (cday));
    return (long) dntDateCday (cday) << 32;
  }  //dntDttmCday(int)

  //dttm = dntDttmCdayDmil (cday, dmil)
  //  歴通日と日通ミリ秒から日時を作る
  public static long dntDttmCdayDmil (int cday,
                                      int dmil) {
    //return dntDttmDateTime (dntDateCday (cday),
    //                        dntTimeDmil (dmil));
    return ((long) dntDateCday (cday) << 32 |
            (long) dntTimeDmil (dmil) & 0xffffffffL);
  }  //dntDttmCdayDmil(int,int)

  //dttm = dntDttmCdayDsec (cday, dsec)
  //  歴通日と日通秒から日時を作る
  public static long dntDttmCdayDsec (int cday,
                                      int dsec) {
    //return dntDttmDateTime (dntDateCday (cday),
    //                        dntTimeDsec (dsec));
    return ((long) dntDateCday (cday) << 32 |
            (long) dntTimeDsec (dsec) & 0xffffffffL);
  }  //dntDttmCdayDsec(int,int)

  //dttm = dntDttmCdayHourMinuSeco (cday, hour, minu, seco)
  //  歴通日と時と分と秒から日時を作る
  public static long dntDttmCdayHourMinuSeco (int cday,
                                              int hour, int minu, int seco) {
    //return dntDttmDateTime (dntDateCday (cday),
    //                        dntTimeHourMinuSeco (hour, minu, seco));
    return ((long) dntDateCday (cday) << 32 |
            (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10) & 0xffffffffL);
  }  //dntDttmCdayHourMinuSeco(int,int,int,int)

  //dttm = dntDttmCdayHourMinuSecoMill (cday, hour, minu, seco, mill)
  //  歴通日と時と分と秒とミリ秒から日時を作る
  public static long dntDttmCdayHourMinuSecoMill (int cday,
                                                  int hour, int minu, int seco, int mill) {
    //return dntDttmDateTime (dntDateCday (cday),
   //                         dntTimeHourMinuSecoMill (hour, minu, seco, mill));
    return ((long) dntDateCday (cday) << 32 |
            (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023) & 0xffffffffL);
  }  //dntDttmCdayHourMinuSecoMill(int,int,int,int,int)

  //dttm = dntDttmCdayTime (cday, time)
  //  歴通日と時刻から日時を作る
  public static long dntDttmCdayTime (int cday,
                                      int time) {
    //return dntDttmDateTime (dntDateCday (cday),
    //                        time);
    return ((long) dntDateCday (cday) << 32 |
            (long) time & 0xffffffffL);
  }  //dntDttmCdayTime(int,int)

  //dttm = dntDttmCmil (cmil)
  //  暦通ミリ秒を日時に変換する
  public static long dntDttmCmil (long cmil) {
    //return dntDttmDateTime (dntDateCday (dntCdayCmil (cmil)),
    //                        dntTimeDmil (dntDmilCmil (cmil)));
    return ((long) dntDateCday ((int) dntFdiv (cmil, 86400000L)) << 32 |
            (long) dntTimeDmil ((int) dntFrem (cmil, 86400000L)) & 0xffffffffL);
  }  //dntDttmCmil(long)

  //dttm = dntDttmCsec (csec)
  //  暦通秒を日時に変換する
  public static long dntDttmCsec (long csec) {
    //return dntDttmDateTime (dntDateCday (dntCdayCsec (csec)),
    //                        dntTimeDsec (dntDsecCsec (csec)));
    return ((long) dntDateCday ((int) dntFdiv (csec, 86400L)) << 32 |
            (long) dntTimeDsec ((int) dntFrem (csec, 86400L)) & 0xffffffffL);
  }  //dntDttmCsec(long)

  //dttm = dntDttmDate (date)
  //  日付から日時を作る
  public static long dntDttmDate (int date) {
    return (long) date << 32;
  }  //dntDttmDate(int)

  //dttm = dntDttmDateDmil (date, dmil)
  //  日付と日通ミリ秒から日時を作る
  public static long dntDttmDateDmil (int date,
                                      int dmil) {
    //return dntDttmDateTime (date,
    //                        dntTimeDmil (dmil));
    return ((long) date << 32 |
            (long) dntTimeDmil (dmil) & 0xffffffffL);
  }  //dntDttmDateDmil(int,int)

  //dttm = dntDttmDateDsec (date, dsec)
  //  日付と日通秒から日時を作る
  public static long dntDttmDateDsec (int date,
                                      int dsec) {
    //return dntDttmDateTime (date,
    //                        dntTimeDsec (dsec));
    return ((long) date << 32 |
            (long) dntTimeDsec (dsec) & 0xffffffffL);
  }  //dntDttmDateDsec(int,int)

  //dttm = dntDttmDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒から日時を作る
  public static long dntDttmDateHourMinuSeco (int date,
                                              int hour, int minu, int seco) {
    //return dntDttmDateTime (date,
    //                        dntTimeHourMinuSeco (hour, minu, seco));
    return ((long) date << 32 |
            (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10) & 0xffffffffL);
  }  //dntDttmDateHourMinuSeco(int,int,int,int)

  //dttm = dntDttmDateHourMinuSecoMill (date, hour, minu, seco, mill)
  //  日付と時と分と秒とミリ秒から日時を作る
  public static long dntDttmDateHourMinuSecoMill (int date,
                                                  int hour, int minu, int seco, int mill) {
    //return dntDttmDateTime (date,
    //                        dntTimeHourMinuSecoMill (hour, minu, seco, mill));
    return ((long) date << 32 |
            (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023) & 0xffffffffL);
  }  //dntDttmDateHourMinuSecoMill(int,int,int,int,int)

  //dttm = dntDttmDateTime (date, time)
  //  日付と時刻から日時を作る
  public static long dntDttmDateTime (int date,
                                      int time) {
    return ((long) date << 32 |
            (long) time & 0xffffffffL);
  }  //dntDttmDateTime(int,int)

  //dttm = dntDttmNow ()
  //  現在の日時を返す
  public static long dntDttmNow () {
    //return dntDttmCmil (System.currentTimeMillis ());
    long cmil = System.currentTimeMillis ();
    return ((long) dntDateCday ((int) dntFdiv (cmil, 86400000L)) << 32 |
            (long) dntTimeDmil ((int) dntFrem (cmil, 86400000L)) & 0xffffffffL);
  }  //dntDttmNow()

  //dttm = dntDttmYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から日時を作る
  public static long dntDttmYearMontMday (int year, int mont, int mday) {
    //return dntDttmDate (dntDateYearMontMday (year, mont, mday));
    return (long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32;
  }  //dntDttmYearMontMday(int,int,int)

  //dttm = dntDttmYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒から日時を作る
  public static long dntDttmYearMontMdayDmil (int year, int mont, int mday,
                                              int dmil) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday),
    //                        dntTimeDmil (dmil));
    return ((long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 |
            (long) dntTimeDmil (dmil) & 0xffffffffL);
  }  //dntDttmYearMontMdayDmil(int,int,int,int)

  //dttm = dntDttmYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒から日時を作る
  public static long dntDttmYearMontMdayDsec (int year, int mont, int mday,
                                              int dsec) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday),
    //                        dntTimeDsec (dsec));
    return ((long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 |
            (long) dntTimeDsec (dsec) & 0xffffffffL);
  }  //dntDttmYearMontMdayDsec(int,int,int,int)

  //dttm = dntDttmYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒から日時を作る
  public static long dntDttmYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                      int hour, int minu, int seco) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday),
    //                        dntTimeHourMinuSeco (hour, minu, seco));
    return ((long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 |
            (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10) & 0xffffffffL);
  }  //dntDttmYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //dttm = dntDttmYearMontMdayHourMinuSecoMill (year, mont, mday, hour, minu, seco, mill)
  //  西暦年と月と月通日と時と分と秒とミリ秒から日時を作る
  public static long dntDttmYearMontMdayHourMinuSecoMill (int year, int mont, int mday,
                                                          int hour, int minu, int seco, int mill) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday),
    //                        dntTimeHourMinuSecoMill (hour, minu, seco, mill));
    return ((long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 |
            (long) (hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023) & 0xffffffffL);
  }  //dntDttmYearMontMdayHourMinuSecoMill(int,int,int,int,int,int,int)

  //dttm = dntDttmYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻から日時を作る
  public static long dntDttmYearMontMdayTime (int year, int mont, int mday,
                                              int time) {
    //return dntDttmDateTime (dntDateYearMontMday (year, mont, mday),
    //                        time);
    return ((long) (year << 10 | (mont & 15) << 6 | mday & 63) << 32 |
            (long) time & 0xffffffffL);
  }  //dntDttmYearMontMdayTime(int,int,int,int)


  //--------------------------------------------------------------------------------
  //hour 時

  //hour = dntHourCmil (cmil)
  //  歴通ミリ秒から時を求める
  public static int dntHourCmil (long cmil) {
    //return (int) dntFrem (cmil, 86400000L) / 3600000;
    //perl optdiv.pl 86399999 3600000
    //  x/3600000==x*39093747>>>47 (0<=x<=169199998) [86399999*39093747==3377699701706253]
    return (int) (dntFrem (cmil, 86400000L) * 39093747L >>> 47);
  }  //dntHourCmil(long)

  //hour = dntHourCsec (csec)
  //  歴通秒から時を求める
  public static int dntHourCsec (long csec) {
    //return (int) dntFrem (csec, 86400L) / 3600;
    //perl optdiv.pl 86399 3600
    //  x/3600==x*37283>>>27 (0<=x<=125998) [86399*37283==3221213917]
    return (int) (dntFrem (csec, 86400L) * 37283L >>> 27);
  }  //dntHourCsec(long)

  //hour = dntHourDmil (dmil)
  //  日通ミリ秒から時を求める
  public static int dntHourDmil (int dmil) {
    return dntFdiv (dmil, 3600000);
  }  //dntHourDmil(int)

  //hour = dntHourDsec (dsec)
  //  日通秒から時を求める
  public static int dntHourDsec (int dsec) {
    return dntFdiv (dsec, 3600);
  }  //dntHourDsec(int)

  //hour = dntHourDttm (dttm)
  //  日時から時を取り出す
  public static int dntHourDttm (long dttm) {
    return (int) dttm >> 22;
  }  //dntHourDttm(long)

  //hour = dntHourNow ()
  //  現在の時を返す
  public static int dntHourNow () {
    //return dntHourCmil (dntCmilNow ());
    //return (int) dntFrem (System.currentTimeMillis (), 86400000L) / 3600000;
    //perl optdiv.pl 86399999 3600000
    //  x/3600000==x*39093747>>>47 (0<=x<=169199998) [86399999*39093747==3377699701706253]
    return (int) (dntFrem (System.currentTimeMillis (), 86400000L) * 39093747L >>> 47);
  }  //dntHourNow()

  //hour = dntHourTime (time)
  //  時刻から時を取り出す
  public static int dntHourTime (int time) {
    return time >> 22;
  }  //dntHourTime(int)


  //--------------------------------------------------------------------------------
  //mday 月通日

  //mday = dntMdayCday (cday)
  //  歴通日から月通日を求める
  public static int dntMdayCday (int cday) {
    //return dntMdayDate (dntDateCday (cday));
    return dntDateCday (cday) & 63;
  }  //dntMdayCday(int)

  //mday = dntMdayCmil (cmil)
  //  歴通ミリ秒から月通日を求める
  public static int dntMdayCmil (long cmil) {
    //return dntMdayDate (dntDateCday (dntCdayCmil (cmil)));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L)) & 63;
  }  //dntMdayCmil(long)

  //mday = dntMdayCsec (csec)
  //  歴通秒から月通日を求める
  public static int dntMdayCsec (long csec) {
    //return dntMdayDate (dntDateCday (dntCdayCsec (csec)));
    return dntDateCday ((int) dntFdiv (csec, 86400L)) & 63;
  }  //dntMdayCsec(long)

  //mday = dntMdayDate (date)
  //  日付から月通日を取り出す
  public static int dntMdayDate (int date) {
    return date & 63;
  }  //dntMdayDate(int)

  //mday = dntMdayDttm (dttm)
  //  日時から月通日を取り出す
  public static int dntMdayDttm (long dttm) {
    return (int) (dttm >> 32) & 63;
  }  //dntMdayDttm(long)

  //mday = dntMdayNow ()
  //  現在の月通日を返す
  public static int dntMdayNow () {
    //return dntMdayDate (dntDateCday (dntCdayCmil (dntCmilNow ())));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L)) & 63;
  }  //dntMdayNow()


  //--------------------------------------------------------------------------------
  //mill ミリ秒

  //mill = dntMillCmil (cmil)
  //  歴通ミリ秒からミリ秒を求める
  public static int dntMillCmil (long cmil) {
    return (int) dntFrem (cmil, 1000L);
  }  //dntMillCmil(long)

  //mill = dntMillDmil (dmil)
  //  日通ミリ秒からミリ秒を求める
  public static int dntMillDmil (int dmil) {
    return dntFrem (dmil, 1000);
  }  //dntMillDmil(int)

  //mill = dntMillDttm (dttm)
  //  日時からミリ秒を取り出す
  public static int dntMillDttm (long dttm) {
    return (int) dttm & 1023;
  }  //dntMillDttm(long)

  //mill = dntMillNow ()
  //  現在のミリ秒を返す
  public static int dntMillNow () {
    //return dntMillCmil (dntCmilNow ());
    return (int) dntFrem (System.currentTimeMillis (), 1000L);
  }  //dntMillNow()

  //mill = dntMillTime (time)
  //  時刻からミリ秒を取り出す
  public static int dntMillTime (int time) {
    return time & 1023;
  }  //dntMillTime(int)


  //--------------------------------------------------------------------------------
  //minu 分

  //minu = dntMinuCmil (cmil)
  //  歴通ミリ秒から分を求める
  public static int dntMinuCmil (long cmil) {
    //return (int) dntFrem (cmil, 3600000L) / 60000;
    //perl optdiv.pl 3599999 60000
    //  x/60000==x*4581299>>>38 (0<=x<=8339998) [3599999*4581299==16492671818701]
    return (int) (dntFrem (cmil, 3600000L) * 4581299L >>> 38);
  }  //dntMinuCmil(long)

  //minu = dntMinuCsec (csec)
  //  歴通秒から分を求める
  public static int dntMinuCsec (long csec) {
    //return (int) dntFrem (csec, 3600L) / 60;
    //perl optdiv.pl 3599 60
    //  x/60==x*2185>>>17 (0<=x<=4738) [3599*2185==7863815]
    return (int) dntFrem (csec, 3600L) * 2185 >>> 17;
  }  //dntMinuCsec(long)

  //minu = dntMinuDmil (dmil)
  //  日通ミリ秒から分を求める
  public static int dntMinuDmil (int dmil) {
    //return dntFrem (dmil, 3600000) / 60000;
    //perl optdiv.pl 3599999 60000
    //  x/60000==x*4581299>>>38 (0<=x<=8339998) [3599999*4581299==16492671818701]
    return (int) ((long) dntFrem (dmil, 3600000) * 4581299L >>> 38);
  }  //dntMinuDmil(int)

  //minu = dntMinuDsec (dsec)
  //  日通秒から分を求める
  public static int dntMinuDsec (int dsec) {
    //perl optdiv.pl 3599 60
    //  x/60==x*2185>>>17 (0<=x<=4738) [3599*2185==7863815]
    //return dntFrem (dsec, 3600) / 60;
    return dntFrem (dsec, 3600) * 2185 >>> 17;
  }  //dntMinuDsec(int)

  //minu = dntMinuDttm (dttm)
  //  日時から分を取り出す
  public static int dntMinuDttm (long dttm) {
    return (int) dttm >> 16 & 63;
  }  //dntMinuDttm(long)

  //minu = dntMinuNow ()
  //  現在の分を返す
  public static int dntMinuNow () {
    //return dntMinuCmil (dntCmilNow ());
    //return (int) dntFrem (System.currentTimeMillis (), 3600000L) / 60000;
    //perl optdiv.pl 3599999 60000
    //  x/60000==x*4581299>>>38 (0<=x<=8339998) [3599999*4581299==16492671818701]
    return (int) (dntFrem (System.currentTimeMillis (), 3600000L) * 4581299L >>> 38);
  }  //dntMinuNow()

  //minu = dntMinuTime (time)
  //  時刻から分を取り出す
  public static int dntMinuTime (int time) {
    return time >> 16 & 63;
  }  //dntMinuTime(int)


  //--------------------------------------------------------------------------------
  //mont 月

  //mont = dntMontCday (cday)
  //  歴通日から月を求める
  public static int dntMontCday (int cday) {
    //return dntMontDate (dntDateCday (cday));
    return dntDateCday (cday) >> 6 & 15;
  }  //dntMontCday(int)

  //mont = dntMontCmil (cmil)
  //  歴通ミリ秒から月を求める
  public static int dntMontCmil (long cmil) {
    //return dntMontDate (dntDateCday (dntCdayCmil (cmil)));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L)) >> 6 & 15;
  }  //dntMontCmil(long)

  //mont = dntMontCsec (csec)
  //  歴通秒から月を求める
  public static int dntMontCsec (long csec) {
    //return dntMontDate (dntDateCday (dntCdayCsec (csec)));
    return dntDateCday ((int) dntFdiv (csec, 86400L)) >> 6 & 15;
  }  //dntMontCsec(long)

  //mont = dntMontDate (date)
  //  日付から月を取り出す
  public static int dntMontDate (int date) {
    return date >> 6 & 15;
  }  //dntMontDate(int)

  //mont = dntMontDttm (dttm)
  //  日時から月を取り出す
  public static int dntMontDttm (long dttm) {
    return (int) (dttm >> 38) & 15;
  }  //dntMontDttm(long)

  //mont = dntMontNow ()
  //  現在の月を返す
  public static int dntMontNow () {
    //return dntMontDate (dntDateCday (dntCdayCmil (dntCmilNow ())));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L)) >> 6 & 15;
  }  //dntMontNow()


  //--------------------------------------------------------------------------------
  //sdt 日付文字列(year-mo-md)

  //sdt = dntSdtCday (cday)
  //  暦通日を日付文字列(year-mo-md)に変換する
  public static String dntSdtCday (int cday) {
    return dntSdtDate (dntDateCday (cday));
  }  //dntSdtCday(int)

  //sdt = dntSdtCmil (cmil)
  //  暦通ミリ秒を日付文字列(year-mo-md)に変換する
  public static String dntSdtCmil (long cmil) {
    return dntSdtDate (dntDateCmil (cmil));
  }  //dntSdtCmil(long)

  //sdt = dntSdtCsec (csec)
  //  暦通秒を日付文字列(year-mo-md)に変換する
  public static String dntSdtCsec (long csec) {
    return dntSdtDate (dntDateCsec (csec));
  }  //dntSdtCsec(long)

  //sdt = dntSdtDate (date)
  //  日付を日付文字列(year-mo-md)に変換する
  public static String dntSdtDate (int date) {
    //return dntSdtYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntSdtYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntSdtDate(int)

  //sdt = dntSdtDttm (dttm)
  //  日時を日付文字列(year-mo-md)に変換する
  public static String dntSdtDttm (long dttm) {
    //return dntSdtDate (dntDateDttm (dttm));
    return dntSdtDate ((int) (dttm >> 32));
  }  //dntSdtDttm(long)

  //sdt = dntSdtNow ()
  //  現在の日付文字列(year-mo-md)を返す
  public static String dntSdtNow () {
    return dntSdtDate (dntDateNow ());
  }  //dntSdtNow()

  //sdt = dntSdtYearMontMday (year, mont, mday)
  //  西暦年と月と月通日を日付文字列(year-mo-md)に変換する
  public static String dntSdtYearMontMday (int year, int mont, int mday) {
    return String.format ("%04d-%02d-%02d", year, mont, mday);
  }  //dntSdtYearMontMday(int,int,int)


  //--------------------------------------------------------------------------------
  //sdw 日付文字列(year-mo-md (Www))

  //sdw = dntSdwCday (cday)
  //  暦通日を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwCday (int cday) {
    return dntSdwDateWday (dntDateCday (cday), dntWdayCday (cday));
  }  //dntSdwCday(int)

  //sdt = dntSdwCmil (cmil)
  //  暦通ミリ秒を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwCmil (long cmil) {
    return dntSdwDateWday (dntDateCmil (cmil), dntWdayCmil (cmil));
  }  //dntSdwCmil(long)

  //sdt = dntSdwCsec (csec)
  //  暦通秒を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwCsec (long csec) {
    return dntSdwDateWday (dntDateCsec (csec), dntWdayCsec (csec));
  }  //dntSdwCsec(long)

  //sdw = dntSdwDate (date)
  //  日付を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwDate (int date) {
    //return dntSdwYearMontMdayWday (dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayDate (date));
    return dntSdwYearMontMdayWday (date >> 10, date >> 6 & 15, date & 63, dntWdayDate (date));
  }  //dntSdwDate(int)

  //sdw = dntSdwDateWday (date, wday)
  //  日付と曜日を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwDateWday (int date, int wday) {
    //return dntSdwYearMontMdayWday (dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday);
    return dntSdwYearMontMdayWday (date >> 10, date >> 6 & 15, date & 63, wday);
  }  //dntSdwDateWday(int,int)

  //sdw = dntSdwDttm (dttm)
  //  日時を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwDttm (long dttm) {
    //return dntSdwDate (dntDateDttm (dttm));
    return dntSdwDate ((int) (dttm >> 32));
  }  //dntSdwDttm(long)

  //sdw = dntSdwDttmWday (dttm, wday)
  //  日時と曜日を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwDttmWday (long dttm, int wday) {
    //return dntSdwDateWday (dntDateDttm (dttm), wday);
    return dntSdwDateWday ((int) (dttm >> 32), wday);
  }  //dntSdwDttmWday(long,int)

  //sdw = dntSdwNow ()
  //  現在の日付文字列(year-mo-md (Www))を返す
  public static String dntSdwNow () {
    return dntSdwDate (dntDateNow ());
  }  //dntSdwNow()

  //sdw = dntSdwYearMontMday (year, mont, mday)
  //  西暦年と月と月通日を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwYearMontMday (int year, int mont, int mday) {
    return dntSdwYearMontMdayWday (year, mont, mday, dntWdayYearMontMday (year, mont, mday));
  }  //dntSdwYearMontMday(int,int,int)

  //sdw = dntSdwYearMontMdayWday (year, mont, mday, wday)
  //  西暦年と月と月通日と曜日を日付文字列(year-mo-md (Www))に変換する
  public static String dntSdwYearMontMdayWday (int year, int mont, int mday, int wday) {
    return String.format ("%04d-%02d-%02d (%s)", year, mont, mday, DNT_ENWDAY3_WDAY[wday]);
  }  //dntSdwYearMontMdayWday(int,int,int,int)


  //--------------------------------------------------------------------------------
  //sdttl 日時文字列(year-mo-md ho:mi:se)

  //sdttl = dntSdttlCday (cday)
  //  暦通日を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCday (int cday) {
    return dntSdttlDateTime (dntDateCday (cday),
                             0);
  }  //dntSdttlCday(int)

  //sdttl = dntSdttlCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCdayDmil (int cday,
                                         int dmil) {
    return dntSdttlDateTime (dntDateCday (cday),
                             dntTimeDmil (dmil));
  }  //dntSdttlCdayDmil(int,int)

  //sdttl = dntSdttlCdayDsec (cday, dsec)
  //  暦通日と日通秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCdayDsec (int cday,
                                         int dsec) {
    return dntSdttlDateTime (dntDateCday (cday),
                             dntTimeDsec (dsec));
  }  //dntSdttlCdayDsec(int,int)

  //sdttl = dntSdttlCdayHourMinuSeco (cday, hour, minu, seco)
  //  暦通日と時と分と秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCdayHourMinuSeco (int cday,
                                                 int hour, int minu, int seco) {
    int date = dntDateCday (cday);
    //return dntSdttlYearMontMdayHourMinuSeco (dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //                                         hour, minu, seco);
    return dntSdttlYearMontMdayHourMinuSeco (date >> 10, date >> 6 & 15, date & 63,
                                             hour, minu, seco);
  }  //dntSdttlCdayHourMinuSeco(int,int,int,int)

  //sdttl = dntSdttlCdayTime (cday, time)
  //  暦通日と時刻を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCdayTime (int cday,
                                         int time) {
    return dntSdttlDateTime (dntDateCday (cday),
                             time);
  }  //dntSdttlCdayTime(int,int)

  //sdttl = dntSdttlCmil (cmil)
  //  暦通ミリ秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCmil (long cmil) {
    return dntSdttlDttm (dntDttmCmil (cmil));
  }  //dntSdttlCmil(long)

  //sdttl = dntSdttlCsec (csec)
  //  暦通秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlCsec (long csec) {
    return dntSdttlDttm (dntDttmCsec (csec));
  }  //dntSdttlCsec(long)

  //sdttl = dntSdttlDate (date)
  //  日付を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlDate (int date) {
    return dntSdttlDateTime (date,
                             0);
  }  //dntSdttlDate(int)

  //sdttl = dntSdttlDateDmil (date, dmil)
  //  日付と日通ミリ秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlDateDmil (int date,
                                         int dmil) {
    return dntSdttlDateTime (date,
                             dntTimeDmil (dmil));
  }  //dntSdttlDateDmil(int,int)

  //sdttl = dntSdttlDateDsec (date, dsec)
  //  日付と日通秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlDateDsec (int date,
                                         int dsec) {
    return dntSdttlDateTime (date,
                             dntTimeDsec (dsec));
  }  //dntSdttlDateDsec(int,int)

  //sdttl = dntSdttlDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlDateHourMinuSeco (int date,
                                                 int hour, int minu, int seco) {
    //return dntSdttlYearMontMdayHourMinuSeco (dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //                                         hour, minu, seco);
    return dntSdttlYearMontMdayHourMinuSeco (date >> 10, date >> 6 & 15, date & 63,
                                             hour, minu, seco);
  }  //dntSdttlDateHourMinuSeco(int,int,int,int)

  //sdttl = dntSdttlDateTime (date, time)
  //  日付と時刻を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlDateTime (int date,
                                       int time) {
    //return dntSdttlYearMontMdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdttlYearMontMdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdttlDateTime(int,int)

  //sdttl = dntSdttlDttm (dttm)
  //  日時を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlDttm (long dttm) {
    //return dntSdttlDateTime (dntDateDttm (dttm),
    //                         dntTimeDttm (dttm));
    int date = (int) (dttm >> 32);
    int time = (int) dttm;
    //return dntSdttlYearMontMdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdttlYearMontMdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdttlDttm(long)

  //sdttl = dntSdttlNow ()
  //  現在の日時文字列(year-mo-md ho:mi:se)を返す
  public static String dntSdttlNow () {
    return dntSdttlDttm (dntDttmNow ());
  }  //dntSdttlNow()

  //sdttl = dntSdttlYearMontMday (year, mont, mday)
  //  西暦年と月と月通日を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlYearMontMday (int year, int mont, int mday) {
    return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
                                             0, 0, 0);
  }  //dntSdttlYearMontMday(int,int,int)

  //sdttl = dntSdttlYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlYearMontMdayDmil (int year, int mont, int mday,
                                               int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
    //                                         dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
                                             time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdttlYearMontMdayDmil(int,int,int,int)

  //sdttl = dntSdttlYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlYearMontMdayDsec (int year, int mont, int mday,
                                               int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
    //                                         dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
                                             time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdttlYearMontMdayDsec(int,int,int,int)

  //sdttl = dntSdttlYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒とミリ秒を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                         int hour, int minu, int seco) {
    return String.format ("%04d-%02d-%02d %02d:%02d:%02d", year, mont, mday, hour, minu, seco);
  }  //dntSdttlYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //sdttl = dntSdttlYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻を日時文字列(year-mo-md ho:mi:se)に変換する
  public static String dntSdttlYearMontMdayTime (int year, int mont, int mday,
                                                 int time) {
    //return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
    //                                         dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdttlYearMontMdayHourMinuSeco (year, mont, mday,
                                             time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdttlYearMontMdayTime(int,int,int,int)


  //--------------------------------------------------------------------------------
  //sdttm 日時文字列(year-mo-md ho:mi:se.mil)

  //sdttm = dntSdttmCday (cday)
  //  暦通日を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCday (int cday) {
    return dntSdttmDateTime (dntDateCday (cday),
                             0);
  }  //dntSdttmCday(int)

  //sdttm = dntSdttmCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCdayDmil (int cday,
                                         int dmil) {
    return dntSdttmDateTime (dntDateCday (cday),
                             dntTimeDmil (dmil));
  }  //dntSdttmCdayDmil(int,int)

  //sdttm = dntSdttmCdayDsec (cday, dsec)
  //  暦通日と日通秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCdayDsec (int cday,
                                         int dsec) {
    return dntSdttmDateTime (dntDateCday (cday),
                             dntTimeDsec (dsec));
  }  //dntSdttmCdayDsec(int,int)

  //sdttm = dntSdttmCdayHourMinuSeco (cday, hour, minu, seco)
  //  暦通日と時と分と秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCdayHourMinuSeco (int cday,
                                                 int hour, int minu, int seco) {
    int date = dntDateCday (cday);
    //return dntSdttmYearMontMdayHourMinuSecoMill (dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //                                             hour, minu, seco, 0);
    return dntSdttmYearMontMdayHourMinuSecoMill (date >> 10, date >> 6 & 15, date & 63,
                                                 hour, minu, seco, 0);
  }  //dntSdttmCdayHourMinuSeco(int,int,int,int)

  //sdttm = dntSdttmCdayHourMinuSecoMill (cday, hour, minu, seco, mill)
  //  暦通日と時と分と秒とミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCdayHourMinuSecoMill (int cday,
                                                     int hour, int minu, int seco, int mill) {
    int date = dntDateCday (cday);
    //return dntSdttmYearMontMdayHourMinuSecoMill (dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //                                             hour, minu, seco, mill);
    return dntSdttmYearMontMdayHourMinuSecoMill (date >> 10, date >> 6 & 15, date & 63,
                                                 hour, minu, seco, mill);
  }  //dntSdttmCdayHourMinuSecoMill(int,int,int,int,int)

  //sdttm = dntSdttmCdayTime (cday, time)
  //  暦通日と時刻を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCdayTime (int cday,
                                         int time) {
    return dntSdttmDateTime (dntDateCday (cday),
                             time);
  }  //dntSdttmCdayTime(int,int)

  //sdttm = dntSdttmCmil (cmil)
  //  暦通ミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCmil (long cmil) {
    return dntSdttmDttm (dntDttmCmil (cmil));
  }  //dntSdttmCmil(long)

  //sdttm = dntSdttmCsec (csec)
  //  暦通秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmCsec (long csec) {
    return dntSdttmDttm (dntDttmCsec (csec));
  }  //dntSdttmCsec(long)

  //sdttm = dntSdttmDate (date)
  //  日付を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDate (int date) {
    return dntSdttmDateTime (date,
                             0);
  }  //dntSdttmDate(int)

  //sdttm = dntSdttmDateDmil (date, dmil)
  //  日付と日通ミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDateDmil (int date,
                                         int dmil) {
    return dntSdttmDateTime (date,
                             dntTimeDmil (dmil));
  }  //dntSdttmDateDmil(int,int)

  //sdttm = dntSdttmDateDsec (date, dsec)
  //  日付と日通秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDateDsec (int date,
                                         int dsec) {
    return dntSdttmDateTime (date,
                             dntTimeDsec (dsec));
  }  //dntSdttmDateDsec(int,int)

  //sdttm = dntSdttmDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDateHourMinuSeco (int date,
                                                 int hour, int minu, int seco) {
    //return dntSdttmYearMontMdayHourMinuSecoMill (dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //                                             hour, minu, seco, 0);
    return dntSdttmYearMontMdayHourMinuSecoMill (date >> 10, date >> 6 & 15, date & 63,
                                                 hour, minu, seco, 0);
  }  //dntSdttmDateHourMinuSeco(int,int,int,int)

  //sdttm = dntSdttmDateHourMinuSecoMill (date, hour, minu, seco, mill)
  //  日付と時と分と秒とミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDateHourMinuSecoMill (int date,
                                                     int hour, int minu, int seco, int mill) {
    //return dntSdttmYearMontMdayHourMinuSecoMill (dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //                                             hour, minu, seco, mill);
    return dntSdttmYearMontMdayHourMinuSecoMill (date >> 10, date >> 6 & 15, date & 63,
                                                 hour, minu, seco, mill);
  }  //dntSdttmDateHourMinuSeco(int,int,int,int,int)

  //sdttm = dntSdttmDateTime (date, time)
  //  日付と時刻を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDateTime (int date,
                                       int time) {
    //return dntSdttmYearMontMdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdttmYearMontMdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdttmDateTime(int,int)

  //sdttm = dntSdttmDttm (dttm)
  //  日時を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmDttm (long dttm) {
    //return dntSdttmDateTime (dntDateDttm (dttm),
    //                         dntTimeDttm (dttm));
    int date = (int) (dttm >> 32);
    int time = (int) dttm;
    //return dntSdttmYearMontMdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdttmYearMontMdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdttmDttm(long)

  //sdttm = dntSdttmNow ()
  //  現在の日時文字列(year-mo-md ho:mi:se.mil)を返す
  public static String dntSdttmNow () {
    return dntSdttmDttm (dntDttmNow ());
  }  //dntSdttmNow()

  //sdttm = dntSdttmYearMontMday (year, mont, mday)
  //  西暦年と月と月通日を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmYearMontMday (int year, int mont, int mday) {
    return dntSdttmYearMontMdayHourMinuSecoMill (year, mont, mday,
                                                 0, 0, 0, 0);
  }  //dntSdttmYearMontMday(int,int,int)

  //sdttm = dntSdttmYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmYearMontMdayDmil (int year, int mont, int mday,
                                                 int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdttmYearMontMdayHourMinuSecoMill (
    //  year, mont, mday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdttmYearMontMdayHourMinuSecoMill (
      year, mont, mday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdttmYearMontMdayDmil(int,int,int,int)

  //sdttm = dntSdttmYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmYearMontMdayDsec (int year, int mont, int mday,
                                                 int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdttmYearMontMdayHourMinuSecoMill (
    //  year, mont, mday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdttmYearMontMdayHourMinuSecoMill (
      year, mont, mday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdttmYearMontMdayDsec(int,int,int,int)

  //sdttm = dntSdttmYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                         int hour, int minu, int seco) {
    return dntSdttmYearMontMdayHourMinuSecoMill (year, mont, mday,
                                                 hour, minu, seco, 0);
  }  //dntSdttmYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //sdttm = dntSdttmYearMontMdayHourMinuSecoMill (year, mont, mday, hour, minu, seco, mill)
  //  西暦年と月と月通日と時と分と秒とミリ秒を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmYearMontMdayHourMinuSecoMill (int year, int mont, int mday,
                                                             int hour, int minu, int seco, int mill) {
    return String.format ("%04d-%02d-%02d %02d:%02d:%02d.%03d", year, mont, mday, hour, minu, seco, mill);
  }  //dntSdttmYearMontMdayHourMinuSecoMill(int,int,int,int,int,int,int)

  //sdttm = dntSdttmYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻を日時文字列(year-mo-md ho:mi:se.mil)に変換する
  public static String dntSdttmYearMontMdayTime (int year, int mont, int mday,
                                               int time) {
    //return dntSdttmYearMontMdayHourMinuSecoMill (
    //  year, mont, mday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdttmYearMontMdayHourMinuSecoMill (
      year, mont, mday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdttmYearMontMdayTime(int,int,int,int)


  //--------------------------------------------------------------------------------
  //sdwtl 日時文字列(year-mo-md (Www) ho:mi:se)

  //sdwtl = dntSdwtlCday (cday)
  //  暦通日を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCday (int cday) {
    return dntSdwtlDttmWday (dntDttmCday (cday), dntWdayCday (cday));
  }  //dntSdwtlCday(int)

  //sdwtl = dntSdwtlCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCdayDmil (int cday, int dmil) {
    return dntSdwtlDateWdayTime (dntDateCday (cday), dntWdayCday (cday),
                                 dntTimeDmil (dmil));
  }  //dntSdwtlCdayDmil(int,int)

  //sdwtl = dntSdwtlCdayDsec (cday, dsec)
  //  暦通日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCdayDsec (int cday,
                                         int dsec) {
    return dntSdwtlDateWdayTime (dntDateCday (cday), dntWdayCday (cday),
                                 dntTimeDsec (dsec));
  }  //dntSdwtlCdayDsec(int,int)

  //sdwtl = dntSdwtlCdayHourMinuSeco (cday, hour, minu, seco)
  //  暦通日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCdayHourMinuSeco (int cday,
                                                 int hour, int minu, int seco) {
    int date = dntDateCday (cday);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayCday (cday),
    //  hour, minu, seco);
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, dntWdayCday (cday),
      hour, minu, seco);
  }  //dntSdwtlCdayHourMinuSeco(int,int)

  //sdwtl = dntSdwtlCdayTime (cday, time)
  //  暦通日と時刻を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCdayTime (int cday,
                                         int time) {
    return dntSdwtlDateWdayTime (dntDateCday (cday), dntWdayCday (cday),
                                 time);
  }  //dntSdwtlCdayTime(int,int)

  //sdwtl = dntSdwtlCmil (cmil)
  //  暦通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCmil (long cmil) {
    return dntSdwtlDttmWday (dntDttmCmil (cmil), dntWdayCmil (cmil));
  }  //dntSdwtlCmil(long)

  //sdwtl = dntSdwtlCsec (csec)
  //  暦通秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlCsec (long csec) {
    return dntSdwtlDttmWday (dntDttmCsec (csec), dntWdayCsec (csec));
  }  //dntSdwtlCsec(long)

  //sdwtl = dntSdwtlDate (date)
  //  日付を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDate (int date) {
    return dntSdwtlDateWdayTime (date, dntWdayDate (date),
                                 0);
  }  //dntSdwtlDate(int)

  //sdwtl = dntSdwtlDateDmil (date, dmil)
  //  日付と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateDmil (int date, int dmil) {
    return dntSdwtlDateWdayTime (date, dntWdayDate (date),
                                 dntTimeDmil (dmil));
  }  //dntSdwtlDateDmil(int,int)

  //sdwtl = dntSdwtlDateDsec (date, dsec)
  //  日付と日通秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateDsec (int date, int dsec) {
    return dntSdwtlDateWdayTime (date, dntWdayDate (date),
                                 dntTimeDsec (dsec));
  }  //dntSdwtlDateDsec(int,int)

  //sdwtl = dntSdwtlDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateHourMinuSeco (int date,
                                                 int hour, int minu, int seco) {
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayCday (cday),
    //  hour, minu, seco);
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, dntWdayDate (date),
      hour, minu, seco);
  }  //dntSdwtlDateHourMinuSeco(int,int)

  //sdwtl = dntSdwtlDateTime (date, time)
  //  日付と時刻を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateTime (int date,
                                         int time) {
    return dntSdwtlDateWdayTime (date, dntWdayDate (date),
                                 time);
  }  //dntSdwtlDateTime(int,int)

  //sdwtl = dntSdwtlDateWday (date, wday)
  //  日付と曜日を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateWday (int date, int wday) {
    return dntSdwtlDateWdayTime (date, wday,
                                 0);
  }  //dntSdwtlDateWday(int,int)

  //sdwtl = dntSdwtlDateWdayDmil (date, wday, dmil)
  //  日付と曜日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateWdayDmil (int date, int wday,
                                             int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlDateWdayDmil(int,int,int)

  //sdwtl = dntSdwtlDateWdayDsec (date, wday, dsec)
  //  日付と曜日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateWdayDsec (int date, int wday,
                                             int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlDateWdayDsec(int,int,int)

  //sdwtl = dntSdwtlDateWdayHourMinuSeco (date, wday, hour, minu, seco)
  //  日付と曜日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateWdayHourMinuSeco (int date, int wday,
                                                     int hour, int minu, int seco) {
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  hour, minu, seco);
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, wday,
      hour, minu, seco);
  }  //dntSdwtlDateWdayHourMinuSeco(int,int,int,int,int)

  //sdwtl = dntSdwtlDateWdayTime (date, wday, time)
  //  日付と曜日と時刻を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDateWdayTime (int date, int wday,
                                             int time) {
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlDateWdayTime(int,int,int)

  //sdwtl = dntSdwtlDttm (dttm)
  //  日時を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDttm (long dttm) {
    //int date = dntDateDttm (dttm);
    int date = (int) (dttm >> 32);
    //int time = dntTimeDttm (dttm);
    int time = (int) dttm;
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayDate (date),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, dntWdayDate (date),
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlDttm(long)

  //sdwtl = dntSdwtlDttmWday (dttm, wday)
  //  日時と曜日を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlDttmWday (long dttm, int wday) {
    //int date = dntDateDttm (dttm);
    int date = (int) (dttm >> 32);
    //int time = dntTimeDttm (dttm);
    int time = (int) dttm;
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlDttmWday(long,int)

  //sdwtl = dntSdwtlNow ()
  //  現在の日時文字列(year-mo-md (Www) ho:mi:se)を返す
  public static String dntSdwtlNow () {
    return dntSdwtlCmil (dntCmilNow ());
  }  //dntSdwtlNow()

  //sdwtl = dntSdwtlYearMontMday (year, mont, mday)
  //  西暦年と月と月通日を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMday (int year, int mont, int mday) {
    return dntSdwtlYearMontMdayWdayHourMinuSeco (year, mont, mday, dntWdayYearMontMday (year, mont, mday),
                                                 0, 0, 0);
  }  //dntSdwtlYearMontMday(int,int,int)

  //sdwtl = dntSdwtlYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayDmil (int year, int mont, int mday,
                                                 int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, dntWdayYearMontMday (year, mont, mday),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlYearMontMdayDmil(int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayDsec (int year, int mont, int mday,
                                                 int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, dntWdayYearMontMday (year, mont, mday),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlYearMontMdayDsec(int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                         int hour, int minu, int seco) {
    return dntSdwtlYearMontMdayWdayHourMinuSeco (year, mont, mday, dntWdayYearMontMday (year, mont, mday),
                                                 hour, minu, seco);
  }  //dntSdwtlYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayTime (int year, int mont, int mday,
                                                 int time) {
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, dntWdayYearMontMday (year, mont, mday),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlYearMontMdayTime(int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayWday (year, mont, mday, wday)
  //  西暦年と月と月通日と曜日を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayWday (int year, int mont, int mday, int wday) {
    return dntSdwtlYearMontMdayWdayHourMinuSeco (year, mont, mday, wday,
                                                 0, 0, 0);
  }  //dntSdwtlYearMontMdayWday(int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayWdayDmil (year, mont, mday, wday, dmil)
  //  西暦年と月と月通日と曜日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayWdayDmil (int year, int mont, int mday, int wday,
                                                     int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      year, mont, mday, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlYearMontMdayWdayDmil(int,int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayWdayDsec (year, mont, mday, wday, dsec)
  //  西暦年と月と月通日と曜日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayWdayDsec (int year, int mont, int mday, int wday,
                                                     int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      year, mont, mday, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlYearMontMdayWdayDsec(int,int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayWdayHourMinuSeco (year, mont, mday, wday, hour, minu, seco)
  //  西暦年と月と月通日と曜日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayWdayHourMinuSeco (int year, int mont, int mday, int wday,
                                                             int hour, int minu, int seco) {
    return String.format ("%04d-%02d-%02d (%s) %02d:%02d:%02d",
                          year, mont, mday, DNT_ENWDAY3_WDAY[wday],
                          hour, minu, seco);
  }  //dntSdwtlYearMontMdayWdayHourMinuSeco(int,int,int,int,int,int,int)

  //sdwtl = dntSdwtlYearMontMdayWdayTime (year, mont, mday, wday, time)
  //  西暦年と月と月通日と曜日と時刻を日時文字列(year-mo-md (Www) ho:mi:se)に変換する
  public static String dntSdwtlYearMontMdayWdayTime (int year, int mont, int mday, int wday,
                                                     int time) {
    //return dntSdwtlYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtlYearMontMdayWdayHourMinuSeco (
      year, mont, mday, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtlYearMontMdayWdayTime(int,int,int,int,int)


  //--------------------------------------------------------------------------------
  //sdwtm 日時文字列(year-mo-md (Www) ho:mi:se.mil)

  //sdwtm = dntSdwtmCday (cday)
  //  暦通日を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCday (int cday) {
    return dntSdwtmDttmWday (dntDttmCday (cday), dntWdayCday (cday));
  }  //dntSdwtmCday(int)

  //sdwtm = dntSdwtmCdayDmil (cday, dmil)
  //  暦通日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCdayDmil (int cday, int dmil) {
    return dntSdwtmDateWdayTime (dntDateCday (cday), dntWdayCday (cday),
                                 dntTimeDmil (dmil));
  }  //dntSdwtmCdayDmil(int,int)

  //sdwtm = dntSdwtmCdayDsec (cday, dsec)
  //  暦通日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCdayDsec (int cday,
                                         int dsec) {
    return dntSdwtmDateWdayTime (dntDateCday (cday), dntWdayCday (cday),
                                 dntTimeDsec (dsec));
  }  //dntSdwtmCdayDsec(int,int)

  //sdwtm = dntSdwtmCdayHourMinuSeco (cday, hour, minu, seco)
  //  暦通日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCdayHourMinuSeco (int cday,
                                                 int hour, int minu, int seco) {
    int date = dntDateCday (cday);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayCday (cday),
    //  hour, minu, seco, 0);
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, dntWdayCday (cday),
      hour, minu, seco, 0);
  }  //dntSdwtmCdayHourMinuSeco(int,int,int,int)

  //sdwtm = dntSdwtmCdayHourMinuSecoMill (cday, hour, minu, seco, mill)
  //  暦通日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCdayHourMinuSecoMill (int cday,
                                                     int hour, int minu, int seco, int mill) {
    int date = dntDateCday (cday);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayCday (cday),
    //  hour, minu, seco, mill);
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, dntWdayCday (cday),
      hour, minu, seco, mill);
  }  //dntSdwtmCdayHourMinuSecoMill(int,int,int,int,int)

  //sdwtm = dntSdwtmCdayTime (cday, time)
  //  暦通日と時刻を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCdayTime (int cday,
                                         int time) {
    return dntSdwtmDateWdayTime (dntDateCday (cday), dntWdayCday (cday),
                                 time);
  }  //dntSdwtmCdayTime(int,int)

  //sdwtm = dntSdwtmCmil (cmil)
  //  暦通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCmil (long cmil) {
    return dntSdwtmDttmWday (dntDttmCmil (cmil), dntWdayCmil (cmil));
  }  //dntSdwtmCmil(long)

  //sdwtm = dntSdwtmCsec (csec)
  //  暦通秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmCsec (long csec) {
    return dntSdwtmDttmWday (dntDttmCsec (csec), dntWdayCsec (csec));
  }  //dntSdwtmCsec(long)

  //sdwtm = dntSdwtmDate (date)
  //  日付を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDate (int date) {
    return dntSdwtmDateWdayTime (date, dntWdayDate (date),
                                 0);
  }  //dntSdwtmDate(int)

  //sdwtm = dntSdwtmDateDmil (date, dmil)
  //  日付と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateDmil (int date, int dmil) {
    return dntSdwtmDateWdayTime (date, dntWdayDate (date),
                                 dntTimeDmil (dmil));
  }  //dntSdwtmDateDmil(int,int)

  //sdwtm = dntSdwtmDateDsec (date, dsec)
  //  日付と日通秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateDsec (int date, int dsec) {
    return dntSdwtmDateWdayTime (date, dntWdayDate (date),
                                 dntTimeDsec (dsec));
  }  //dntSdwtmDateDsec(int,int)

  //sdwtm = dntSdwtmDateHourMinuSeco (date, hour, minu, seco)
  //  日付と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateHourMinuSeco (int date,
                                                 int hour, int minu, int seco) {
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayCday (cday),
    //  hour, minu, seco, 0);
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, dntWdayDate (date),
      hour, minu, seco, 0);
  }  //dntSdwtmDateHourMinuSeco(int,int)

  //sdwtm = dntSdwtmDateHourMinuSecoMill (date, hour, minu, seco, mill)
  //  日付と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateHourMinuSecoMill (int date,
                                                     int hour, int minu, int seco, int mill) {
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayCday (cday),
    //  hour, minu, seco, mill);
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, dntWdayDate (date),
      hour, minu, seco, mill);
  }  //dntSdwtmDateHourMinuSecoMill(int,int,int,int,int)

  //sdwtm = dntSdwtmDateTime (date, time)
  //  日付と時刻を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateTime (int date,
                                         int time) {
    return dntSdwtmDateWdayTime (date, dntWdayDate (date),
                                 time);
  }  //dntSdwtmDateTime(int,int)

  //sdwtm = dntSdwtmDateWday (date, wday)
  //  日付と曜日を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateWday (int date, int wday) {
    return dntSdwtmDateWdayTime (date, wday,
                                 0);
  }  //dntSdwtmDateWday(int,int)

  //sdwtm = dntSdwtmDateWdayDmil (date, wday, dmil)
  //  日付と曜日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateWdayDmil (int date, int wday,
                                             int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmDateWdayDmil(int,int,int)

  //sdwtm = dntSdwtmDateWdayDsec (date, wday, dsec)
  //  日付と曜日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateWdayDsec (int date, int wday,
                                             int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmDateWdayDsec(int,int,int)

  //sdwtm = dntSdwtmDateWdayHourMinuSeco (date, wday, hour, minu, seco)
  //  日付と曜日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateWdayHourMinuSeco (int date, int wday,
                                                     int hour, int minu, int seco) {
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  hour, minu, seco, 0);
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, wday,
      hour, minu, seco, 0);
  }  //dntSdwtmDateWdayHourMinuSeco(int,int,int,int,int)

  //sdwtm = dntSdwtmDateWdayHourMinuSecoMill (date, wday, hour, minu, seco, mill)
  //  日付と曜日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateWdayHourMinuSecoMill (int date, int wday,
                                                         int hour, int minu, int seco, int mill) {
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  hour, minu, seco, mill);
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, wday,
      hour, minu, seco, mill);
  }  //dntSdwtmDateWdayHourMinuSecoMill(int,int,int,int,int,int)

  //sdwtm = dntSdwtmDateWdayTime (date, wday, time)
  //  日付と曜日と時刻を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDateWdayTime (int date, int wday,
                                             int time) {
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmDateWdayTime(int,int,int)

  //sdwtm = dntSdwtmDttm (dttm)
  //  日時を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDttm (long dttm) {
    //int date = dntDateDttm (dttm);
    int date = (int) (dttm >> 32);
    //int time = dntTimeDttm (dttm);
    int time = (int) dttm;
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), dntWdayDate (date),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, dntWdayDate (date),
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmDttm(long)

  //sdwtm = dntSdwtmDttmWday (dttm, wday)
  //  日時と曜日を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmDttmWday (long dttm, int wday) {
    //int date = dntDateDttm (dttm);
    int date = (int) (dttm >> 32);
    //int time = dntTimeDttm (dttm);
    int time = (int) dttm;
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  dntYearDate (date), dntMontDate (date), dntMdayDate (date), wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      date >> 10, date >> 6 & 15, date & 63, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmDttmWday(long,int)

  //sdwtm = dntSdwtmNow ()
  //  現在の日時文字列(year-mo-md (Www) ho:mi:se.mil)を返す
  public static String dntSdwtmNow () {
    return dntSdwtmCmil (dntCmilNow ());
  }  //dntSdwtmNow()

  //sdwtm = dntSdwtmYearMontMday (year, mont, mday)
  //  西暦年と月と月通日を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMday (int year, int mont, int mday) {
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      0, 0, 0, 0);
  }  //dntSdwtmYearMontMday(int,int,int)

  //sdwtm = dntSdwtmYearMontMdayDmil (year, mont, mday, dmil)
  //  西暦年と月と月通日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayDmil (int year, int mont, int mday,
                                                 int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  year, mont, mday, dntWdayYearMontMday (year, mont, mday),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmYearMontMdayDmil(int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayDsec (year, mont, mday, dsec)
  //  西暦年と月と月通日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayDsec (int year, int mont, int mday,
                                                 int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  year, mont, mday, dntWdayYearMontMday (year, mont, mday),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmYearMontMdayDsec(int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayHourMinuSeco (year, mont, mday, hour, minu, seco)
  //  西暦年と月と月通日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayHourMinuSeco (int year, int mont, int mday,
                                                         int hour, int minu, int seco) {
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      hour, minu, seco, 0);
  }  //dntSdwtmYearMontMdayHourMinuSeco(int,int,int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayHourMinuSecoMill (year, mont, mday, hour, minu, seco, mill)
  //  西暦年と月と月通日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayHourMinuSecoMill (int year, int mont, int mday,
                                                             int hour, int minu, int seco, int mill) {
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      hour, minu, seco, mill);
  }  //dntSdwtmYearMontMdayHourMinuSeco(int,int,int,int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayTime (year, mont, mday, time)
  //  西暦年と月と月通日と時刻を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayTime (int year, int mont, int mday,
                                                 int time) {
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  year, mont, mday, dntWdayYearMontMday (year, mont, mday),
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, dntWdayYearMontMday (year, mont, mday),
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmYearMontMdayTime(int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayWday (year, mont, mday, wday)
  //  西暦年と月と月通日と曜日を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayWday (int year, int mont, int mday, int wday) {
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, wday,
      0, 0, 0, 0);
  }  //dntSdwtmYearMontMdayWday(int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayWdayDmil (year, mont, mday, wday, dmil)
  //  西暦年と月と月通日と曜日と日通ミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayWdayDmil (int year, int mont, int mday, int wday,
                                                     int dmil) {
    int time = dntTimeDmil (dmil);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  year, mont, mday, wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmYearMontMdayWdayDmil(int,int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayWdayDsec (year, mont, mday, wday, dsec)
  //  西暦年と月と月通日と曜日と日通秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayWdayDsec (int year, int mont, int mday, int wday,
                                                     int dsec) {
    int time = dntTimeDsec (dsec);
    //return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
    //  year, mont, mday, wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntSdwtmYearMontMdayWdayDsec(int,int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayWdayHourMinuSeco (year, mont, mday, wday, hour, minu, seco)
  //  西暦年と月と月通日と曜日と時と分と秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayWdayHourMinuSeco (int year, int mont, int mday, int wday,
                                                             int hour, int minu, int seco) {
    return dntSdwtmYearMontMdayWdayHourMinuSecoMill (
      year, mont, mday, wday,
      hour, minu, seco, 0);
  }  //dntSdwtmYearMontMdayWdayHourMinuSeco(int,int,int,int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayWdayHourMinuSecoMill (year, mont, mday, wday, hour, minu, seco, mill)
  //  西暦年と月と月通日と曜日と時と分と秒とミリ秒を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayWdayHourMinuSecoMill (int year, int mont, int mday, int wday,
                                                                 int hour, int minu, int seco, int mill) {
    return String.format ("%04d-%02d-%02d (%s) %02d:%02d:%02d.%03d",
                          year, mont, mday, DNT_ENWDAY3_WDAY[wday],
                          hour, minu, seco, mill);
  }  //dntSdwtmYearMontMdayWdayHourMinuSecoMill(int,int,int,int,int,int,int,int)

  //sdwtm = dntSdwtmYearMontMdayWdayTime (year, mont, mday, wday, time)
  //  西暦年と月と月通日と曜日と時刻を日時文字列(year-mo-md (Www) ho:mi:se.mil)に変換する
  public static String dntSdwtmYearMontMdayWdayTime (int year, int mont, int mday, int wday,
                                                     int time) {
    //return dntSdwtmYearMontMdayWdayHourMinuSeco (
    //  year, mont, mday, wday,
    //  dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntSdwtmYearMontMdayWdayHourMinuSeco (
      year, mont, mday, wday,
      time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntSdwtmYearMontMdayWdayTime(int,int,int,int,int)


  //--------------------------------------------------------------------------------
  //seco 秒

  //seco = dntSecoCmil (cmil)
  //  歴通ミリ秒から秒を求める
  public static int dntSecoCmil (long cmil) {
    //return (int) dntFrem (cmil, 60000L) / 1000;
    //perl optdiv.pl 59999 1000
    //  x/1000==x*67109>>>26 (0<=x<=493998) [59999*67109==4026472891]
    return (int) dntFrem (cmil, 60000L) * 67109 >>> 26;
  }  //dntSecoCmil(long)

  //seco = dntSecoCsec (csec)
  //  歴通秒から秒を求める
  public static int dntSecoCsec (long csec) {
    return (int) dntFrem (csec, 60L);
  }  //dntSecoCsec(long)

  //seco = dntSecoDmil (dmil)
  //  日通ミリ秒から秒を求める
  public static int dntSecoDmil (int dmil) {
    //return dntFrem (dmil, 60000) / 1000;
    //perl optdiv.pl 59999 1000
    //  x/1000==x*67109>>>26 (0<=x<=493998) [59999*67109==4026472891]
    return dntFrem (dmil, 60000) * 67109 >>> 26;
  }  //dntSecoDmil(int)

  //seco = dntSecoDsec (dsec)
  //  日通秒から秒を求める
  public static int dntSecoDsec (int dsec) {
    return dntFrem (dsec, 60);
  }  //dntSecoDsec(int)

  //seco = dntSecoDttm (dttm)
  //  日時から秒を取り出す
  public static int dntSecoDttm (long dttm) {
    return (int) dttm >> 10 & 63;
  }  //dntSecoDttm(long)

  //seco = dntSecoNow ()
  //  現在の秒を返す
  public static int dntSecoNow () {
    //return dntSecoCmil (dntCmilNow ());
    //return (int) dntFrem (System.currentTimeMillis (), 60000L) / 1000;
    //perl optdiv.pl 59999 1000
    //  x/1000==x*67109>>>26 (0<=x<=493998) [59999*67109==4026472891]
    return (int) dntFrem (System.currentTimeMillis (), 60000L) * 67109 >>> 26;
  }  //dntSecoNow()

  //seco = dntSecoTime (time)
  //  時刻から秒を取り出す
  public static int dntSecoTime (int time) {
    return time >> 10 & 63;
  }  //dntSecoTime(int)


  //--------------------------------------------------------------------------------
  //stl 時刻文字列(ho:mi:se)

  //stl = dntStlCmil (cmil)
  //  歴通ミリ秒を時刻文字列(ho:mi:se)に変換する
  public static String dntStlCmil (int cmil) {
    return dntStlTime (dntTimeCmil (cmil));
  }  //dntStlCmil(int)

  //stl = dntStlCsec (csec)
  //  歴通秒を時刻文字列(ho:mi:se)に変換する
  public static String dntStlCsec (int csec) {
    return dntStlTime (dntTimeCsec (csec));
  }  //dntStlCsec(int)

  //stl = dntStlDmil (dmil)
  //  日通秒を時刻文字列(ho:mi:se)に変換する
  public static String dntStlDmil (int dmil) {
    return dntStlTime (dntTimeDmil (dmil));
  }  //dntStlDmil(int)

  //stl = dntStlDsec (dsec)
  //  日通秒を時刻文字列(ho:mi:se)に変換する
  public static String dntStlDsec (int dsec) {
    return dntStlTime (dntTimeDsec (dsec));
  }  //dntStlDsec(int)

  //stl = dntStlDttm (dttm)
  //  日時を時刻文字列(ho:mi:se)に変換する
  public static String dntStlDttm (long dttm) {
    //return dntStlTime (dntTimeDttm (dttm));
    return dntStlTime ((int) dttm);
  }  //dntStlDttm(long)

  //stl = dntStlHourMinuSeco (hour, minu, seco)
  //  時と分と秒を時刻文字列(ho:mi:se)に変換する
  public static String dntStlHourMinuSeco (int hour, int minu, int seco) {
    return String.format ("%02d:%02d:%02d", hour, minu, seco);
  }  //dntStlHourMinuSeco(int,int,int)

  //stl = dntStlNow ()
  //  現在の時刻文字列(ho:mi:se)を返す
  public static String dntStlNow () {
    return dntStlTime (dntTimeNow ());
  }  //dntStlNow()

  //stl = dntStlTime (time)
  //  時刻を時刻文字列(ho:mi:se)に変換する
  public static String dntStlTime (int time) {
    //return dntStlHourMinuSeco (dntHourTime (time), dntMinuTime (time), dntSecoTime (time));
    return dntStlHourMinuSeco (time >> 22, time >> 16 & 63, time >> 10 & 63);
  }  //dntStlTime(int)


  //--------------------------------------------------------------------------------
  //stm 時刻文字列(ho:mi:se.mil)

  //stm = dntStmCmil (cmil)
  //  歴通ミリ秒を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmCmil (int cmil) {
    return dntStmTime (dntTimeCmil (cmil));
  }  //dntStmCmil(int)

  //stm = dntStmCsec (csec)
  //  歴通秒を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmCsec (int csec) {
    return dntStmTime (dntTimeCsec (csec));
  }  //dntStmCsec(int)

  //stm = dntStmDmil (dmil)
  //  日通秒を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmDmil (int dmil) {
    return dntStmTime (dntTimeDmil (dmil));
  }  //dntStmDmil(int)

  //stm = dntStmDsec (dsec)
  //  日通秒を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmDsec (int dsec) {
    return dntStmTime (dntTimeDsec (dsec));
  }  //dntStmDsec(int)

  //stm = dntStmDttm (dttm)
  //  日時を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmDttm (long dttm) {
    //return dntStmTime (dntTimeDttm (dttm));
    return dntStmTime ((int) dttm);
  }  //dntStmDttm(long)

  //stm = dntStmHourMinuSeco (hour, minu, seco)
  //  時と分と秒を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmHourMinuSeco (int hour, int minu, int seco) {
    return dntStmHourMinuSecoMill (hour, minu, seco, 0);
  }  //dntStmHourMinuSeco(int,int,int)

  //stm = dntStmHourMinuSecoMill (hour, minu, seco, mill)
  //  時と分と秒とミリ秒を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmHourMinuSecoMill (int hour, int minu, int seco, int mill) {
    return String.format ("%02d:%02d:%02d.%03d", hour, minu, seco, mill);
  }  //dntStmHourMinuSecoMill(int,int,int,int)

  //stm = dntStmNow ()
  //  現在の時刻文字列(ho:mi:se)を返す
  public static String dntStmNow () {
    return dntStmTime (dntTimeNow ());
  }  //dntStmNow()

  //stm = dntStmTime (time)
  //  時刻を時刻文字列(ho:mi:se.mil)に変換する
  public static String dntStmTime (int time) {
    //return dntStmHourMinuSecoMill (dntHourTime (time), dntMinuTime (time), dntSecoTime (time), dntMillTime (time));
    return dntStmHourMinuSecoMill (time >> 22, time >> 16 & 63, time >> 10 & 63, time & 1023);
  }  //dntStmTime(int)


  //--------------------------------------------------------------------------------
  //time 時

  //time = dntTimeCmil (cmil)
  //  歴通ミリ秒を時刻に変換する
  public static int dntTimeCmil (long cmil) {
    //return dntTimeDmil (dntDmilCmil (cmil));
    return dntTimeDmil ((int) dntFrem (cmil, 86400000L));
  }  //dntTimeCmil(long)

  //time = dntTimeCsec (csec)
  //  歴通秒を時刻に変換する
  public static int dntTimeCsec (long csec) {
    //return dntTimeDsec (dntDsecCsec (csec));
    return dntTimeDsec ((int) dntFrem (csec, 86400L));
  }  //dntTimeCsec(long)

  //time = dntTimeDmil (dmil)
  //  日通ミリ秒を時刻に変換する
  public static int dntTimeDmil (int dmil) {
    int hour = dntFdiv (dmil, 3600000);
    dmil -= hour * 3600000;
    int minu = dntFdiv (dmil, 60000);
    dmil -= minu * 60000;
    int seco = dntFdiv (dmil, 1000);
    dmil -= seco * 1000;
    return dntTimeHourMinuSecoMill (hour, minu, seco, dmil);
  }  //dntTimeDmil(int)

  //time = dntTimeDsec (dsec)
  //  日通秒を時刻に変換する
  public static int dntTimeDsec (int dsec) {
    int hour = dntFdiv (dsec, 3600);
    dsec -= hour * 3600;
    int minu = dntFdiv (dsec, 60);
    dsec -= minu * 60;
    return dntTimeHourMinuSeco (hour, minu, dsec);
  }  //dntTimeDsec(int)

  //time = dntTimeDttm (dttm)
  //  日時から時刻を取り出す
  public static int dntTimeDttm (long dttm) {
    return (int) dttm;
  }  //dntTimeDttm(long)

  //time = dntTimeHourMinuSeco (hour, minu, seco)
  //  時と分と秒から時刻を作る
  public static int dntTimeHourMinuSeco (int hour, int minu, int seco) {
    return hour << 22 | (minu & 63) << 16 | (seco & 63) << 10;
  }  //dntTimeHourMinuSeco(int,int,int)

  //time = dntTimeHourMinuSecoMill (hour, minu, seco, mill)
  //  時と分と秒とミリ秒から時刻を作る
  public static int dntTimeHourMinuSecoMill (int hour, int minu, int seco, int mill) {
    return hour << 22 | (minu & 63) << 16 | (seco & 63) << 10 | mill & 1023;
  }  //dntTimeHourMinuSecoMill(int,int,int,int)

  //time = dntTimeNow ()
  //  現在の時刻を返す
  public static int dntTimeNow () {
    //return dntTimeDmil (dntDmilCmil (dntCmilNow ()));
    return dntTimeDmil ((int) dntFrem (System.currentTimeMillis (), 86400000L));
  }  //dntTimeNow()


  //--------------------------------------------------------------------------------
  //wday 曜日

  //wday = dntWdayCday (cday)
  //  暦通日から曜日を求める
  public static int dntWdayCday (int cday) {
    return dntFrem (cday + 4, 7);
  }  //dntWdayCday(int)

  //wday = dntWdayCmil (cmil)
  //  暦通ミリ秒から曜日を求める
  public static int dntWdayCmil (long cmil) {
    //return dntWdayCday (dntCdayCmil (cmil));
    return dntFrem ((int) dntFdiv (cmil, 86400000L) + 4, 7);
  }  //dntWdayCmil(long)

  //wday = dntWdayCsec (csec)
  //  暦通秒から曜日を求める
  public static int dntWdayCsec (long csec) {
    //return dntWdayCday (dntCdayCsec (csec));
    return dntFrem ((int) dntFdiv (csec, 86400L) + 4, 7);
  }  //dntWdayCsec(long)

  //wday = dntWdayDate (date)
  //  日付から曜日を求める
  public static int dntWdayDate (int date) {
    //return dntWdayCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return dntFrem (dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) + 4, 7);
  }  //dntWdayDate(int)

  //wday = dntWdayDttm (dttm)
  //  日時から曜日を求める
  public static int dntWdayDttm (long dttm) {
    //return dntWdayDate (dntDateDttm (dttm));
    int date = (int) (dttm >> 32);
    //return dntWdayCday (dntCdayYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date)));
    return dntFrem (dntCdayYearMontMday (date >> 10, date >> 6 & 15, date & 63) + 4, 7);
  }  //dntWdayDttm(long)

  //wday = dntWdayNow ()
  //  現在の曜日を返す
  public static int dntWdayNow () {
    //return dntWdayCday (dntCdayCmil (dntCmilNow ()));
    return dntFrem ((int) dntFdiv (System.currentTimeMillis (), 86400000L) + 4, 7);
  }  //dntWdayNow()

  //wday = dntWdayYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から曜日を求める
  public static int dntWdayYearMontMday (int year, int mont, int mday) {
    return dntFrem (dntCdayYearMontMday (year, mont, mday) + 4, 7);
  }  //dntWdayYearMontMday(int,int,int)


  //--------------------------------------------------------------------------------
  //year 西暦年

  //year = dntYearCday (cday)
  //  歴通日から西暦年を求める
  public static int dntYearCday (int cday) {
    //return dntYearDate (dntDateCday (cday));
    return dntDateCday (cday) >> 10;
  }  //dntYearCday(int)

  //year = dntYearCmil (cmil)
  //  歴通ミリ秒から西暦年を求める
  public static int dntYearCmil (long cmil) {
    //return dntYearDate (dntDateCday (dntCdayCmil (cmil)));
    return dntDateCday ((int) dntFdiv (cmil, 86400000L)) >> 10;
  }  //dntYearCmil(long)

  //year = dntYearCsec (csec)
  //  歴通秒から西暦年を求める
  public static int dntYearCsec (long csec) {
    //return dntYearDate (dntDateCday (dntCdayCsec (csec)));
    return dntDateCday ((int) dntFdiv (csec, 86400L)) >> 10;
  }  //dntYearCsec(long)

  //year = dntYearDate (date)
  //  日付から西暦年を取り出す
  public static int dntYearDate (int date) {
    return date >> 10;
  }  //dntYearDate(int)

  //year = dntYearDttm (dttm)
  //  日時から西暦年を取り出す
  public static int dntYearDttm (long dttm) {
    return (int) (dttm >> 42);
  }  //dntYearDttm(long)

  //year = dntYearNow ()
  //  現在の西暦年を返す
  public static int dntYearNow () {
    //return dntYearDate (dntDateCday (dntCdayCmil (dntCmilNow ())));
    return dntDateCday ((int) dntFdiv (System.currentTimeMillis (), 86400000L)) >> 10;
  }  //dntYearNow()


  //--------------------------------------------------------------------------------
  //jaholi 日本の祝日の名前(日本語)
  //
  //    国民の祝日  国民の祝日に関する法律で定められた日
  //    国民の休日  前日が国民の祝日で翌日も国民の祝日である国民の祝日ではない日
  //    振替休日    直前の日曜日から前日まで国民の祝日が続いている国民の祝日ではない日
  //
  //    1月1日            1949年～        元日 (New Year's Day)
  //    1月2日(月)        1974年～        振替休日 (transfer holiday) (元日が日曜日のとき)
  //    1月第2月曜日      2000年～        成人の日 (Coming of Age Day)
  //    1月15日           1949年～1999年  成人の日 (Coming of Age Day)
  //    1月16日(月)       1974年～1999年  振替休日 (transfer holiday) (成人の日が日曜日のとき)
  //    2月11日           1967年～        建国記念の日 (National Foundation Day)
  //    2月12日(月)       1974年～        振替休日 (transfer holiday) (建国記念の日が日曜日のとき)
  //    3月21日頃         1949年～        春分の日 (Vernal Equinox Day)
  //    3月22日頃(月)     1974年～        振替休日 (transfer holiday) (春分の日が日曜日のとき)
  //    4月29日           1949年～1988年  天皇誕生日 (The Emperor's Birthday)
  //                      1989年～2006年  みどりの日 (Greenery Day)
  //                      2007年～        昭和の日 (Showa Day)
  //    4月30日(月)       1973年～        振替休日 (transfer holiday) (天皇誕生日,みどりの日,昭和の日が日曜日のとき)
  //    5月3日            1949年～        憲法記念日 (Constitution Memorial Day)
  //    5月4日(月)        1973年～1987年  振替休日 (transfer holiday) (憲法記念日が日曜日のとき)
  //    5月4日            1988年～2006年  国民の休日 (national day of rest) (憲法記念日とこどもの日に挟まれた平日)
  //                      2007年～        みどりの日 (Greenery Day)
  //    5月5日            1949年～        こどもの日 (Children's Day)
  //    5月6日(月)        1973年～2006年  振替休日 (transfer holiday) (こどもの日が日曜日のとき)
  //    5月6日(月,火,水)  2007年～        振替休日 (transfer holiday) (憲法記念日,みどりの日,こどもの日が日曜日のとき)
  //    7月第3月曜日      2003年～        海の日 (Marine Day)
  //    7月20日           1996年～2002年  海の日 (Marine Day)
  //    8月11日           2016年～        山の日 (Mountain Day)
  //    7月21日(月)       1996年～2002年  振替休日 (transfer holiday) (海の日が日曜日のとき)
  //    9月15日           1966年～2002年  敬老の日 (Respect for the Aged Day)
  //    9月第3月曜日      2003年～        敬老の日 (Respect for the Aged Day)
  //    9月16日(月)       1973年～2002年  振替休日 (transfer holiday) (敬老の日が日曜日のとき)
  //    9月22日頃(火)     2003年～        国民の休日 (national day of rest) (敬老の日と秋分の日に挟まれた平日)
  //    9月23日頃         1948年～        秋分の日 (Autumnal Equinox Day)
  //    9月24日頃(月)     1973年～        振替休日 (transfer holiday) (秋分の日が日曜日のとき)
  //    10月10日          1966年～1999年  体育の日 (Health and Sports Day)
  //    10月第2月曜日     2000年～        体育の日 (Health and Sports Day)
  //    10月11日(月)      1973年～1999年  振替休日 (transfer holiday) (体育の日が日曜日のとき)
  //    11月3日           1948年～        文化の日 (Culture Day)
  //    11月4日(月)       1973年～        振替休日 (transfer holiday) (文化の日が日曜日のとき)
  //    11月23日          1948年～        勤労感謝の日 (Labor Thanksgiving Day)
  //    11月24日(月)      1973年～        振替休日 (transfer holiday) (勤労感謝の日が日曜日のとき)
  //    12月23日          1989年～        天皇誕生日 (The Emperor's Birthday)
  //    12月24日(月)      1989年～        振替休日 (transfer holiday) (天皇誕生日が日曜日のとき)
  //                      1959年4月10日   皇太子明仁親王の結婚の儀 (The Rite of Wedding of HIH Crown Prince Akihito)
  //                      1989年2月24日   昭和天皇の大喪の礼 (The Funeral Ceremony of Emperor Showa.)
  //                      1990年11月12日  即位礼正殿の儀 (The Ceremony of the Enthronement of His Majesty the Emperor (at the Seiden))
  //                      1993年6月9日    皇太子徳仁親王の結婚の儀 (The Rite of Wedding of HIH Crown Prince Naruhito)
  //                                      (HIH: His/Her Imperial Highness; 殿下/妃殿下)
  //    参考
  //      http://www8.cao.go.jp/chosei/shukujitsu/gaiyou.html
  //      http://eco.mtk.nao.ac.jp/koyomi/yoko/
  //      http://www.nao.ac.jp/faq/a0301.html
  //      https://ja.wikipedia.org/wiki/%E5%9B%BD%E6%B0%91%E3%81%AE%E7%A5%9D%E6%97%A5
  //      https://en.wikipedia.org/wiki/Public_holidays_in_Japan

  //jaholi = jaholiDttm (dttm)
  //  日時から日本の祝日の名前を求める
  public static String dntJaholiDttm (long dttm) {
    //return dntJaholiDate (dntDateDttm (dttm));
    int date = (int) (dttm >> 32);
    //return dntJaholiYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntJaholiYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntJaholiDttm(long)

  //jaholi = dntJaholiDate (date)
  //  日付から日本の祝日の名前(日本語)を求める
  public static String dntJaholidate (int date) {
    //return dntJaholiYearMontMday (dntYearDate (date), dntMontDate (date), dntMdayDate (date));
    return dntJaholiYearMontMday (date >> 10, date >> 6 & 15, date & 63);
  }  //dntJaholiDate(int)

  //jaholi = dntJaholiYearMontMday (year, mont, mday)
  //  西暦年と月と月通日から日本の祝日の名前(日本語)を求める
  public static String dntJaholiYearMontMday (int year, int mont, int mday) {
    //int wday = dntWdayYearMontMday (year, mont, mday);
    int wday = dntWdayYearMontMday (year, mont, mday);
    int wnum = dntFdiv (mday + 6, 7);  //第何wday曜日か (1～)
    int mdayVernal = ((year & 3) == 0 ? year <= 1956 ? 21 : year <= 2088 ? 20 : 19 :
                      (year & 3) == 1 ? year <= 1989 ? 21 : 20 :
                      (year & 3) == 2 ? year <= 2022 ? 21 : 20 :
                      year <= 1923 ? 22 : year <= 2055 ? 21 : 20);  //春分の日の月通日
    int mdayAutumnal = ((year & 3) == 0 ? year <= 2008 ? 23 : 22 :
                        (year & 3) == 1 ? year <= 1917 ? 24 : year <= 2041 ? 23 : 22 :
                        (year & 3) == 2 ? year <= 1946 ? 24 : year <= 2074 ? 23 : 22 :
                        year <= 1979 ? 24 : 23);  //秋分の日の月通日
    return (mont == 1 ?  //1月
            1949 <= year && mday == 1 ? "元日" :  //1949年～ 1月1日
            1974 <= year && mday == 2 && wday == 1 ? "振替休日" :  //1974年～ 1月2日(月)
            2000 <= year && wnum == 2 && wday == 1 ? "成人の日" :  //2000年～ 1月第2月曜日
            1949 <= year && year <= 1999 && mday == 15 ? "成人の日" :  //1949年～1999年 1月15日
            1974 <= year && year <= 1999 && mday == 16 && wday == 1 ? "振替休日" : null : //1974年～1999年 1月16日(月)
            mont == 2 ?  //2月
            year == 1989 && mday == 24 ? "昭和天皇の大喪の礼" :  //1989年2月24日
            1967 <= year && mday == 11 ? "建国記念の日" :  //1967年～ 2月11日
            1974 <= year && mday == 12 && wday == 1 ? "振替休日" : null :  //1974年～ 2月12日(月)
            mont == 3 ?  //3月
            1949 <= year && mday == mdayVernal ? "春分の日" :  //1949年～ 3月21日頃
            1974 <= year && mday == mdayVernal + 1 && wday == 1 ? "振替休日" : null :  //1974年～ 3月22日頃(月)
            mont == 4 ?  //4月
            year == 1959 && mday == 10 ? "皇太子明仁親王の結婚の儀" :  //1959年4月10日
            1949 <= year && year <= 1988 && mday == 29 ? "天皇誕生日" :  //1949年～1988年 4月29日
            1989 <= year && year <= 2006 && mday == 29 ? "みどりの日" :  //1989年～2006年 4月29日
            2007 <= year && mday == 29 ? "昭和の日" :  //2007年～ 4月29日
            1973 <= year && mday == 30 && wday == 1 ? "振替休日" : null :  //1973年～ 4月30日(月)
            mont == 5 ?  //5月
            1949 <= year && mday == 3 ? "憲法記念日" :  //1949年～ 5月3日
            1973 <= year && year <= 1987 && mday == 4 && wday == 1 ? "振替休日" :  //1973年～1987年 5月4日(月)
            1988 <= year && year <= 2006 && mday == 4 ? "国民の休日" :  //1988年～2006年 5月4日
            2007 <= year && mday == 4 ? "みどりの日" :  //2007年～ 5月4日
            1949 <= year && mday == 5 ? "こどもの日" :  //1949年～ 5月5日
            1973 <= year && year <= 2006 && mday == 6 && wday == 1 ? "振替休日" :  //1973年～2006年 5月6日(月)
            2007 <= year && mday == 6 && (wday == 1 || wday == 2 || wday == 3) ? "振替休日" : null :  //2007年～ 5月6日(月,火,水)
            mont == 6 ?  //6月
            year == 1993 && mday == 9 ? "皇太子徳仁親王の結婚の儀" : null :  //1993年6月9日
            mont == 7 ?  //7月
            2003 <= year && wnum == 3 && wday == 1 ? "海の日" :  //2003年～ 7月第3月曜日
            1996 <= year && year <= 2002 && mday == 20 ? "海の日" :  //1996年～2002年 7月20日
            1996 <= year && year <= 2002 && mday == 21 && wday == 1 ? "振替休日" : null :  //1996年～2002年 7月21日(月)
            mont == 8 ?  //8月
            2016 <= year && mday == 11 ? "山の日" : null :  //2016年～ 8月11日
            mont == 9 ?  //9月
            1966 <= year && year <= 2002 && mday == 15 ? "敬老の日" :  //1966年～2002年 9月15日
            2003 <= year && wnum == 3 && wday == 1 ? "敬老の日" :  //2003年～ 9月第3月曜日
            1973 <= year && year <= 2002 && mday == 16 && wday == 1 ? "振替休日" :  //1973年～2002年 9月16日(月)
            2003 <= year && dntFdiv (mday + 5, 7) == 3 && wday == 2 && mday == mdayAutumnal - 1 ? "国民の休日" :  //2003年～ 9月22日頃(火)
            1948<=  year && mday == mdayAutumnal ? "秋分の日" :  //1948年～ 9月23日頃
            1973 <= year && mday == mdayAutumnal + 1 && wday == 1 ? "振替休日" : null :  //1973年～ 9月24日頃(月)
            mont == 10 ?  //10月
            1966 <= year && year <= 1999 && mday == 10 ? "体育の日" :  //1966年～1999年 10月10日
            2000 <= year && wnum == 2 && wday == 1 ? "体育の日" :  //2000年～ 10月第2月曜日
            1973 <= year && year <= 1999 && mday == 11 && wday == 1 ? "振替休日" : null :  //1973年～1999年 10月11日(月)
            mont == 11 ?  //11月
            year == 1990 && mday == 12 ? "即位礼正殿の儀" :  //1990年11月12日
            1948 <= year && mday == 3 ? "文化の日" :  //1948年～ 11月3日
            1973 <= year && mday == 4 && wday == 1 ? "振替休日" :  //1973年～ 11月4日(月)
            1948 <= year && mday == 23 ? "勤労感謝の日" :  //1948年～ 11月23日
            1973 <= year && mday == 24 && wday == 1 ? "振替休日" : null :  //1973年～ 11月24日(月)
            mont == 12 ?  //12月
            1989 <= year && mday == 23 ? "天皇誕生日" :  //1989年～ 12月23日
            1989 <= year && mday == 24 && wday == 1 ? "振替休日" : null :  //1989年～ 12月24日(月)
            null);
  }  //dntJaholiYearMontMday(int,int,int)


}  //class DnT



