/*
 * Packet.java
 */

package csa;

import java.math.*;

public class Packet
{

   private char mDestination;
   private char mSource;
//    private char mType;
   private Global.PacketType mType;
   private String mPayload;
   private byte mChecksum;
   private String randomStamp;

   public Packet()
   {

      mDestination = Global.NULL_LOGIN_ID;
      mSource = Global.NULL_LOGIN_ID;
      mType = Global.PacketType.NULL;
      mPayload = "          ";
      mChecksum = 0;
   }

   /**
    *
    * @return
    */
   @Override
   public String toString()
   {

      //{|D|S|T|<----- Payload ------> | CS | }
      String retString = "{";

      retString += this.mDestination;
      retString += String.valueOf(mSource);
      retString += String.valueOf(mType.GetCharPacketType());
      retString += mPayload;
      retString += " Checksum:" + String.valueOf(mChecksum) + " ";
      retString += "}";
      return retString;
   }

   public void SetSource(char pSource)
   {
      mSource = pSource;
   }

   public void SetDestination(char pDestination)
   {
      mDestination = pDestination;
   }

   
   public void SetType(Global.PacketType pType)
   {

      mType = pType;
   }
   
   public void SetType(char pType)
   {
      mType = mType.getPacketTypeFromChar(pType);
   }
   public boolean SetPayload(String pPayload)
   {
//     {   D   S   T   f   o   u   r   -   f     o    u    r    !     C   }
//     00  01  02  03  04  05  06  07  08  09    10   11   12   13   14   15
//     1st 2nd 3rd 4th 5th 6th 7th 8th 9th 10th 11th 12th 13th 14th 15th 16th
      //fill with spaces
      if (pPayload.length() < Global.PAYLOAD_LENGTH)
      {
         int i = Global.PAYLOAD_LENGTH - pPayload.length();
         for (int count = 2; count <= i; count++)
         {
            pPayload = pPayload + " ";
         }

         mPayload = pPayload;
         return true;
      }
      else
      {
         //truncate
         mPayload = pPayload.substring(0, Global.PAYLOAD_LENGTH);
      }
      return false;
   }

   public boolean SetPayload(char[] charArrayPayload)
   {

      if (SetPayload(String.valueOf(charArrayPayload)))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   public void ManuallySetCheckSum(byte pCheckSum)
   {
      this.mChecksum = pCheckSum;
   }

   public void CalculateAndSetCheckSum()
   {
      this.mChecksum = this.CalculateButNotSetCorrectCheckSum();
   }

   public char GetSource()
   {
      return mSource;
   }

   public char GetDestination()
   {
      return mDestination;
   }

   public Global.PacketType GetType()
   {
      return mType;
   }

   public char GetTypeAsChar()
   {
      return mType.GetCharPacketType();
   }
   public String GetPayload()
   {

      if (mPayload.length() < Global.PAYLOAD_LENGTH)
      {
         int i = Global.PAYLOAD_LENGTH - mPayload.length();
         for (int count = 0; count < i; count++)
         {
            mPayload = mPayload + " ";
         }
      }
      return mPayload;
   }

   public boolean IsCheckSumOK()
   {

      if (mChecksum == CalculateButNotSetCorrectCheckSum())
      {
         return true;
      }
      else
      {
         return false;
      }
   }


   public byte CalculateButNotSetCorrectCheckSum()
   {

      /*the inverted modulo-128 sum of all the other bytes in the packet
        convert to int, sum each byte
        take mod 128 (i.e. make first bit 0)
        invert bits
        size = Dest Source Type + payload(10 bytes)
       */
      int firstByteOfPayload = Global.START_OF_PAYLOAD; //starting point of payload
      int sizeOfDataBytesExcludingCheckSum = firstByteOfPayload + mPayload.length();

      byte[] byteArray = new byte[sizeOfDataBytesExcludingCheckSum];
      int i = 0;
      byte b = 0;

      int x = 0;
      byteArray[0] = (byte) mDestination;
      byteArray[1] = (byte) mSource;
      byteArray[2] = (byte) mType.GetCharPacketType();

      //counts through payload adding each char to byte array
      for (x = firstByteOfPayload; x < sizeOfDataBytesExcludingCheckSum; x++)
      {
         byteArray[x] = (byte) mPayload.charAt(x-firstByteOfPayload);
      }
      // calculates integer sum
      for (x = 0; x < sizeOfDataBytesExcludingCheckSum; x++)
      {
         i += byteArray[x];
      }
      // 0x01111111 = 127 if you and this you got modulo128
      b = (byte) i;
      b ^= Global.EIGHT_BIT_ALL_THE_ONES; //invert bits
      b &= Global.BINARY_SEVEN_ONES; //modulo 128
      return b;
   }

   public byte getChecksum()
   {
      return mChecksum;
   }

   public String getRandomStamp()
   {

      return randomStamp;
   }

//     {   D   S   T   f   o   u   r   -   f     o    u    r    !     C   }
//     00  01  02  03  04  05  06  07  08  09    10   11   12   13   14   15
//     1st 2nd 3rd 4th 5th 6th 7th 8th 9th 10th 11th 12th 13th 14th 15th 16th
   public String setRandomStamp()
   {
      String temp = String.valueOf(java.lang.Math.random());
      //strip off and ignore first 2 digits i.e. "0."
      randomStamp = temp.substring(2, Global.PAYLOAD_LENGTH + 2);
      this.mPayload = randomStamp;
      return mPayload;
   }
}