package com.alexrykov.examples.mqexit.tests;

import java.util.*;
import javax.naming.*;
import javax.jms.*;
import com.ibm.mq.MQEnvironment;
public class QueueGenSend
{
  
  private QueueConnectionFactory qconFactory;
  private QueueConnection qcon;
  private QueueSession qsession;
  private QueueSender qsender;
  private Queue queue;
  private TextMessage msg;
  

  /**
   * Creates all the necessary objects for sending
   * messages to a JMS queue.
   *
   * @param ctx JNDI initial context
   * @param queueName name of queue
   * @exception NamingException if operation cannot be performed
   * @exception JMSException if JMS fails to initialize due to internal error
   */
  public void init(Context ctx, String queueName, String jmsFactory)
       throws NamingException, JMSException
  {

	//lookup queue connection factory
    qconFactory = (QueueConnectionFactory) ctx.lookup(jmsFactory);
    //create queue connection
    qcon = qconFactory.createQueueConnection();
    //create session
    qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    //lookup queue
    queue = (Queue) ctx.lookup(queueName);
    //create sender
    qsender = qsession.createSender(queue);
    //create a message
    msg = qsession.createTextMessage();    
    qcon.start();
    
  }

  /**
   * Sends a message to a JMS queue.
   *
   * @params message  message to be sent
   * @exception JMSException if JMS fails to send message due to internal error
   */
  public void send(String message)
       throws JMSException
  {  
    msg.setText(message);
    qsender.send(msg);
  }

  /**
   * Closes JMS objects.
   * @exception JMSException if JMS fails to close objects due to internal error
   */
  public void close()
       throws JMSException
  {
    qsender.close();
    qsession.close();
    qcon.close();
  }	

  private static InitialContext getInitialContext(String jndiFactory, String url)
       throws NamingException
  {
    Hashtable env = new Hashtable();
    env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
    env.put(Context.PROVIDER_URL, url);    
    
  
    return new InitialContext(env);
  }

  public static void help(){
	  	System.out.println("Usage: java QueueGenSend -jf JNDIFactory -url providerURL -f JMSFactoryJNDIName -q queueJNDIName -m message [-t MQ tracing level in hex]");
	  }
	 /** main() method.
	  *
	  * @param args WebLogic Server URL
	  * @exception Exception if operation fails
	  */
	  public static void main(String[] args)
	       throws Exception
	  {
	  	String	 jndiFactory = null;
		String 	 providerURL = null;
		String 	 jmsFactory = null;
		String 	 jmsQueue = null;
		String message = null;
	    int i=0, tracingLevel = -1;
	    
	    while(i<args.length){
	    	String flag = args[i++];
	    	if(flag.equals("-jf"))jndiFactory = args[i];
	    	else if(flag.equals("-url"))providerURL = args[i];
	    	else if(flag.equals("-f"))jmsFactory = args[i];
	    	else if(flag.equals("-q"))jmsQueue = args[i];
	    	else if(flag.equals("-m"))message = args[	i];
	    	else if(flag.equals("-t"))
			  try{				  
				  tracingLevel = Integer.parseInt(args[i]);
			  }catch(NumberFormatException nfe){
				  nfe.printStackTrace(System.err);				  
			  }
	    	else {
	    		help();
	    		System.exit(-1);
	    	}
	    	
	    	i++;
	    	
	    }	
	    
	    if(jndiFactory == null || providerURL == null || message == null || jmsQueue == null || jmsFactory == null){    
	    	help();
	    	System.exit(-1);
	    }
	    
	    if(tracingLevel > -1)
			  MQEnvironment.enableTracing(tracingLevel);
	    InitialContext ic = getInitialContext(jndiFactory, providerURL);
	    
	    QueueGenSend qs = new QueueGenSend();
	    try{
		    qs.init(ic, jmsQueue, jmsFactory);	    
		    qs.send(message);
		    qs.close();
	    }catch(JMSException ex){
	    	ex.getLinkedException().printStackTrace();
	    }
	  }   
}