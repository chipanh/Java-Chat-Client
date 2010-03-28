/*
 * IncomingPacketHub.java
 */

package csa;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IncomingPacketHub implements Runnable
{

   private boolean alive = true;
   
   private char[] rxdChars;
   private int rxdCharCount;

   public IncomingPacketHub()
   {

      rxdCharCount = 0;
      rxdChars = new char[Global.PACKET_SIZE];
   }

   public void run()
   {
      char b;
      while (alive)
      {

         try
         {
            //keep looping until requested to 'die'
//     {   D   S   T   f   o   u   r   -   f    o    u    r    !     C   }
//     00  01  02  03  04  05  06  07  08  09   10   11   12   13   14   15
//     1st 2nd 3rd 4th 5th 6th 7th 8th 9th 10th 11th 12th 13th 14th 15th 16th
            do
            {

               b = csa.Global.gSerialIO.getCh();
            } while (b != '{'); //ignore any non-start of packet characters
            //Insert bracket
            rxdChars[0] = '{';
            rxdCharCount = 1;

            while (rxdCharCount < csa.Global.PACKET_SIZE)
            {
               //loop till end of packet
               b = csa.Global.gSerialIO.getCh();
               rxdCharCount++;
               rxdChars[rxdCharCount - 1] = b;
            }


            if (rxdChars[csa.Global.PACKET_SIZE - 1] == '}')
            {
               //Here is where the main work gets called
               String packetOutcome = ProcessPacket(rxdChars);
               if (packetOutcome.length()>0)
		  Global.gUserIO.print("\n" + packetOutcome);
               //reset input buffer
               rxdCharCount = 0;
            }
            else
            {
               csa.Global.gUserIO.print("Problem with incoming data packet");
            }

            java.lang.Thread.sleep(csa.Global.THREAD_TIME_OUT);
            // Reset wait flag after wait has happened.
         }
         catch (InterruptedException ex)
         {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
         }
         // Reset wait flag after wait has happened.
      } //while
   } //run

   public void killThread()
   {
      alive = false;
   }

   private String CancelLocalLoginAttempt(Packet p)
   {
      // Cancel my login attempt
      csa.Global.haltTransmission(Global.gMyRequestedLoginID);
      Global.gMyRequestedLoginID = csa.Global.NULL_LOGIN_ID;
      csa.Global.gUserID = csa.Global.NULL_LOGIN_ID;
      csa.Global.gLoggedIn = false;

      // Log in other user
      csa.Global.gPendingTable.LogInUser(p.GetDestination());

      //payload is different
      return "\n\nSomeone is logged in with that ID - you\'ll need to choose another login ID.\n";
   }

   private String DisplayPacket(Packet p)
   {
      String outcome = "";
      if (Global.gDebugMode)
      {
         outcome = "\n\nPacket received: " + (p.toString());
      }

      outcome += "\n\n" + String.valueOf(p.GetSource());
      outcome += ": ";
      outcome += p.GetPayload() + "\n";

      return outcome;
   }

   private String ProcessPacket(char[] rxdChars)
   {

      Packet p = CreatePacket(rxdChars);
      String packetOutcome = "";
      // if packet is OK and destination is this terminal then display
      if (p.IsCheckSumOK())
      {

         if (Global.gDebugMode)
         {
            packetOutcome = ("\nMe<-: ");
            packetOutcome += (p.toString()) + "\n";
         }

         packetOutcome += DecisionChart(p);
      }
      else
      {
         //Checksum not OK
         //drop packet garbled - ignore
         if (Global.gDebugMode)
         {
            packetOutcome = ("\nGarbled packet received: ");
            packetOutcome += (p.toString());
         }
      }
      return packetOutcome;
   }

   private Packet CreatePacket(final char[] rxdChars)
   {

      Packet p = new Packet();

      p.SetDestination(rxdChars[1]);
      p.SetSource(rxdChars[2]);
      p.SetType(rxdChars[3]);


      //     {   D   S   T   P   a   y   l   o   a    d    !    !    !    C    }
      //     0   1   2   3   4   5   6   7   8   9    10   11   12   13   14   15
      //     1st 2nd 3rd 4th 5th 6th 7th 8th 9th 10th 11th 12th 13th 14th 15th 16th
      //take off 1 for bracket and 1 because the array starts at 0
      int positionOfCheckSum = Global.PACKET_SIZE - 2;

      //get payload
      int count;
      String payload = "";

      for (count = Global.START_OF_PAYLOAD; count < positionOfCheckSum; count++)
      {
         payload = payload + rxdChars[count];
      }

      p.SetPayload(payload);
      p.ManuallySetCheckSum((byte) rxdChars[positionOfCheckSum]);
      return p;
   }

   private void PerformLocalLogin()
   {
      // Set userID to Login Character
      Global.haltTransmission(Global.gMyRequestedLoginID);
      Global.gUserID = Global.gMyRequestedLoginID;
      Global.gPendingTable.LogInUser(Global.gMyRequestedLoginID);

      Global.gLoggedIn = true;
   }

   private String ResponsePacketSameAsSentPacket(Packet p, char source, String retString)
   {
      //returns 1 if true, 0 if false, -1 if error
      int sameStamp = HasSameStampAsSentPacket(p);

      if (sameStamp == 1)
      {
         if (!Global.gLoggedIn)
         {
            PerformLocalLogin(); //successful login completed
            retString = "\n\nLogin successful: you are logged in as " + java.lang.String.valueOf(Global.gMyRequestedLoginID) + "\n";
         }
         if (loginUser(source))
         {
            retString += "\nLog in response from " + String.valueOf(source) + "\n";
         }
      }

      if (sameStamp == 0)
      {
         //payload is different
         retString = "\n\nSomeone just pipped you to the post on login - you\'ll need to choose another login ID.\n";

         retString += CancelLocalLoginAttempt(p);
         // Log them in
         loginUser(source);
         // Show new menu
      }
      if (sameStamp == -1)
      {
         CancelLocalLoginAttempt(p);
         retString = ("\n\nPacket sending timed out cannot verify if incoming packet was sent from local machine");
      }

      return retString;
   }

   private String processNakReceivedFromMeToMe(Packet p)
   {
      // I sent myself a NAK - therefore packet must have been broken
      // or ring is broken
      String output = "";
      PendingTable.PendingTableEntry pt = Global.GetPendTableEntry(p.GetSource());
      if (pt.getTransmissionAttempts() < Global.TRANSMISSION_ATTEMPTS)
      {

         output = pt.getPacketTimeOut().AttemptTransmit();
      }
      else
      {

         output = ("\n\nYou have an error on the network" + "the preceding packet (From you to you) was not received" + String.valueOf(Global.TRANSMISSION_ATTEMPTS + 1) + " times.");
      }
      return output;
   }

   private String HandleLocalLoginPacket(Packet p)
   {

      // if incoming packet payload is same as the stored random stamp
      // on the requested login packet then this is the same packet
      // being returned
      Packet pStoredPacket = Global.GetPendTableEntry(Global.gMyRequestedLoginID).getPacket();
      String output = "";
      String strStoredPayload = "";
      String strIncomingPayload = "";

      strIncomingPayload = p.GetPayload();
      if (pStoredPacket != null)
      {
         strStoredPayload = pStoredPacket.getRandomStamp();
      }
      //now check payload
      if (strIncomingPayload.equals(strStoredPayload))
      {
         PerformLocalLogin(); //successful login completed
         output = "\n\nLogin successful. You are now logged in as " + Character.toString(Global.gMyRequestedLoginID) + "\n";
      }
      else
      {
         /* this is a remote users login attempt with same ID
         so cancel current attempt
          */
         CancelLocalLoginAttempt(p);

         //Forward their packet on
         Global.gMessageTransmitter.sendPacket(p);
         if (loginUser(p))
         {

            output = "\n\nIncoming login packet received by another user with a different payload." + "\nLogin attempt cancelled. You\'ll need to try logging in again";
         }
         else
         {
            output = "\n";
         }
      }

      return output;
   }

   private String HandleLoginPacketWhenLoggedOut(Packet p)
   {
      char source = p.GetSource();
      char destination = p.GetDestination();
      String output = "";
      // check if packet is Local Requested login ID
      if ((source == Global.gMyRequestedLoginID) & (destination == Global.gMyRequestedLoginID))
      {
         return HandleLocalLoginPacket(p);
      }
      else
      {
         //if not logged in log them in
         if (loginUser(p.GetDestination()))
         {
            output = "\n\n" + Character.toString(p.GetDestination()) + " has just logged in\n";
         }
         else
         {
            if (Global.gDebugMode)
            {
               output = "\n\nSurplus login packet received from " + Character.toString(p.GetDestination()) + " - already logged in. Forwarded on for that user to handle";
            }
         }
         Global.gMessageTransmitter.sendPacket(p);
         return output;
      }
   }

   private int HasSameStampAsSentPacket(Packet p)
   {

      // if incoming packet payload is same as the stored random stamp
      // on the requested login packet then this is the same packet
      // being returned
      Packet pStoredPacket = Global.GetPendTableEntry(Global.gMyRequestedLoginID).getPacket();
      if (pStoredPacket != null)
      {
         String strStoredPayload = pStoredPacket.getRandomStamp();
         String strIncomingPayload = p.GetPayload();

         //now check payload
         if (strIncomingPayload.equals(strStoredPayload))
         {
            return 1; //true
         }
         else
         {
            return 0; //false
         }
      }
      else
      {
         return -1; //error
      }
   }

   private String HandleResponsePacketWhenLoggedOut(Packet p)
   {
      String retString = "";
      char source = p.GetSource();
      char destination = p.GetDestination();

      // check if packet is Local Requested login ID
      if (destination == Global.gMyRequestedLoginID)
      {
         return retString = ResponsePacketSameAsSentPacket(p, source, retString);
      }
      else
      {
         if (loginUser(source))
         {
            retString += ("\n\n" + Character.toString(source) + " is logged in\n");
         }

         if (loginUser(destination))
         {
            retString += ("\n\n" + Character.toString(destination) + " is logged in\n");
         }

         //Forward it on
         Global.gMessageTransmitter.sendPacket(p);
         return retString;
      }
   }

   private String HandleLogoutPacketForNonLoggedInUser(Packet p)
   {
      String retString = "";
      char incomingPacketSource = p.GetSource();


      boolean incomingIsProxy = p.GetPayload().equals(Global.PROXY_LOGOUT_MESSAGE);

      if (incomingIsProxy)
      {
         retString = handleProxyLogOutPacket(p);
      }
      else
      {
         if (Global.gMyLogOutPacket != null)
         {
            Packet pStoredLogOut = Global.gMyLogOutPacket;
            char requestedLogOutId = pStoredLogOut.GetSource();

            String stamp = pStoredLogOut.GetPayload();

            if ((incomingPacketSource == requestedLogOutId) && 
		  (p.GetPayload().equals(stamp)))
            {
               //this is returned local logout packet
               retString = "\n\nYour logout packet was successfully returned\n";
               logOutUser(requestedLogOutId);
	       Global.gUserID = Global.NULL_LOGIN_ID;
               Global.gPendingTable.RemovePacket(requestedLogOutId);
               Global.gMyLogOutPacket = null;
               Global.gLoggedIn = false;
            }
         }
         else
         {
            Global.gMessageTransmitter.sendPacket(p);
            logOutUser(p.GetDestination());
            retString = "\n\n" + Character.toString(p.GetDestination()) + " has logged out\n";
         }
      }
      return retString;
   }

   private String HandleLogoutPacketWhenLoggedOut(Packet p)
   {
      String retString = "";

      // Log them out
      if (Global.GetPendTableEntry(p.GetDestination()).isLoggedIn())
      {

         Global.gPendingTable.LogOutUser(p.GetDestination());
         //Forward it on
         Global.gMessageTransmitter.sendPacket(p);
         retString = ("\n" + Character.toString(p.GetDestination()) + " has just logged out\n");
      }
      else
      {
         retString = HandleLogoutPacketForNonLoggedInUser(p);
      }
      return retString;
   }


 private String HandleDataPacketWhenLoggedOut(Packet p)
   {

      String output = "";
      if ((p.GetSource() == Global.gMyRequestedLoginID))
      {
         CancelLocalLoginAttempt(p);

         output = "\nMessage sent from " + String.valueOf(p.GetSource()) + " received you must choose another loginID";
         loginUser(p.GetSource());
      }
      else
      {
         //just retransmit
         Global.gMessageTransmitter.sendPacket(p);
         if (loginUser(p.GetSource()))
	 {//if not already logged in notify
	    output += "\n\n"+String.valueOf(p.GetSource()) + " is logged in\n";
	 }
	 if (Global.gDebugMode)
         {
            output = "\n<->: " + p.toString()+"\n";
         }
      }
      return output;
   }


   private String HandleAcknowledgePacketWhenLoggedOut(Packet p)
   {

      String output = "";
      if ((p.GetSource() == Global.gMyRequestedLoginID) && (p.GetDestination() == Global.gMyRequestedLoginID))
      {
         CancelLocalLoginAttempt(p);

         output = "\nACK from " + String.valueOf(p.GetSource()) + " received you must choose another loginID";
         loginUser(p.GetDestination());
      }
      else
      {
         //just retransmit
         Global.gMessageTransmitter.sendPacket(p);
      
      }
      return output;
   }

   private String HandlePacketsWhenLoggedOut(Packet p)
   {
      String output = "";
      switch (p.GetType())
      {

         case LOGIN:
            output = HandleLoginPacketWhenLoggedOut(p);
            break;
         case LOG_OUT:
            output = HandleLogoutPacketWhenLoggedOut(p);
            break;
         case LOGIN_RESPONSE:
            output = HandleResponsePacketWhenLoggedOut(p);
            break;
         case ACKNOWLEDGE:
            output = HandleAcknowledgePacketWhenLoggedOut(p);
            break;
         case DATA_PAYLOAD:
	    output= HandleDataPacketWhenLoggedOut(p);
	    break;
	 
         default:
            Global.gMessageTransmitter.sendPacket(p);
            
            break;
      }
      return output;
   }

   private String DecisionChart(Packet p)
   {
      // check in case it is a login packet being returned
      if (!Global.gLoggedIn)
      {
         return HandlePacketsWhenLoggedOut(p);
      }
      else
      {
         char destination = 'r';
         char source = 'r';

         if (p.GetDestination() == Global.gUserID)
         {
            destination = 'l';
         }
         if (p.GetSource() == Global.gUserID)
         {
            source = 'l';
         }
         if ((source == 'l') & (destination == 'l'))
         {
            return LocalToLocal(p);
         }
         if ((source == 'l') & (destination == 'r'))
         {
            return LocalToRemote(p);
         }
         if ((source == 'r') & (destination == 'l'))
         {
            return RemoteToLocal(p);
         }
         if ((source == 'r') & (destination == 'r'))
         {
            return RemoteToRemote(p);
         }
      }
      return "\n\nShouldn\'t see this message\n";
   }

   private String LocalToLocal(final Packet p)
   {
      String output = "";
      switch (p.GetType())
      {
         case LOGIN:
            {

               /*
               If an ID duplication has
               occurred, the other station will see its own ID,
               remove the packet and transmit an ACK packetback.
               Should this happen, an error message should be displayed,
               and a newIDrequested. ACK & NAK packets are never
               acknowledged.
                */

               p.SetType(Global.PacketType.ACKNOWLEDGE);
               p.setRandomStamp();
               p.CalculateAndSetCheckSum();
               Global.gMessageTransmitter.sendPacket(p);

               if (Global.gDebugMode)
               {
                  output = "\n\nAttempted login by another user with your ID, packet discarded, ACK sent\n";
               }
               break;
            }
         case LOGIN_RESPONSE:
            {
               //  R-response to login packet  illegal
               if (Global.gDebugMode)
               {
                  output = "\n\nIllegal response to login received using local User ID\n";
               }
               break;
            }
         case DATA_PAYLOAD:
            {
               //D-data payload             
	       //message to self display, return ACK, cancel pending
	       output=DisplayPacket(p);
	       Global.gPendingTable.haltTranmission(p.GetSource());
	       returnACK(p);
               if (Global.gDebugMode)
               {
                  output = "\n\nSilly messg to self, returnACK\n";
               }
               break;
            }
         case ACKNOWLEDGE:
            {
               //A-acknowledge,ACK           message to self acknowledged
               if (Global.gDebugMode)
               {
                  output = "\n\nMessage to self acknowledged\n";
               }
               break;
            }
         case NON_ACKNOWLEDGE:
            {
               //N-nonacknowledge,NAK       processNakReceivedFromMeToMe
               processNakReceivedFromMeToMe(p);
               if (Global.gDebugMode)
               {
                  output = "\n\nThere is a problem with the network you received a NAK from " + "yourself\n";
               }
               break;
            }
         case LOG_OUT:
            {
               //X-logout byuser
               logOutUser(p.GetSource());
               output = "\n\nYour logout packet was returned - everyone knows you are gone\n";
               break;
            }
         default:
            {
               output = "\n\nUnknown packet type received\n";
               break;
            }
      }
      return output;
   }


//LOGIN    LOGIN_RESPONSE   DATA_PAYLOAD    ACKNOWLEDGE  NON_ACKNOWLEDGE  LOG_OUT
   private String LocalToRemote(final Packet p)
   {
      String output = "";
      switch (p.GetType())
      {
         case LOGIN:
            {
               //L-login bynew user          illegal
               if (Global.gDebugMode)
               {
                  output = "\n\nInvalid login packet received. \n";
               }
               break;
            }
         case LOGIN_RESPONSE:
            {
               // error,where has he gone now?
               Global.gPendingTable.LogOutUser(p.GetDestination());
               if (Global.gDebugMode)
               {
                  output = "\n\nLogin_Reponse returned, logged out user: " + String.valueOf(p.GetDestination());
               }
               break;
            }
         case DATA_PAYLOAD:
            {
               //D-data payload Message failed
               if (Global.gDebugMode)
	       {
		  output = "\n\nMessage sending to " 
			+ String.valueOf(p.GetDestination()) 
			+ " failed\n";
	       }

               output += reTransmitPendingTableEntry(p);
               break;
            }
         case ACKNOWLEDGE:
            {
               //A-acknowledge,ACK
               char c = p.GetDestination();
               //logOutUser(c);
               
               Packet r = Global.createProxyLogout(c);
	       Global.haltTransmissionsAddProxyPacketAndSend(r);
               if (Global.gDebugMode)
               {
                  output = "\n\nLocally initiated ACK returned. Sending proxy logout packet";
               }

               break;
            }
         case NON_ACKNOWLEDGE:
            {
               //N-nonacknowledge,NAK
               char c = p.GetDestination();
               Packet r = Global.createProxyLogout(c);
	       Global.haltTransmissionsAddProxyPacketAndSend(r);
	       
               if (Global.gDebugMode)
               {
                  output = "\n\nLocally initiated NAK returned. Sending proxy logout packet for user\n";
               }
              
               break;
            }
         case LOG_OUT:
            {
               //X-logout byuser
               if (Global.gDebugMode)
               {
                  output = "\n\nIllegal logout packet received\n";
               }
               break;
            }
         default:
            output = "\n\nUnknown packet type";
            break;
      }
      return output;
   }

   private String RemoteToLocal(final Packet p)
   {
      String output = "";

      switch (p.GetType())
      {

         case LOGIN:
            {
               //L-login bynew user          mylogin id is OK, del packet
               if (Global.gDebugMode)
               {
                  output = "\nlogin attempt by new user illegal - packet must have matching source and destination";
               }
               break;
            }
         case LOGIN_RESPONSE:
            {
               // R-response
               loginUser(p.GetSource());
               output = "\nLogin Response received from " + String.valueOf(p.GetSource());
               break;
            }
         case DATA_PAYLOAD:
            {
               //D-data payload	rxfailed, del packet, re-tx from pending table
               output = DisplayPacket(p);
               returnACK(p);

               break;
            }
         case ACKNOWLEDGE:
            {
               //A-acknowledge,ACK           rxfailed, del packet,
               ACKReceivedUpdatePendingPackets(p);
               if (Global.gDebugMode)
               {
                  output = "\n\nACK received\n";
               }
               break;
            }
         case NON_ACKNOWLEDGE:
            {
               //N-nonacknowledge,NAK        rxfailed, del packet,
               if (Global.gDebugMode)
               {
                  output = "\nNAK for one of your messages was received. Retransmitting.";
               }
               output = reTransmitPendingTableEntry(p);
               break;
            }
         case LOG_OUT:
            {
               //X-logout byuser
               if (Global.gDebugMode)
               {
                  output = "\nlogout by user illegal";
               }
               break;
            }
         default:
            if (Global.gDebugMode)
            {
               output = "\nUnknown packet type received";
            }
            break;
      }
      return output;
   }

   private String RemoteToRemote(final Packet p)
   {
      String output = "";
      switch (p.GetType())
      {

         case LOGIN:
            {
               //Login packet received - forward and update login table
               loginUser(p.GetSource());
               reTransmitPacket(p);
               returnRACKPacket(p);
               output = "\n\n" + String.valueOf(p.GetSource()) + " has just logged in\n";
               break;
            }
         case LOGIN_RESPONSE:
            {
               //response to login packet just forward and login both users
               if (loginUser(p.GetSource()))
               {
                  output = "\n\n" + String.valueOf(p.GetSource()) + " is logged in\n";
               }
               if (loginUser(p.GetDestination()))
               {
                  output = "\n\n" + String.valueOf(p.GetSource()) + " is logged in\n";
               }
               reTransmitPacket(p);


               break;
            }
         case DATA_PAYLOAD:
            {
               if (loginUser(p.GetSource()))
               {
                  output = "\n\n" + String.valueOf(p.GetSource()) + " is logged in\n";
               }
               if (loginUser(p.GetDestination()))
               {
                  output = "\n\n" + String.valueOf(p.GetSource()) + " is logged in\n";
               }
               reTransmitPacket(p);

               if (Global.gDebugMode)
               {
                  output = "\n\nMessage from " + String.valueOf(p.GetSource()) + " to " + String.valueOf(p.GetDestination()) + ". Forwarding on.\n";
               }
               break;
            }
         case ACKNOWLEDGE:
            {
               //A-acknowledge,ACK          re-tx packet
               if (loginUser(p.GetSource()))
               {
                  output = "\n\n" + String.valueOf(p.GetSource()) + " is logged in\n";
               }
               if (loginUser(p.GetDestination()))
               {
                  output = "\n\n" + String.valueOf(p.GetSource()) + " is logged in\n";
               }
               reTransmitPacket(p);
               if (Global.gDebugMode)
               {
                  output += "\n\nACK Received. Forwarding on.\n";
               }
               break;
            }
         case NON_ACKNOWLEDGE:
            {
               //N-nonacknowledge,NAK       re-tx packet
               loginUser(p.GetSource());
               reTransmitPacket(p);
               if (Global.gDebugMode)
               {
                  output = "\n\nNAK Received. Forwarding on.\n";
               }

               break;
            }
         case LOG_OUT:
            {
               // if proxy then handle that case
               if (p.GetPayload().equals(Global.PROXY_LOGOUT_MESSAGE))
	       {
		  output = handleProxyLogOutPacket(p);
	       }
	       else //otherwise just forward and log out user
	       {
		  logOutUser(p.GetSource());
		  reTransmitPacket(p);
		  output = "\n\n" + String.valueOf(p.GetSource()) + " has just logged out\n";
	       }
               break;
            }
         default:
            output = "\nUnknown packet type received";
            break;
      }
      return output;
   }

   private String handleProxyLogOutPacket(Packet p)
   {
      String retString = "";

      PendingTable.PendingTableEntry pt = Global.GetPendTableEntry(p.GetSource());
      boolean removePacket = pt.HasPacket() && pt.getPacket().GetPayload().equals(Global.PROXY_LOGOUT_MESSAGE);

      //this is a proxy packet, if it was locally sent then remove
      if (removePacket)
      {
         pt.HaltReTransmission();
         pt.RemovePacket();
      }
      else
      {
         Global.gMessageTransmitter.sendPacket(p);
         retString = ("\n\nProxy logout received for " + Character.toString(p.GetDestination())) + "\n";
         if (Global.gDebugMode)
         {
            retString += "\nForwarded proxy packet on\n";
         }
      }
      logOutUser(p.GetSource());

      return retString;
   }

   private void ACKReceivedUpdatePendingPackets(Packet p)
   {

      Global.gPendingTable.RemovePacket(p.GetSource());
   }

   private void reTransmitPacket(Packet p)
   {
      //retransmit
      Global.gMessageTransmitter.sendPacket(p);
   }

   private boolean loginUser(Packet p)
   {

      return Global.gPendingTable.LogInUser(p.GetSource());
   }

   private boolean loginUser(char c)
   {
      if (!Global.GetPendTableEntry(c).isLoggedIn())
      {
         Global.gPendingTable.LogInUser(c);
         return true;
      }
      else
      {
         return false;
      }
   }

   private boolean logOutUser(char c)
   {
      //X-logout byuser
      if (Global.GetPendTableEntry(c).isLoggedIn())
      {
         Global.gPendingTable.LogOutUser(c);
         return true;
      }
      else
      {
         return false;
      }
   }

   private String reTransmitPendingTableEntry(Packet p)
   {

      PendingTable.PendingTableEntry pt = Global.GetPendTableEntry(p.GetDestination());
      return pt.getPacketTimeOut().AttemptTransmit();
   }

   private void returnACK(Packet p)
   {
      Packet r = new Packet();
      r.SetDestination(p.GetSource());
      r.SetSource(Global.gUserID);
      r.SetType(Global.PacketType.ACKNOWLEDGE);
      r.SetPayload(p.GetPayload());
      r.CalculateAndSetCheckSum();
      Global.gMessageTransmitter.sendPacket(r);
   }

   private void returnRACKPacket(Packet p)
   {
      Packet r = new Packet();
      r.SetDestination(p.GetSource());
      r.SetSource(Global.gUserID);
      r.SetType(Global.PacketType.LOGIN_RESPONSE);
      r.SetPayload(p.GetPayload());
      r.CalculateAndSetCheckSum();
      Global.gMessageTransmitter.sendPacket(r);
   }



}
