//========================================================================================
//  SimpleTask.java
//    en:Simple task
//    ja:シンプルタスク
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.util.concurrent.*;  //ScheduledExecutorService

public class SimpleTask implements Runnable {

  //interface SimpleTask.Runner
  //  タスクの本体
  public interface Runner {
    //n = runN (n)
    //  nは初回は1、次回からは前回返した値。
    //  0を返すと終了、0以外を返すと継続。
    //  runN(n)の内側からstop()を使って停止させようとしないこと。
    //  スレッドを占有しないように適度に細切れにすること。
    public int runN (int n);
  }  //interface Runner

  //interface SimpleTask.FinalizableRunner
  //  後始末付きタスクの本体
  public interface FinalizableRunner extends Runner {
    //finalizeN (n)
    //  後始末をする
    //  nはstop()で停止したときは次にrunN(n)に渡す予定だったn、
    //  最後まで実行したときは0。
    //  最後のrunN(n)の後に1回だけfinalizeN(n)が呼び出される。
    //  シャットダウンでは呼び出されない(ことがある)ので、
    //  後始末が必要なときは必ずstop()すること。
    public void finalizeN (int n);
  }  //interface FinalizableRunner

  //状態
  public static final int STOPPED = 0;  //停止中
  public static final int ONE_SHOT = 1;  //1回実行中
  public static final int FIXED_DELAY = 2;  //固定間隔実行中
  public static final int FIXED_PERIOD = 3;  //固定周期実行中

  //引数
  //  https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html
  private ScheduledExecutorService executor;  //スレッド
  private Runner runner;  //タスクの本体

  //変数
  private volatile int status;  //状態。STOPPED、ONE_SHOT、FIXED_DELAY、FIXED_PERIODのいずれか
  private volatile int n;  //(実行中のとき)次回のrunN(n)の引数
  private volatile boolean lastRun;  //(実行中のとき)次回のrun()が最後
  private volatile long delayOrPeriodNano;  //(実行中のとき)現在の間隔または周期
  private volatile long nextDelayOrPeriodNano;  //(実行中のとき)次回の間隔または周期
  private final Object lock = new Object ();  //futureが確定するまでrun()に読ませない
  private volatile ScheduledFuture<?> future;  //(実行中のとき)scheduleが返したfuture

  //task = new SimpleTask (executor, runner)
  //  コンストラクタ
  public SimpleTask (ScheduledExecutorService executor, Runner runner) {
    this.executor = executor;
    this.runner = runner;
    status = STOPPED;  //停止中
  }  //new SimpleTask

  //task = task.fixedDelay (initialDelay, delay)
  //task = task.fixedDelay (initialDelayNano, delayNano)
  //  固定間隔実行を予約する。
  public SimpleTask fixedDelay (long initialDelay, long delay) {
    return fixedDelayNano (1000000L * initialDelay, 1000000L * delay);
  }  //fixedDelay
  public SimpleTask fixedDelayNano (long initialDelayNano, long delayNano) {
    if (status != STOPPED) {  //実行中
      throw new IllegalStateException ("task is running");
    }
    n = 1;  //初回は1
    status = FIXED_DELAY;  //固定間隔実行中
    lastRun = false;  //次回のrun()が最後ではない
    delayOrPeriodNano =
      nextDelayOrPeriodNano = delayNano;
    synchronized (lock) {
      future = executor.scheduleWithFixedDelay (
        this, initialDelayNano, delayNano, TimeUnit.NANOSECONDS);  //固定間隔実行
    }
    return this;
  }  //fixedDelayNano

  //task = task.fixedPeriod (initialDelay, period)
  //task = task.fixedPeriodNano (initialDelayNano, periodNano)
  //  固定周期実行を予約する。
  public SimpleTask fixedPeriod (long initialDelay, long period) {
    return fixedPeriodNano (1000000L * initialDelay, 1000000L * period);
  }  //fixedPeriod
  public SimpleTask fixedPeriodNano (long initialDelayNano, long periodNano) {
    if (status != STOPPED) {  //実行中
      throw new IllegalStateException ("task is running");
    }
    status = FIXED_PERIOD;  //固定周期実行中
    n = 1;  //初回は1
    lastRun = false;  //次回のrun()が最後ではない
    delayOrPeriodNano =
      nextDelayOrPeriodNano = periodNano;
    synchronized (lock) {
      future = executor.scheduleAtFixedRate (
        this, initialDelayNano, periodNano, TimeUnit.NANOSECONDS);  //固定頻度実行
    }
    return this;
  }  //fixedPeriodNano

  //task.gaze ()
  //  終了するまで待つ。
  public void gaze () {
    if (status != STOPPED) {  //実行中
      try {
        future.get ();
      } catch (Exception e) {
      }
    }
  }  //gaze

