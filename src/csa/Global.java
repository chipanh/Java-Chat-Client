/*
 * Global.java
 */

package csa;

/**
 *
 * @author mrk
 */
public class Global
{

   public enum UserIOState
   {

      DOING_NOTHING, GETTING_STRING, GETTING_CHAR, GETTING_FINITE_STRING, GETTING_CHAR_ARRAY
   }

   public enum PacketType
   {

      LOGIN ('L'), 
      LOG_OUT ('X'), 
      LOGIN_RESPONSE ('R'), 
      DATA_PAYLOAD ('D'), 
      ACKNOWLEDGE ('Y'), 
      NON_ACKNOWLEDGE ('N'),
      NULL ('0');

      PacketType getPacketTypeFromChar(char c){
	 switch(c){
	 case 'L':
	    return PacketType.LOGIN;
	 case 'X':
	    return PacketType.LOG_OUT;
	 case 'R':
	    return PacketType.LOGIN_RESPONSE;
	 case 'D':
	    return PacketType.DATA_PAYLOAD;
	 case 'Y':
	    return PacketType.ACKNOWLEDGE;
	 case 'N':
	    return PacketType.NON_ACKNOWLEDGE;
	  
	 case '0':
	 default:
	    return PacketType.NULL;
	  
	 }
      }
      char GetCharPacketType()
      {
         return mPacketType;
      }
      PacketType(char pPacketType)
      {
         this.mPacketType = pPacketType;
      }
      private char mPacketType;
   }
  
   public static final char NULL_LOGIN_ID ='0';
   public static final int TRANSMISSION_ATTEMPTS = 4;
   public static final int THREAD_TIME_OUT = 500;
   public static final int THREAD_TIME_OUT_LONG = 2150;
   public static final int PACKET_TIMEOUT_DELAY = 6000; //ms until attempt retransmission
   public static final int PACKET_SIZE = 16;
   public static final int START_OF_PAYLOAD = 4; // "{DST" then payload
   public static final int PAYLOAD_LENGTH = 10;
   public static final int EIGHT_BIT_ALL_THE_ONES = 255;
   public static final int BINARY_SEVEN_ONES = 127;
   public static final int TOTAL_NUMBER_OF_USERS = 26; //  26 for A to Z
   public static final int UNICODE_LETTER_A = 65;
   public static final String LINE = "\n--------------------------------------------------\n";
   public static final String PROXY_LOGOUT_MESSAGE = "PRXY_LGOUT";
   
   public static Packet gMyLogOutPacket;
   public static boolean gLoggedIn = false;
   public static boolean gDebugMode = true;
   
   public static char gMyRequestedLoginID = Global.NULL_LOGIN_ID;
   public static char gUserID = NULL_LOGIN_ID; //id of user logged in at this terminal
   public static csa.PendingTable gPendingTable;
   public static csa.UserIOSystem gUserIO;
   public static csa.SerialIO gSerialIO = null; //Class that actually communicates with COM port
   public static csa.MenuLoop gMenuLoop;
   public static csa.IncomingPacketHub gIncomingPacketHub;
   public static csa.MessageTransmitter gMessageTransmitter;
   public static Thread gThreadMsgReceiver;
   public static Thread gThreadMsgTransmitter;
   public static Thread gThreadMenuLoop;
   public static Thread gThreadUserIOSystem;

   public static void haltTransmission(char pUserID)
   {
      gPendingTable.haltTranmission(pUserID);
   }
   public static void haltTransmissionsAddProxyPacketAndSend(Packet p)
   {
      char c = p.GetDestination();
      haltTransmission(c);
      PendingTable.PendingTableEntry ptEntry = Global.GetPendTableEntry(c);
      if (ptEntry.HasPacket())
      {
	 ptEntry.RemovePacket();
      }
      ptEntry.setPacket(p);
      Global.gMessageTransmitter.sendPacket(p);
   }
   public static Packet createProxyLogout (char c)
   {
      Packet r = new Packet();
      r.SetDestination(c);
      r.SetSource(c);
      r.SetType(Global.PacketType.LOG_OUT);
      r.SetPayload(Global.PROXY_LOGOUT_MESSAGE);
      r.CalculateAndSetCheckSum();
      Global.gMessageTransmitter.sendPacket(r);
      return r;
   }
   public static csa.PendingTable.PendingTableEntry GetLocalUserPendTableEntry()
   {
      return gPendingTable.GetPendTableEntry(gUserID);
   }

   public static csa.PendingTable.PendingTableEntry GetPendTableEntry(char pUserID)
   {
      return gPendingTable.GetPendTableEntry(pUserID);
   }

 
  
   public Global()
   {
   }
}
