package com.idega.block.messenger.business;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.AWTEventMulticaster;

/**
 * Title:        com.idega.block.messenger.business
 * Description:  idega classes
 * Copyright:    Copyright (c) 2001
 * Company:      Idega Software
 * @author <a href="mailto:eiki@idega.is">Eirikur S. Hrafnsson</a>
 * @version 1.0
 */

public class MessageListener implements Runnable{
  private ActionListener listener;
  private Thread t;
  private long threadSleep = 5000;//5 seconds
  private boolean runThread = false;

  public MessageListener() {
  }

  public MessageListener(long interval) {
    setInterval(interval);
  }

  public void run(){
    while(runThread){
      try {
        if( listener!=null ) listener.actionPerformed(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"iw-cycle"));
        Thread.sleep(threadSleep);
      }
      catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
  }

  public void start(){
    runThread = true;
    if( t == null ){
      t = new Thread(this,"MessageListener thread");
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
    }
  }

  public void stop(){
    if ( t != null ){
      runThread = false;
    }
  }

   /**Destroy the thread*/
  public void destroy() {
    stop();
    t = null;
  }

  public void setInterval(long interval){
    this.threadSleep = interval;
  }

  public void addActionListener(ActionListener l) {
    listener = AWTEventMulticaster.add(listener, l);
  }

  public void removeActionListener(ActionListener l) {
    listener = AWTEventMulticaster.remove(listener, l);
  }

}