  //task.gaze (timeout)
  //task.gazeNano (timeoutNano)
  //  終了するかタイムアウトまで待つ。
  public void gaze (long timeout) {
    gazeNano (1000000L * timeout);
  }  //gaze
  public void gazeNano (long timeoutNano) {
    if (status != STOPPED) {  //実行中
      try {
        future.get (timeoutNano, TimeUnit.NANOSECONDS);
      } catch (Exception e) {
      }
    }
  }  //gazeNano

  //status = task.getStatus ()
  //  状態を得る。
  //  STOPPED、ONE_SHOT、FIXED_DELAY、FIXED_PERIODのいずれかを返す
  public int getStatus () {
    return status;
  }  //getStatus

  //task = task.oneShot (initialDelay)
  //task = task.oneShotNano (initialDelayNano)
  //  1回実行を予約する。
  public SimpleTask oneShot (long initialDelay) {
    return oneShotNano (1000000L * initialDelay);
  }  //oneShot
  public SimpleTask oneShotNano (long initialDelayNano) {
    if (status != STOPPED) {  //実行中
      throw new IllegalStateException ("task is running");
    }
    status = ONE_SHOT;  //1回実行中
    n = 1;  //初回は1
    lastRun = true;  //次回のrun()が最後
    synchronized (lock) {
      future = executor.schedule (
        this, initialDelayNano, TimeUnit.NANOSECONDS);  //1回実行
    }
    return this;
  }  //oneShotNano

  //run ()
  //  タスク
  @Override public void run () {
    n = runner.runN (n);  //タスクの本体
    if (n == 0 ||  //終了
        lastRun) {  //次回のrun()が最後
      synchronized (lock) {
        future.cancel (false);  //次回以降のrun()をキャンセルする
      }
      if (runner instanceof FinalizableRunner) {
        ((FinalizableRunner) runner).finalizeN (n);  //後始末をする
      }
      status = STOPPED;  //停止中
      return;
    }
    if (delayOrPeriodNano != nextDelayOrPeriodNano) {  //間隔または周期が変更された
      delayOrPeriodNano = nextDelayOrPeriodNano;
      if (status == FIXED_DELAY) {
        synchronized (lock) {
          future.cancel (false);
          future = executor.scheduleWithFixedDelay (
            this, delayOrPeriodNano, delayOrPeriodNano, TimeUnit.NANOSECONDS);  //固定間隔実行
        }
      } else if (status == FIXED_PERIOD) {
        synchronized (lock) {
          future.cancel (false);
          future = executor.scheduleAtFixedRate (
            this, delayOrPeriodNano, delayOrPeriodNano, TimeUnit.NANOSECONDS);  //固定頻度実行
        }
      }
    }
  }  //run

  //task.setDelay (delay)
  //task.setDelayNano (delayNano)
  //  固定間隔実行の間隔を変更する。
  public void setDelay (long delay) {
    setDelayNano (1000000L * delay);
  }  //setDelay
  public void setDelayNano (long delayNano) {
    nextDelayOrPeriodNano = delayNano;
  }  //setDelayNano

  //task.setPeriod (period)
  //task.setPeriodNano (periodNano)
  //  固定周期実行の周期を変更する。
  public void setPeriod (long period) {
    setPeriodNano (1000000L * period);
  }  //setPeriod
  public void setPeriodNano (long periodNano) {
    nextDelayOrPeriodNano = periodNano;
  }  //setPeriodNano

  //sleep (timeout)
  //sleepNano (timeoutNano)
  //  タイムアウトまで待つ。
  //  InterruptedExceptionがあるとタイムアウトを待たない。
  public static void sleep (long timeout) {
    sleepNano (1000000L * timeout);
  }  //sleep
  public static void sleepNano (long timeoutNano) {
    try {
      Thread.sleep (timeoutNano / 1000000L, (int) (timeoutNano % 1000000L));
    } catch (InterruptedException ie) {
    }
  }  //sleepNano

  //task = task.stop ()
  //  実行中のとき次回以降のrun()をキャンセルして終了するまで待つ。
  //  runN(n)の内側からstop()を使って停止させようとしないこと。
  public SimpleTask stop () {
    if (status != STOPPED) {  //実行中
      lastRun = true;  //次回のrun()が最後
      future.cancel (false);  //次回以降のrun()をキャンセルする
      try {
        future.get ();  //run()が終わるまで待つ
      } catch (Exception e) {
      }
      if (status != STOPPED) {  //実行中。最後のrun()が後始末をしなかった
        if (runner instanceof FinalizableRunner) {
          ((FinalizableRunner) runner).finalizeN (n);  //後始末をする
        }
        status = STOPPED;  //停止中
      }
    }
    return this;
  }  //stop

}  //class SimpleTask



