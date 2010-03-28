

package csa;

public class ChatApplication implements Runnable
{

   Thread t;

   private static String[] pArgs;

   public static void main(String[] args)
   {
      new ChatApplication(args);
   } //main

   public ChatApplication(String[] args)
   {
      pArgs = args;
      t = new Thread(this);
      t.start();
   } //ChatApplication

   public void run()
   {
      //create pending table
      Global.gPendingTable = new PendingTable();

      //set up input thread
      Global.gUserIO = SetUpUserIOSystem();

      Global.gSerialIO = SetUpConnection();

      //set up other threads
      Global.gMessageTransmitter = SetupTransmitter();
      Global.gIncomingPacketHub = SetUpReceiver();
      Global.gMenuLoop = SetUpMenuLoop();
   }

   private UserIOSystem SetUpUserIOSystem()
   {
      UserIOSystem r;
      //Set up new UserIO
     
      r = new UserIOSystem();
      return r;
   }

   public MessageTransmitter SetupTransmitter()
   {

      MessageTransmitter r;
      //Set up new Transmitter
      try
      {
         r = new MessageTransmitter(); // Pass instance of this application so it has access
         // to global variables through getter methods like debug mode etc
         Global.gThreadMsgTransmitter = new Thread(r);
         Global.gThreadMsgTransmitter.setName("Transmitter_Thread");
         Global.gThreadMsgTransmitter.start();
         return r;
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         return null;
      }
   }

   public IncomingPacketHub SetUpReceiver()
   {
      //Set up new Receiver
      IncomingPacketHub r;

      //ChatApplication pChatApplication,  char  pMainUserID,
      // boolean pDebugMode, int iPacketSize
      r = new IncomingPacketHub();
      Global.gThreadMsgReceiver = new Thread(r);
      Global.gThreadMsgReceiver.setName("Receiver_Thread");
      Global.gThreadMsgReceiver.start();
      return r;
   }

   public MenuLoop SetUpMenuLoop()
   {
      //Set up new Receiver
      MenuLoop m;


      m = new MenuLoop();
      Global.gThreadMenuLoop = new Thread(m);
      Global.gThreadMenuLoop.setName("Menu_Loop");
      Global.gThreadMenuLoop.start();
      return m;
   }

   private SerialIO SetUpConnection()
   {
      SerialIO s;
      String outputMessage;
      s = new SerialIO();
      boolean portSetOK = false;
      char inputChar;

      while (portSetOK == false)
      {

         // This iterates through ports seeing if a port
         // matches the first command line argument
         String strPortId = s.getNextPortName();
         if (strPortId.equals("No valid ports left"))
         {
            break;
         }
         if (pArgs.length > 0)
         {
            if (pArgs[0].equals(strPortId))
            {
               if (s.setPort(strPortId).equals("1"))
               {
                  portSetOK = true;
                  return s;
               }
            }
         }
      }

      s.resetPortList();
      while (portSetOK == false)
      {

         String strPortId = s.getNextPortName();
         String message;

         if (!strPortId.equals("No valid ports left"))
         {

            message = Global.LINE + "Found COM Port: " + strPortId + Global.LINE;
            message += "Do you want to use this COM Port? (Y to select): ";
            Global.gUserIO.print(message);

            inputChar = Global.gUserIO.getChar();
            if (Character.isLetter(inputChar))
	       inputChar = Character.toUpperCase(inputChar);

            if (inputChar == 'Y')
            {
               outputMessage = s.setPort(strPortId);
               if (outputMessage.equals("1"))
               {
                  portSetOK = true;
               }
               else
               {
                  Global.gUserIO.print(outputMessage);
               }
            }
            else
            {
               portSetOK = false;
            }
         }
         else
         {
            message = strPortId + Global.LINE + "Do you want to go back to the start of the list? (Y OR N): ";
            Global.gUserIO.print(message);

            inputChar = Global.gUserIO.getChar();
            if (Character.isLetter(inputChar))
	       inputChar = Character.toUpperCase(inputChar);

            if (inputChar == 'Y')
            {
               s.resetPortList();
            }
            else
            {
               Global.gMenuLoop.exitChat();
            }
         }
      }
      return s;
   } //SetUpConnection
} //ChatApplication
