package com.idega.block.messenger.presentation;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import java.io.*;
import java.net.*;

import com.idega.block.messenger.data.Message;
import com.idega.block.messenger.data.Packet;
import com.idega.block.messenger.data.Property;
import com.idega.block.messenger.business.MessageListener;
import com.idega.presentation.awt.ImageLabel;
import com.idega.presentation.awt.SingleLineItem;

/**
 * Title:        MessengerApplet
 * Description:  Simple client sceleton
 * Copyright:    Copyright (c) 2001
 * Company:      Idega Software
 * @author <a href="mailto:eiki@idega.is">Eirikur S. Hrafnsson</a>
 * @version 1.0
 */

public class MessengerApplet extends Applet implements Runnable, ActionListener{
//public class MessengerApplet extends Applet implements ActionListener{
  private boolean runThread = false;
  private boolean isfirstRun = true;
  private static String FRAME_NAME= "IdegaWeb Messenger";
  private static int FRAME_WIDTH = 295;
  private static int FRAME_HEIGHT = 310;
  private static String SESSION_ID = "session_id";
  private static String USER_ID = "user_id";
  private static String USER_NAME = "user_name";
  private static String USER_LIST = "user_list";
  private static String USER_LIST_VERSION = "user_list_version";

  private static String SERVLET_URL = "servlet_url";
  private static String SERVER_ROOT_URL = "server_root_url";
  private static String RESOURCE_URL = "resource_url";

  private String sessionId;
  private String userId;
  private String userName;
  private String userListVersion = "v.0";
  private String servletURL;
  private URL hostURL;
  private String resourceURL;

  private Hashtable dialogs = new Hashtable();
  private ImageLabel faceLabel;
  private ImageLabel logoLabel;
  private MessageListener cycler;

  private AudioClip alertSound;

  //debug
 private Panel userPanel;

  private Thread t;

  private String keyPressed=null;
  //private Image offscreenImage;
  //private Graphics offscr;

  private long checkTimer = 5000;
  private long threadSleep = 50;

  private Packet packetToServlet;
  private Packet packetFromServlet;

  /**Construct the applet*/
  public MessengerApplet() {
  }


  /**Initialize the applet*/
  public void init() {

    setBackground(Color.red);
    setForeground(Color.blue);

    try {
      sessionId = this.getParameter(SESSION_ID, "noId");
      userId = this.getParameter(USER_ID, "-1");
      userName = this.getParameter(USER_NAME, "Anonymous");
      servletURL = this.getParameter(SERVLET_URL, "servlet/ClientServer");
      hostURL = new URL(this.getParameter(SERVER_ROOT_URL, getCodeBase().getProtocol()+"://"+getCodeBase().getHost()));
      resourceURL = this.getParameter(RESOURCE_URL,"/idegaweb/bundles/com.idega.block.messenger.bundle/resources/");
    }
    catch(MalformedURLException e) {
      System.out.println("MessageApplet: error in init!");
      e.printStackTrace(System.err);
    }

    try {
        //faceLabel = new ImageLabel(getImage(new URL(hostURL+resourceURL),"face_in.gif"));
        faceLabel = new ImageLabel(getImage(getCodeBase(),"face_in.gif"));
        System.out.println("CODEBASE = "+getCodeBase());
        logoLabel = new ImageLabel(getImage(getCodeBase(),"idegalogo.gif"));
        //alertSound = getAudioClip(getCodeBase(),"notify.wav");

      }
      catch (Exception ex) {
        ex.printStackTrace(System.err);
      }

      /*

    userPanel = new Panel();
    userPanel.setSize(FRAME_WIDTH,FRAME_HEIGHT);
    add(userPanel);*/

  }

  public void run(){
    if(isfirstRun){
        if(cycler==null){
          cycler = new MessageListener(checkTimer);
          cycler.addActionListener(this);
        }
        cycler.start();
        isfirstRun=false;
    }


    while(runThread){
      //message checking is done in another thread
      repaint();

      try {
        //getToolkit().sync();
        t.sleep(threadSleep);
      }
      catch (InterruptedException e) {
        e.printStackTrace(System.err);
        System.out.println("MessageApplet : Problem in the main thread");
      }
    }
  }

