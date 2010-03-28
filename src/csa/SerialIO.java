package csa;

import java.io.*;
import java.util.*;
import javax.comm.*;


public class SerialIO {

    private Enumeration portList;
    CommPortIdentifier portId;
    SerialPort serialPort;
    OutputStream outputStream;
    InputStream inputStream;
    private static final int BAUD_RATE = 9600;
    private static final int PORT_ID = 1000;
    public String getNextPortName(){
        
        if(portList.hasMoreElements()) {
            // enumeration refers to Objects - must be cast to required one
            portId = (CommPortIdentifier) portList.nextElement();
            return portId.getName();
        } else return "No valid ports left";
        
    }
    
    public void resetPortList(){
        
        portList = CommPortIdentifier.getPortIdentifiers();
        
    }
    public String setPort(String portname){
        try{
            
            portId = CommPortIdentifier.getPortIdentifier(portname);
        } catch (Exception ex) {
            return  "Exception - " + ex.getMessage();
        }
        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
            
           
            try {
                
                // open using "java lan" as application and 1s as
                // blocking time to actually open
                serialPort = (SerialPort)portId.open("Java Chat App", PORT_ID);
                //System.out.println();
                //set usual parameters
                serialPort.setSerialPortParams(BAUD_RATE,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                
                //turn off blocking
                //serialPort.enableReceiveThreshold(0); //   15/02/07
                
                //set flow control
                serialPort.setRTS(true);
                serialPort.setDTR(true);
                
                //finally set streams
                outputStream = serialPort.getOutputStream();
                inputStream = serialPort.getInputStream();
                
                return "1";
                //Only need 1 port so exit while loop
                
            }//try
            catch ( IOException e ){} catch (UnsupportedCommOperationException e) {
                return "Unsupported use... "+e.getMessage();
                
            } catch (PortInUseException e) {
                return "Port in use... " +e.getMessage();
                
            }
        }//if
        else{
            return "PortIdentifier is not of correct type";
        }
        return "";
    }
    
    public SerialIO( ) {
        
        //CommPortIdentifier is the comms port manager for controlling
        //access ie avaiability and then open/closing
        // getPortIdentifiers() returns an enumeration type
        portList = CommPortIdentifier.getPortIdentifiers();
        
    }//constructor
    
    
    /**
     * 
     * @param b 
     */
    public void putCh( char b ) {
        try {
            outputStream.write(b);
        } catch ( IOException e ){
            e.printStackTrace();
        }
    }//putCh
    
    public void putByte( byte b ) {
        try {
            outputStream.write(b);
        } catch ( IOException e ){
            e.printStackTrace();
        }
    }//putCh
    
    /**
     * @return : first character available from serial comm stream, otherwise
     * -1
     */
    public char getCh() {
        char b = 0;
        try {
            do{
                b = (char)inputStream.read();
            }
            while (b=='\uffff');
        } catch (IOException e){
            e.printStackTrace();
        }
        
        return b;
    }//getCh

    
    
}//serialio

