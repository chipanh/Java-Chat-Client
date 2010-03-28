package csa;


import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MessageTransmitter implements Runnable {
    
    private Packet mPacket;
    private boolean alive = true;

    
    
    public void sendPacket(Packet pPacket) {
        // sets member variable so that run method picks up change
        // and sends packet
        mPacket = pPacket;
        
        
    }
    
   
    public void killThread(){
        this.alive=false;
    }
    
    public void run() {
        while(alive){
         try
         {
            
            java.lang.Thread.sleep(csa.Global.THREAD_TIME_OUT_LONG);
            if (mPacket != null)
            {
               // if packet waiting then transmit else sleep
	    try
               {
	       synchronized(this)//stop overlapping packets being sent
	       {
                  String transmit = "{" + mPacket.GetDestination() + mPacket.GetSource() 
			+ mPacket.GetTypeAsChar() + mPacket.GetPayload();

                  //     {   D   S   T   f   o   u   r   -   f   o   u   r   !   C   }
                  //     0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
                  //     1st 2nd 3rd 4th 5th 6th 7th 8th 9th 10th11th12th13th14th15th16th
                  for (int i = 0; i < csa.Global.PACKET_SIZE - 2; i++)
                  {

                     csa.Global.gSerialIO.putCh(transmit.charAt(i));
                  }

                  //add checksum and final bracket
                  csa.Global.gSerialIO.putByte(mPacket.CalculateButNotSetCorrectCheckSum());
                  csa.Global.gSerialIO.putCh('}'); //end of packet
                  mPacket = null;
	       }
               }
               catch (java.lang.Exception ex)
               {
                  mPacket = null;
                  ex.printStackTrace();
               }
            }
         }
         catch (InterruptedException ex)
         {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
         }
 
        }
    }
    
    
}