    /**
   * Display the list of Messages <br>
   *
   * Iterate over the vector of Messages and display
   */
  private void dispatchMessagesToDialogs(Vector MessageVector){
    Enumeration enum = MessageVector.elements();

    Message aMessage = null;

    while (enum.hasMoreElements()){

      aMessage = (Message) enum.nextElement();

      MessageDialog messageDialog = (MessageDialog) dialogs.get(Integer.toString(aMessage.getId()));

      if( messageDialog == null ) { //create a new dialog

        messageDialog = createAMessageDialog(false,aMessage);

        messageDialog.addMessage(aMessage);
        messageDialog.setVisible(true);

      }
      else {
        messageDialog.addMessage(aMessage);
        messageDialog.setVisible(true);
      }

    }
  }

  private MessageDialog createAMessageDialog(boolean newId, Message aMessage){
    MessageDialog messageDialog;
    if( logoLabel != null ) messageDialog = new MessageDialog(FRAME_NAME,aMessage,logoLabel);
    else messageDialog = new MessageDialog(FRAME_NAME,aMessage);

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    messageDialog.setLocation((d.width - messageDialog.getSize().width) / 2, (d.height - messageDialog.getSize().height) / 2);
    messageDialog.setSize(FRAME_WIDTH,FRAME_HEIGHT);
    messageDialog.addActionListener(this);
    if( alertSound!=null ) messageDialog.setAudioClip(alertSound);

    if( newId ) dialogs.put(Integer.toString(messageDialog.hashCode()),messageDialog);
    else dialogs.put(Integer.toString(aMessage.getId()),messageDialog);

    return messageDialog;
  }


  private void getMessagesFromDialog(MessageDialog dialog){//gets called on and iw-send event action
    if( packetToServlet == null ){
      packetToServlet = new Packet();
    }
    packetToServlet.setSender(sessionId);

    Vector msg = dialog.getMessages();
    int length = msg.size();
    for (int i = 0; i < length; i++) {
      ((Message)msg.elementAt(i)).setSender(sessionId);
    }


    /**@todo make this work for many dialogs..*/
    packetToServlet.addMessages(msg);
    dialog.clearMessageVector();
    cycle();
  }

  private URLConnection getURLConnection(){
    URLConnection servletConnection = null;

    try{
        // connect to the servlet
        System.out.println("Connecting to servlet...");
        URL servlet = new URL(hostURL,servletURL+"/");

        servletConnection = servlet.openConnection();
        System.out.println("Connected");

        // inform the connection that we will send output and accept input
        servletConnection.setDoInput(true);
        servletConnection.setDoOutput(true);

        // Don't used a cached version of URL connection.
        servletConnection.setUseCaches(false);

        // Specify the content type that we will send binary data
        servletConnection.setRequestProperty("Content-Type", "application/octet-stream");
       //servletConnection.setRequestProperty("Connection", "Keep-Alive");

      }
      catch (Exception e){
          System.out.println("MessengerApplet : error in getURLConnection"+e.toString());
          e.printStackTrace(System.err);
      }
      return servletConnection;
    }


  /**
  *  Sends the message object to a servlet. It is serialized over the URL connection
  */
  private void sendPacket(URLConnection conn){

    ObjectOutputStream outputToServlet = null;

    try{

     // if( isfirstRun ){
        if( packetToServlet == null) packetToServlet = new Packet();

        packetToServlet.addProperty(new Property(SESSION_ID,sessionId));
        packetToServlet.addProperty(new Property(USER_ID,userId));
        /**@todo: send the latest version*/
        packetToServlet.addProperty(new Property(USER_LIST_VERSION,userListVersion));

        packetToServlet.setSender(sessionId);
     //   isfirstRun = false;
     // }

     // if( packetToServlet != null ){
        System.out.println("sending packets");
        outputToServlet = new ObjectOutputStream(conn.getOutputStream());
        // serialize the object
        outputToServlet.writeObject(packetToServlet);

        outputToServlet.flush();
        outputToServlet.close();

        System.out.println("Sending Complete.");
     // }

    }
    catch (IOException e){
        System.out.println(e.getMessage());
        e.printStackTrace(System.err);
    }
  }

