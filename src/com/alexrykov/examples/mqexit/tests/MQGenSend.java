package com.alexrykov.examples.mqexit.tests;


import java.io.*;
import java.util.GregorianCalendar;


import com.alexrykov.examples.mqexit.AppIDDataSendExit;
import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;


/**
 * 
 *	MQGenSend is a utility to send messages  
 * 
 * 
 **/
public class MQGenSend
{
	private MQQueueManager queueManager;
	private MQQueue queue;

  

  /**
   * Creates all the necessary objects for sending
   * messages to a JMS queue.
   *
   * @exception NamingException if operation cannot be performed
   * @throws MQException 
   * @exception JMSException if JMS fails to initialize due to internal error
   */
  public void init(String queueManagerName, String channelName,  String queueName, String hostName, int port)
       throws  MQException
  {
	  MQEnvironment.channel = channelName;
      MQEnvironment.hostname = hostName;
      MQEnvironment.port = port;      
      
      //if you want to try exit with mq sender
      MQEnvironment.sendExit = new AppIDDataSendExit("applicationIdData=data");
      
      // get a handle to the QManager
      queueManager = new MQQueueManager(queueManagerName);

      // set the open options for the output queue and
      // set context such that message can change the appDataID
      int openOptions = MQC.MQOO_OUTPUT | MQC.MQOO_FAIL_IF_QUIESCING | MQC.MQOO_SET_IDENTITY_CONTEXT;
      
      queue = queueManager.accessQueue(queueName, openOptions, null, null, null);    
  }
  

  /**
   * Sends a message to a JMS queue.
   * @throws MQException 
   * @throws IOException 
   *
   * @params message  message to be sent
   * @exception JMSException if JMS fails to send message due to internal error
   */
  public void send(String message, String appid) throws MQException, IOException
       
  {

      
          // define the MQMessage for the 
          MQMessage mqMessage = new MQMessage();
          mqMessage.clearMessage(); //  reset the buffer
          mqMessage.correlationId = MQC.MQCI_NONE; //  set correlationId
          mqMessage.messageId = MQC.MQMI_NONE; //  set messageId
          mqMessage.writeString(message); //  set the actual message          
          mqMessage.applicationIdData = appid; // required in our example
           
           
          
          // create message options
          // required to change the applicationIdData
          
          
          MQPutMessageOptions pmo = new MQPutMessageOptions();
          pmo.options = MQC.MQPMO_SET_IDENTITY_CONTEXT;
          
          queue.put(mqMessage, pmo);
  }

  /**
   * Closes JMS objects.
 * @throws MQException 
   * @exception JMSException if JMS fails to close objects due to internal error
   */
  public void close() throws MQException
    
  {
	  queue.close();
	  queueManager.close();	  
  }
  
  
  
  public static void help(){
	  System.out.println("Usage: java MQGenSend -qm QueueManager -q Queue -c Channel -h Host -p Port -a AppIdData -m message [-t MQ tracing level in hex]");
  }
  /** 
  *		main
  */
  public static void main(String[] args)
       throws Exception
  {
	  
	  
	  
	  String queueManager = null, channel = null, queue = null, host = null, appid = null, message = null;
	  int port = -1, tracingLevel = -1;
	  int i = 0;
	  System.out.println("args.length: "+ args.length);
	  while(i<args.length){		  
		  String flag = args[i++];
		  
		  if(flag.equals("-qm"))queueManager = args[i];
		  else if(flag.equals("-c"))channel = args[i];
		  else if(flag.equals("-q"))queue = args[i];
		  else if(flag.equals("-h"))host = args[i];
		  else if(flag.equals("-a"))appid = args[i];
		  else if(flag.equals("-m"))message = args[i];
		  else if(flag.equals("-p"))
			  try{				  
				  port = Integer.parseInt(args[i]);
			  }catch(NumberFormatException nfe){
				  nfe.printStackTrace(System.err);				  
			  }
		  else if(flag.equals("-t"))
			  try{				  
				  
				  tracingLevel = Integer.parseInt(args[i]);
			  }catch(NumberFormatException nfe){
				  nfe.printStackTrace(System.err);				  
			  }
			  
		  i++;
	     
	  }
	  
	  if(queueManager == null || queue == null || channel == null || host == null || port == -1 || appid == null || message == null){
		  help();
		  System.exit(-1);
		  
	  }
	  
	  if(tracingLevel > -1)
		  MQEnvironment.enableTracing(tracingLevel);
	  MQGenSend qs = new MQGenSend();
	  

	  qs.init(queueManager, channel, queue, host, port);
	  qs.send(message, appid);
    
	  qs.close();
  }

}

