package com.alexrykov.examples.mqexit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;


import com.ibm.mq.MQC;
import com.ibm.mq.MQChannelDefinition;
import com.ibm.mq.MQChannelExit;
import com.ibm.mq.MQSESSION;
import com.ibm.mq.MQSendExit;


public class AppIDDataSendExit implements MQSendExit{
	
		
	private  byte [] applicationIdDataBytes;
	
	
	public AppIDDataSendExit(String param){

		try{
			Properties props = new Properties();
			//we are loading the initialization parameters from the bndings
			InputStream is = new ByteArrayInputStream(param.getBytes());
			props.load(is);
			is.close();
			
			String applicationIdData = props.getProperty("applicationIdData");
			if(applicationIdData != null){
				//applicationIdData takes 32 bytes
				//the following call prepares the applicationIdDataStr				
				this.applicationIdDataBytes = appendSpacesToLength(applicationIdData, 32); 
			}
		}catch(Exception ex){
			throw new RuntimeException("Unable to load exit parameters.", ex);
		}
		
	}
	
	/*
	 * Extracts bytes from the string upto length and fills it with spaces if required
	 */
	private byte[] appendSpacesToLength(String data, int length){
		byte [] retBuffer = new byte[length];
		byte [] originalData = data.getBytes();
		int originalCopyLength = originalData.length;
		if(originalCopyLength>length)originalCopyLength=length;
		
		System.arraycopy(originalData, 0, retBuffer, 0, originalCopyLength);
		if(originalCopyLength < length)
			Arrays.fill(retBuffer, originalCopyLength, length, (byte)' ');
		return retBuffer;
	}
	/*
	 * Reads int from buffer at specified offset
	 */
	private int readInt(byte [] buffer, int offset){
		int value = 0;
		for(int i=offset;i<offset+4;i++){
			value = (value << 8) + buffer [i];
		}
		return value;
		
	}
	
	/*
	 * Writes int to the buffer at specified offset
	 */
	private void writeInt(int value, byte [] buffer, int offset){
		int shift_bits = 24;
		
		for(int i=offset;i<offset+4;i++){
			buffer [i] = (byte) (value >> shift_bits);
			shift_bits -= 8;
		}
	}
	
	//API calls
	static final int OPERATION_MQ_OPEN_REQUEST = 0x83;
	static final int OPERATION_MQ_PUT_REQUEST = 0x86;
	
	//All offsets bellow are absolute
	//offset of MQMD structure in the buffer
	static final int MD_OFFSET = 44;	
	static final int MD_APPLICATION_ID_DATA_OFFSET = MD_OFFSET + 240;	
	//offset of MQOD structure in the buffer
	static final int OD_OFFSET = 44;	
	static final int OD_V1_SIZE = 168;
	static final int OD_OBJECT_TYPE_OFFSET = OD_OFFSET + 8;
	static final int OPEN_OPTIONS_OFFSET = OD_OFFSET + OD_V1_SIZE;  	
	//offset of PMO structure in the buffer
	static final int PMO_OFFSET = 408;	
	static final int PMO_OPTIONS_OFFSET = PMO_OFFSET + 8;	
	
	//
	static final int TSH_API_CALL_OFFSET = 9;
	static final int MQOT_Q = 1;
	
	
	public byte[] sendExit(MQChannelExit mqChannelExitParams, MQChannelDefinition mqChannelDefinition,
			byte[] agentBuffer) {


		if(mqChannelExitParams.exitReason == MQChannelExit.MQXR_XMIT){			
			if(agentBuffer != null && agentBuffer.length > TSH_API_CALL_OFFSET){
				//mq operation
				int operation = agentBuffer[TSH_API_CALL_OFFSET] & 0xFF; 

				//is this is an MQOPEN call?				 
				if(operation == OPERATION_MQ_OPEN_REQUEST){
					//4b - length of the object type
					if(agentBuffer.length > OD_OBJECT_TYPE_OFFSET + 4){
						int openObjectType = readInt(agentBuffer, OD_OBJECT_TYPE_OFFSET);
						//if object type = MQOT_Q, this is a queue that is currently being opened
						if(openObjectType == MQOT_Q ){		
							System.out.println("**************************");
							//add MQOO_SET_IDENTITY_CONTEXT flag
							int openFlag = readInt(agentBuffer, OPEN_OPTIONS_OFFSET);
							openFlag |= MQC.MQOO_SET_IDENTITY_CONTEXT;
							writeInt(openFlag, agentBuffer, OPEN_OPTIONS_OFFSET);
						}
					}
				}			
				//is this an MQPUT call?
				
				if(operation == OPERATION_MQ_PUT_REQUEST){
						
						//put data in						
						if(agentBuffer != null){
							System.arraycopy(applicationIdDataBytes, 0, agentBuffer, MD_APPLICATION_ID_DATA_OFFSET, applicationIdDataBytes.length);							
						}
						int putOption = readInt(agentBuffer, PMO_OPTIONS_OFFSET);
						//set put option MQC.MQPMO_SET_IDENTITY_CONTEXT
						putOption |= MQC.MQPMO_SET_IDENTITY_CONTEXT;
						writeInt(putOption, agentBuffer, PMO_OPTIONS_OFFSET);
					}
				
			}
        }

        return agentBuffer;		
	}
}