    /**
     * Read the input from the servlet.  <b>
     *
     * The servlet will return a serialized Packet ( with messages, processes and/or property changes )
     *
     */
    private void receivePacket(URLConnection conn){
      packetFromServlet = null;
      ObjectInputStream theInputFromServlet = null;
      try{
        System.out.println("receiving packets");

          theInputFromServlet = new ObjectInputStream(conn.getInputStream());
            // read the serialized Packet from the servlet
          System.out.println("Reading data...");
          packetFromServlet = (Packet) theInputFromServlet.readObject();
          theInputFromServlet.close();
          System.out.println("Finished reading data.");

        }
        catch (IOException e){
          System.out.println(e.getMessage());
          e.printStackTrace(System.err);
        }
        catch (ClassNotFoundException e){
          System.out.println(e.getMessage());
          e.printStackTrace(System.err);
        }

    }

  private void processPacket(){

    System.out.println("processing the packet ...");
    //packet.getProperties(); //get properties to change
    if( packetFromServlet!=null ){
      packetFromServlet.process(this);
      Vector messages = packetFromServlet.getMessages();
      if( messages!= null) dispatchMessagesToDialogs(messages);

      Vector props = packetFromServlet.getProperties();
      Vector userlist = null;
      String listVersion = null;

      if( props!=null ){
        int length = props.size();
        for (int i = 0; i < length; i++) {
          if( ((Property)props.elementAt(i)).getKey().equals(USER_LIST) ){
             userlist = (Vector)((Property)props.elementAt(i)).getValue();
          }
          else if (((Property)props.elementAt(i)).getKey().equals(USER_LIST_VERSION) ){
           listVersion = (String)((Property)props.elementAt(i)).getValue();
          }
        }
/**@todo check if latter check is neccessary this should have been done in ClientManager*/
       if( (userlist!=null) && (!userListVersion.equalsIgnoreCase(listVersion)) ){
         userListVersion = listVersion;
         int length2 = userlist.size();
         for (int k = 0; k < length2; k++) {
           Property user = (Property)userlist.elementAt(k);
           addToUserList( user.getKey() , (String)user.getValue() );
          }

         System.out.println("MessengerApplet: userListVersion : "+userListVersion);

        }

      }else System.out.println("MessengerApplet: PROPERTIES IS NULL");

    }else{
     System.err.println("MessengerApplet : packetFromServlet == null !!");
    }

    System.out.println("DONE! processing the packet");

    repaint();

    packetToServlet = null;


  }

  private void addToUserList(String sendToId, String name){
    System.out.println("MessengerApplet: Adding to userlist! id: "+sendToId+" name: "+name);
      Message msg = new Message();
      msg.setSender(sendToId);
      msg.setSenderName(name);
      msg.setRecipientName(userName);

      MessageDialog dialog = createAMessageDialog(true,msg);

      SingleLineItem item = new SingleLineItem(this);
      item.setId(sendToId);
      item.setWindowToOpen(dialog);
      item.addActionListener(this);

      if( faceLabel!= null ) item.add(faceLabel);

      item.add(new Label(name));
      item.setSize(18,150);

      add(item);
      item.repaint();
      repaint();
      item.repaint();


  }

  public synchronized void cycle(){
    URLConnection conn = getURLConnection();
    // send the Packet object to the servlet using serialization
    sendPacket(conn);

    // now, let's read the packet from the servlet.
    receivePacket(conn);

    // get messages, perform processes change properties
    processPacket();

    conn = null;
  }

  private Packet getPacketToServlet(){
    return this.packetToServlet;
  }

  private Packet getPacketFromServlet(){
    return this.packetFromServlet;
  }

  public void actionPerformed(ActionEvent e){
    String action = e.getActionCommand();

    if(action.equalsIgnoreCase("iw-send")){
      MessageDialog d = (MessageDialog) e.getSource();
      getMessagesFromDialog(d);
    }
    else if(action.equalsIgnoreCase("iw-cycle")){
     cycle();
    }

    System.out.println("MessengerApplet: action command was :"+action);
    //debug
  /*  Component[] comps = getComponents();
    for (int i = 0; i < comps.length; i++) {
        comps[i].repaint();
    }*/


    repaint();

  }

  /**
   *  Reads a text response from the servlet.
   */
  protected void readServletResponse(URLConnection servletConnection)
  {
      BufferedReader inFromServlet = null;

      try
      {
              // now, let's read the response from the servlet.
              // this is simply a confirmation string
              inFromServlet = new BufferedReader(new InputStreamReader(servletConnection.getInputStream()));

          String str;
          while (null != ((str = inFromServlet.readLine())))
          {
              System.out.println("Reading servlet response: " + str);
          }

          inFromServlet.close();
      }
      catch (IOException e)
      {
        System.out.println(e.toString());
      }
  }


