package com.idega.block.messenger.data;

import java.io.Serializable;
import java.util.Vector;
import com.idega.block.messenger.presentation.IdegaClient;
import com.idega.block.messenger.data.Message;
/**
 * Title:        idega classes
 * Description:  idega classes
 * Copyright:    Copyright (c) 2001
 * Company:      Idega Software
 * @author <a href="mailto:eiki@idega.is">Eirikur S. Hrafnsson</a>
 * @version 1.0
 */

public class Packet implements Serializable{

  /*public static final int PROCESS = 1;
  public static final int PROPERTY_CHANGE = 2;
  public static final int MESSAGE = 3;
  public static final int PROCESS_TYPE = 3;*/
  private Vector messages;
  private Vector properties;
  private String clientId;

  public void process(){}
  public void process(IdegaClient client){}

  /*
  public void setPacketType(int packetType){
    this.PACKET_TYPE = packetType;
  }

  public int getPacketType(){
    return PACKET_TYPE;
  }*/

  public void addMessage(Message message){
   if( messages == null ){
    messages = new Vector();
   }
    messages.addElement(message);
  }

  public void addMessages(Vector messages){
   if( messages != null ){
    int length = messages.size();
    for (int i = 0; i < length; i++) {
      addMessage((Message) messages.elementAt(i));
    }
   }
  }

  public void setMessages(Vector messages){
   this.messages = messages;
  }

  public Vector getMessages(){
   return this.messages;
  }

  public void addProperty(Property property){
   if( properties == null ){
    properties = new Vector();
   }
    properties.addElement(property);
  }

  public Vector getProperties(){
   return this.properties;
  }

  public void setSender(String clientId){
    this.clientId = clientId;
  }

  public String getSender(){
    return this.clientId;
  }

  public void clearAll(){
    messages=null;
    properties=null;
    clientId=null;
  }

  public void clearMessages(){
    messages=null;
  }
}