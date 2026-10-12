//========================================================================================
//  CoreTask.java
//    en:Core task
//    ja:コアタスク
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

public class CoreTask {
  private static volatile boolean locked = false;
  private static volatile int readCount = 0;
  private static volatile int writeCount = 0;
  private static final int SIZE = 256;
  private static final Runnable[] queue = new Runnable[SIZE];
  private static final Object readKey = new Object ();
  private static final Object writeKey = new Object ();
  public static void lock () {
    locked = true;
  }  //lock
  public static void unlock () {
    locked = false;
    if (readCount != writeCount) {
      synchronized (readKey) {
        do {
          queue[(readCount++) & (SIZE - 1)].run ();
        } while (readCount != writeCount);
      }
    }
  }  //unlock
  public static void add (Runnable task) {
    synchronized (writeKey) {
      queue[(writeCount++) & (SIZE - 1)] = task;
    }
    if (!locked) {
      synchronized (readKey) {
        do {
          queue[(readCount++) & (SIZE - 1)].run ();
        } while (readCount != writeCount);
      }
    }
  }  //add
}  //class CoreTask