 public void update(Graphics g){
  paint(g);
 }

  /**Start the applet*/
  public void start() {
    runThread = true;

    if ( t == null ){
      t = new Thread(this,"MessengerApplet thread");
      t.setPriority(t.NORM_PRIORITY);
      t.start();
    }
  }
  /**Stop the applet*/
  public void stop() {
    if ( t != null ){
      runThread = false;
    }

    if(cycler!=null){
     cycler.stop();
    }

  }

  /**Destroy the applet*/
  public void destroy() {
    stop();
    Graphics g = getGraphics();
    if(g != null) {
        g.dispose(); // crucial
        g = null;
    }

    t=null;

    if(cycler!=null){
     cycler.destroy();
    }

    dialogs.clear();
    dialogs=null;
/**@todo travers through hashtable and do this
      messageDialog.setVisible(false);
      messageDialog.cancel();
      messageDialog.dispose();
      messageDialog = null;
*/
  }

  /**Get Applet information*/
  public String getAppletInfo() {
    return FRAME_NAME;
  }


  /**Get a parameter value*/
  public String getParameter(String key, String def) {
    return (getParameter(key) != null ? getParameter(key) : def);
  }

  /**Get parameter info*/
  public String[][] getParameterInfo() {
    String[][] pinfo =
      {
      {"session_id", "String", "The users sessionId"},
      {"user_id", "String", "The users memberId"},
      {"servlet_url", "String", "The clientServers url"},
      };
    return pinfo;
  }

        /*
  public boolean keyDown(Event e, int key){

    switch (key) {
      case 1004://arrow up
        bally+=0.5f;
        break;
      case 1005://arrow down
        bally-=0.5f;
        break;
      case 1006://arrow left
        ballx-=0.5f;
        break;
      case 1007://arrow right
        ballx+=0.5f;
        break;
      case 32://spacebar
        break;
      case 1002 ://Page Up
        scene.defaultCamera.shift(0f,0f,0.2f);
        break;
      case 1003 ://Page Down
        scene.defaultCamera.shift(0f,0f,-0.2f);
        break;
      case 52 ://KeyPad 4
        scene.defaultCamera.shift(0.2f,0f,0f);
        break;
      case 53 ://KeyPad 5
        scene.defaultCamera.shift(0f,0.2f,0f);
        break;
      case 54 ://KeyPad 6
        scene.defaultCamera.shift(-0.2f,0f,0f);
        break;
      case 56 ://KeyPad 8
        scene.defaultCamera.shift(0f,-0.2f,0f);
        break;
      case 43 ://KeyPad +
        scene.scale(1.2f);
        break;
      case 45 ://KeyPad -
        scene.scale(0.8f);
        break;

      case 97 ://a
        antialias=!antialias; scene.setAntialias(antialias);
        break;
      case 65 ://A
        antialias=!antialias; scene.setAntialias(antialias);
        break;
      case 109 ://m
        for (int i=0;i<scene.objects;i++) scene.object[i].meshSmooth();
        break;
      case 77 ://M
        for (int i=0;i<scene.objects;i++) scene.object[i].meshSmooth();
        break;
      case 110 ://n
        scene.normalize();
        break;
      case 78 ://N
        scene.normalize();
        break;
      case 105 ://i
        idx3d.debug.Inspector.inspect(scene);
        break;
      case 73 ://I
        idx3d.debug.Inspector.inspect(scene);
        break;


      default:
        keyPressed = Integer.toString(key);
        break;
    }

    return true;
  }

  public boolean keyUp(Event e, int key){
    //message = "value = " + key;
    return true;
  }

  public boolean mouseEnter(Event e, int x, int y){
    return true;
  }
  public boolean mouseExit(Event e, int x, int y){
    return true;
  }
  public boolean mouseDown(Event e, int x, int y){
    return true;
  }
  public boolean mouseUp(Event e, int x, int y){
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    return true;
  }
  public boolean mouseMove(Event e, int x, int y){
    return true;
  }

  public boolean mouseDrag(Event e, int x, int y){

    repaint();
    return true;
  }
*/

}