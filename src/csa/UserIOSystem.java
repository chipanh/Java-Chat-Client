package csa;

import java.net.*;
import java.io.*;
//import java.io.Console;
import java.util.*;

public class UserIOSystem
{

   private int intIO = -1;
   private Scanner sc;

 
   public void print(String printMessage)
   {

      if (intIO == 0)
      {
         System.out.print(printMessage);
      }
   }

   public UserIOSystem()
   {
      intIO = 0;
      sc = new Scanner(System.in);
   }

   private String getString(int pLength)
   {
      String inputString="";
      boolean blnDone = false;
      while (!blnDone)
      {
	 String temp = sc.nextLine();
	 inputString += temp;
	 if (inputString.length()>0)
	    blnDone=true;
	  
      }
      if (inputString.length()>pLength)
      {
	 inputString = inputString.substring(0, pLength);
      }
      else
      {
	 while(inputString.length()<pLength)
	 {
	    inputString+=" ";
	 }
      }    
      return inputString;
   }

   public char getChar()
   {
      char a = Global.NULL_LOGIN_ID;
      String str = "";

      while (a == Global.NULL_LOGIN_ID)
      {
	 str = sc.next();
	 if (str.length() > 0)
	 {
	    a = str.charAt(0);
	 }
      }
      
       return a;
   }

   public char[] getCharArray(int pLength)
   {
       //Warning this truncates long strings
      String inputString = getString(pLength);

      //deals with short input strings
      if (inputString.length() < pLength)
      {
         pLength = inputString.length();
      }
      char[] retCharArray = new char[pLength];
      
      //Truncates long strings to length of pLength
      for (int x = 0; x < pLength; x++)
      {
         retCharArray[x] = inputString.charAt(x);
      }
      
      return retCharArray;
     
   }

 

}
