package csa;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MenuLoop implements Runnable {

    private boolean alive = true;

    private boolean blnWaitRequested;

    public boolean GetNeedToWait() {
        return blnWaitRequested;
    }

    public void killThread() {
        alive = false;
    }

    public void SetNeedToWait() {
        blnWaitRequested = true;
    }


    public MenuLoop() {
    }

    public void showMenu() {

        DisplayListOfUsers();
        String message = "\n" + Global.LINE + "Q : Quit\n" + "^D: Debug\n";
        if (Global.gLoggedIn) {
            message += "L : Logout\n" + "D : Enter UserID to send message to\n";
        } else {
            message += "L : Login\n";
        }

        message += "\nDebug mode : ";
        if (Global.gDebugMode) {
            message += "ON.\n";
        } else {
            message += "OFF.\n";
        }
        message += Global.LINE;
        message += "Please enter choice: \n";
        Global.gUserIO.print(message);
    } //showmenu

    public void run() {
        char inputChar;
        showMenu();
        while (alive) {

            if (blnWaitRequested) 
	    {
            try
            {

               java.lang.Thread.sleep(csa.Global.THREAD_TIME_OUT_LONG);
            }
            catch (InterruptedException ex)
            {
               Logger.getLogger("global").log(Level.SEVERE, null, ex);
            }
            }
	    
	    inputChar = Global.gUserIO.getChar();
	    if (Character.isLetter(inputChar))
	       inputChar = Character.toUpperCase(inputChar);

	    
            switch (inputChar) {
                case 'L':
                    //SELECTED LOGIN /OUT
                    {
                        if (!csa.Global.gLoggedIn) {
                            login();
                        } 
			else 
			{
                            Global.gUserIO.print(logout());
                        }
                        break;
                    }
                case 'H':
                    //SELECTED Help
                    {
                        showMenu();
                        break;
                    }
                case 'Q':
                    //SELECTED QUIT
                    {
                        exitChat();
                        break;
                    }
                case 'D':
                    //SEND MESSAGE
                    {
                        SendMessage();
                        break;
                    }
                case 4://Toggle Debug (4 = CTRL D)
	        case 'B':
                    {
                        ToggleDebugMode();
                        break;
                    }
                default:
                    {
                        Global.gUserIO.print(Global.LINE+"Invalid choice!"+Global.LINE);
                        break;
                    }
            }//switch
	
	try
            {

               java.lang.Thread.sleep(csa.Global.THREAD_TIME_OUT_LONG);
               // Reset wait flag after wait has happened.
               blnWaitRequested = false;
            }
            catch (InterruptedException ex)
            {
               Logger.getLogger("global").log(Level.SEVERE, null, ex);
            }
        }//while
    } //GoIntoMenuloop

    public void exitChat() {
      try
      {
	 if (csa.Global.gLoggedIn)
         {
	    csa.Packet p = new csa.Packet();
	    p.SetType(csa.Global.PacketType.LOG_OUT);
	    p.SetSource(csa.Global.gUserID);
	    p.SetDestination(csa.Global.gUserID);
	    p.setRandomStamp();
	    p.CalculateAndSetCheckSum();
	    csa.Global.gMessageTransmitter.sendPacket(p);
 
            LogOutUser(csa.Global.gUserID);
            csa.Global.gLoggedIn = false;
            csa.Global.gUserID = csa.Global.NULL_LOGIN_ID;
	    //this is to make sure the message gets chance to be sent
	    boolean shouldWait = true;
	    while (shouldWait)
	    {
	       int attempts=0;
	       Thread.sleep(csa.Global.THREAD_TIME_OUT_LONG);	    

	       if (Global.gLoggedIn)
		  shouldWait=true;
	       else {
		  shouldWait = false;
		  Global.gUserIO.print("\n\nLogout successful, exiting...");
	       }
	       attempts++;
	       if( attempts > 5){
		  shouldWait=false;
		  csa.Global.gUserIO.print("\n\nLogout unsuccessful, exiting anyway...");
	       }
	    }
	 }
	 csa.Global.gUserIO.print("\nGoodbye!");
	 Thread.sleep(csa.Global.THREAD_TIME_OUT_LONG);
         System.exit(0);
      }
      catch (InterruptedException ex)
      {
         Logger.getLogger("global").log(Level.SEVERE, null, ex);
      }
    } //exitChat

    public void sendPacket(Packet p) {

        if (Global.gDebugMode)
	      Global.gUserIO.print("\n\nSending Packet...");
        Global.gMessageTransmitter.sendPacket(p);
    } //send Packet

    private void LogOutUser(char pUserID) {

        Packet p = new Packet();
        p.SetType(Global.PacketType.LOG_OUT);
        p.SetSource(pUserID);
        p.SetDestination(pUserID);
        p.setRandomStamp();
        p.CalculateAndSetCheckSum();


        PendingTable.PendingTableEntry pt = Global.gPendingTable.GetPendTableEntry(pUserID);

        if (pt.isLoggedIn()) {
            Global.gUserIO.print("\n\nLogging out ... ");
        } else {
            Global.gUserIO.print("\n\nUser is already logged out");
        }
        Global.gPendingTable.LogOutUser(pUserID);

        try {
            Global.gMessageTransmitter.sendPacket(p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void ToggleDebugMode() {

        Global.gDebugMode = !Global.gDebugMode;

        String message = "\nDebug mode is now ";
        if (Global.gDebugMode) {
            message += "ON\n";
            Global.gUserIO.print(message);
        } else {
            message += "OFF\n";
            Global.gUserIO.print(message);
        }
    }

    private void SendMessage() {

        if (Global.gLoggedIn) 
	{
            DisplayListOfUsers();

            // Get login ID of destination
            Global.gUserIO.print("\n\nEnter ID you wish to send to:\n");

            Packet p = getMessageInputToSend();
          
	   
	       if (p.GetSource() != Global.NULL_LOGIN_ID) 
	       {
		  Global.gUserIO.print("\n"+Global.LINE+"\nPacket to send:\n");
		  Global.gUserIO.print(p.toString()+Global.LINE+"\nPress 'S' to send:\n");
		  char c = Global.gUserIO.getChar();
		  if (Character.isLetter(c))
		     c=Character.toUpperCase(c);

		  if (c!='S')
		  {
		     Global.gUserIO.print(Global.LINE+"\nNot sent.\n");
		  }
		  
		  else //send the message
		  {
		     char destination = p.GetDestination();
		      PendingTable.PendingTableEntry pt = Global.GetPendTableEntry(destination);
		      pt.setPacket(p);

		      sendPacket(p);


		      /* Now we create and add the PacketTimeOut object to the
		      correct pendtable entry so we can get to it
		      later for manual retransmission of packet
		      i.e. if NAK received
		       */
		      PacketTimeOut packetTimeOut = new PacketTimeOut(Global.TRANSMISSION_ATTEMPTS, p);
		      pt.setPacketTimeOut(packetTimeOut);
		      packetTimeOut.startTimer();
		  }
		  } 
		  else 
		  {
		      Global.gUserIO.print("\n\nInvalid user ID - choose another\n" + csa.Global.LINE);
		      DisplayListOfUsers();
		  }
	    
        } 
	else 
	{
            Global.gUserIO.print("\n\nInvalid option\n" + csa.Global.LINE);
        }
    }


    private void login() {

        char loginID;
        PendingTable.PendingTableEntry pt = null;
        Packet p = new Packet();
        DisplayListOfUsers();

        Global.gUserIO.print("\n\nChoose your login ID from A-Z: \n"+Global.LINE);


        loginID = Global.gUserIO.getChar();


        if (Character.isLetter(loginID)) {
            loginID = Character.toUpperCase(loginID);
            pt = Global.gPendingTable.GetPendTableEntry(loginID);
        } else {
            Global.gUserIO.print("\n\nYou entered an invalid login ID\nType 'H' for help\n"+Global.LINE);
            
            return;
        }

        if (pt != null) {
            if (pt.isLoggedIn()) {
                Global.gUserIO.print("\n\nUser is already logged in\n");
                return;
            } else 
	    {
                // Login user
                // Send login packet
                p.SetType(Global.PacketType.LOGIN);
                p.SetSource(loginID);
                p.SetDestination(loginID);

                p.setRandomStamp();
                p.CalculateAndSetCheckSum();

                /* add the entry so we know to check
		  for a return login packet
		 if there's already a login packet queuing
		 disallow sending
		 
		*/
                if (Global.gPendingTable.AddPacket(p) 
		      || (
			   Global.gMyRequestedLoginID
			   != Global.NULL_LOGIN_ID
			 )
		   )
		{
		 /*	
		     This is set so that the incoming packet hub can 
		     be aware of loginID to check for
		 */
		   Global.gMyRequestedLoginID=loginID;

		   sendPacket(p);

		   PacketTimeOut packetTimeOut = new PacketTimeOut(Global.TRANSMISSION_ATTEMPTS, p);
		   pt.setPacketTimeOut(packetTimeOut);

		   Global.gUserIO.print("\n\nRequested Login\n");
		}
		else //there is already a login requested
		{
		  Global.gUserIO.print("\n\nLogin already requested\n");
		}
            }
        }
    } //login

    private void DisplayListOfUsers() {
        String list;

        list = Global.gPendingTable.GetListOfLoggedInUsers();

        if (list.length() > 0) {
            Global.gUserIO.print("\n" + Global.LINE + "Users now logged in are:- \n\n");

            if (list.length() > 1) {
                for (int x = 0; x < list.length(); x++) {
		   String id = list.substring(x, x+1);
		   // check not last character
		    if (x < list.length() - 1) {
		       
                        //if this is local user ID
			if (Global.gUserID == id.charAt(0)){
			   Global.gUserIO.print(id + " - (YOU), ");
			}
			else
			   Global.gUserIO.print(id + ", ");
                    } else 
		    {
                        //print last character
			if (Global.gUserID == id.charAt(0))
			{
			   Global.gUserIO.print(id + " - (YOU)");
			}
			else
                        Global.gUserIO.print(list.substring(x, x + 1) + "\n");
                    }
                }
            } else {
                //print single character
                if (Global.gLoggedIn) {
                    Global.gUserIO.print("Just you: " + list + "\n" + Global.LINE);
                } else {
                    Global.gUserIO.print("Just one user: " + list + "\n" + Global.LINE);
                }
            }
        } else {
            Global.gUserIO.print("\n"+ Global.LINE+"Noone is logged in.\n"+ Global.LINE);
        }
    } //DisplayListOfUsers

    private String logout() {
      String output="";
        PendingTable.PendingTableEntry pt = Global.GetLocalUserPendTableEntry();
        if (pt.isLoggedIn()) {
            output=("\n\nLogging out ... \n");
        } else {
            output=("\n\nUser is already logged out\n");
        }
        Global.gPendingTable.LogOutUser(Global.gUserID);

        Packet p = new Packet();

        p.SetType(Global.PacketType.LOG_OUT);
        p.SetSource(Global.gUserID);
        p.SetDestination(Global.gUserID);
        p.setRandomStamp();
        p.CalculateAndSetCheckSum();
	
	/* this global packet is used to check incoming logout
	   packets to see if they should be dropped.
	   otherwise they would keep bouncing round the
	   loop
	*/
	Global.gMyLogOutPacket = p;
	
        try {
            sendPacket(p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	PacketTimeOut pTimeOut = 
	      new PacketTimeOut(Global.TRANSMISSION_ATTEMPTS,Global.gMyLogOutPacket);
	pt.setPacketTimeOut(pTimeOut);
	
        //Logout performed reset values
        Global.gUserID = Global.NULL_LOGIN_ID;
        Global.gLoggedIn = false;
        return output;
    } //logout

    private Packet getMessageInputToSend() {
        // Note returns a packet
        // If source is = NULL then the requested userID doesn't exist
        char DestinationID = Global.NULL_LOGIN_ID;
        PendingTable.PendingTableEntry pt = null;
        Packet p = new Packet();


        DestinationID = Global.gUserIO.getChar();
        //validate DestinationID
        if (Character.isLetter(DestinationID)) {
            DestinationID = Character.toUpperCase(DestinationID);
        } else {
            p.SetSource(Global.NULL_LOGIN_ID);
            return p;
        }

        //if user not logged in then don't send a packet
        if (!Global.gPendingTable.IsUserLoggedIn(DestinationID)) {
            p.SetSource(Global.NULL_LOGIN_ID);
            return p;
        }

        // Get Message
        Global.gUserIO.print(Global.LINE);
        Global.gUserIO.print("\n\nPlease enter your message (up to " + String.valueOf(Global.PAYLOAD_LENGTH) + " characters): \n");

        char[] inputCharArray = Global.gUserIO.getCharArray(Global.PAYLOAD_LENGTH);

        // Prepare packet
        p.SetDestination(DestinationID); // Set destination
        p.SetSource(Global.gUserID); // Set source to be the user logged in at this terminal
        p.SetType(Global.PacketType.DATA_PAYLOAD); //Data payload
        p.SetPayload(inputCharArray); //overloaded method handles char array too
        p.CalculateAndSetCheckSum();


        return p;
    } //getMessageInputToSend
}
