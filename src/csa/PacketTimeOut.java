package csa;

import java.util.*;

public class PacketTimeOut extends TimerTask {

    private int currentRepetition;
    private int totalRepeats;
    private Timer timer;
    private boolean mCompleted=false;
    private PendingTable.PendingTableEntry mPtEntry;

    public PacketTimeOut(int pTotalRepeats, Packet p) {
        totalRepeats = pTotalRepeats;
        mPtEntry = Global.gPendingTable.GetPendTableEntry(p.GetDestination());
	mPtEntry.setPacket(p);
        currentRepetition = 1;
        
        
    }
    public void startTimer()
    {
       timer = new Timer();
       
       timer.scheduleAtFixedRate(this,Global.PACKET_TIMEOUT_DELAY, 
	     Global.PACKET_TIMEOUT_DELAY);
       
    }
   public void setCurrentTransmission(int currentRepetition)
   {
      this.currentRepetition = currentRepetition;
   }

   public int getCurrentTransmission()
   {
      return currentRepetition;
   }

   public boolean isFinalTransmissionCompleted()
   {
      return mCompleted;
   }
    
    public void run() {
        if (currentRepetition < totalRepeats) 
	{
            if (mPtEntry.getPacket() != null) 
            {
                Global.gUserIO.print(AttemptTransmit());
            } else 
            {
                cancel();
		mCompleted=true;
                
            }
        }
    }

    @Override
    public boolean cancel() {

        currentRepetition = totalRepeats + 1;
        return super.cancel();
    }

  

    public String AttemptTransmit()
    {    
      /*
      this elimates the case where
      a packet has been removed due to a returned R packet
      and transmission is attempted infintely
      */
      if (mPtEntry.HasPacket()) 
      {
	 return AttemptTransmit(mPtEntry.getPacket());
      }
      else 
      {
	 return "\n\nNo packet left to retransmit";
      }
    }
    
    public synchronized String AttemptTransmit(Packet outwardPacket) 
    
    {
      String output="";
        
      if (currentRepetition < totalRepeats) 
      {
	  // transmit packet if not reached maximum
	 currentRepetition++;
	 Global.gMessageTransmitter.sendPacket(outwardPacket);
	 if (Global.gDebugMode) 
	 {
	    output = "\nAttempted Retransmission " + (currentRepetition);
	    output += " of " + totalRepeats;
	    output+= ".\nMe->: " + outwardPacket.toString();
	 }
	 else output+=".";
      } 

      else //number of transmissions over, stop sending
      {
	 char outwardDestination = outwardPacket.GetDestination();

	 if    ((outwardDestination!=Global.gUserID)
	       && outwardDestination!=Global.gMyRequestedLoginID)

	 {
	    //  send proxy logout, halt current transmissions, add proxy packet
	    Packet proxyLogOutPacket = Global.createProxyLogout(outwardDestination);
	    Global.haltTransmissionsAddProxyPacketAndSend(proxyLogOutPacket);

	    output = "\n\nFinal message sending failed. Proxy logout packet sent for user " 
		  + String.valueOf(outwardDestination)+"\n\n";

	    if (Global.gDebugMode)
	    {
	       output +="\nMe->: " + proxyLogOutPacket.toString();
	    }
	 }
	 else
	 {//Don't logout self 
	     output = "\nPacket sending to self failed. Possible problem with network?";
	     if(Global.gDebugMode)
		output+= ("\nMe<-: " + outwardPacket.toString());

	 }
	 //delete packet, cancel this task
	 this.cancel();
	 mCompleted=true;

      }
      return output;
    }


   
}