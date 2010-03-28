/*
 * PendingTable.java
 *
 * Created on March 9, 2007, 6:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package csa;

import java.util.*;

public class PendingTable{
    private static List<PendingTableEntry> mListPendingTable;
    
    
    private String strListPacketsToResend;
    private String strListLoggedInUsers;
    
    public PendingTable() {
        
        Initialize();
    }
    

    private void Initialize(){
       
        int iNumberOfUsers=Global.TOTAL_NUMBER_OF_USERS;  
        int iUnicodeValue = Global.UNICODE_LETTER_A; 
        mListPendingTable = new ArrayList<PendingTableEntry>(iNumberOfUsers);
        strListLoggedInUsers="";
        strListPacketsToResend="";
        char cUser = ' ';
        
        int count =0;
        
        for (count=0; count < iNumberOfUsers; count++) {
            PendingTable.PendingTableEntry ptEntry = new PendingTable.PendingTableEntry();
            cUser = (char)iUnicodeValue;
            ptEntry.setUserID(cUser);//set user ID to A to Z
            mListPendingTable.add(ptEntry);//add user to list
            iUnicodeValue++;
        }
    }
    
    public boolean IsUserLoggedIn(char pUserID){
        if ((strListLoggedInUsers.indexOf(String.valueOf(pUserID)))!= -1)
            return true;
        else
            return false;
        
    }
    public Packet GetPacket(char pUserID){
        PendingTable.PendingTableEntry ptEntry = GetPendTableEntry(pUserID);
        return ptEntry.getPacket();
        
    }
    public String GetListOfLoggedInUsers(){
        return strListLoggedInUsers;
        
    
        
    }
    
    public synchronized boolean LogOutUser(char pUserID){
        
        PendingTable.PendingTableEntry ptEntry = GetPendTableEntry(pUserID);
        
        
        //Remove from user from logged in users String
        if (strListLoggedInUsers.contains(String.valueOf(pUserID))){
            strListLoggedInUsers = strListLoggedInUsers.replace(String.valueOf(pUserID), "");
        }
        if (ptEntry.isLoggedIn()){
            ptEntry.setLoggedIn(false);
            return true; //user logged out succesfully
        }
        
        else
            return false; // user is logged in already
    }
    
    private int GetIndexOfUserID(char pUserID){
        
        /*ListIterator pointer = mListPendingTable.listIterator();
        PendingTable.PendingTableEntry currentEntry;
        
        while (pointer.hasNext()) {
            currentEntry =(PendingTable.PendingTableEntry)pointer.next();
            
            if (currentEntry.getUserID() == pUserID)
                return pointer.previousIndex();
            
        }
        return -1;
	 */
	return Integer.valueOf(pUserID)-Global.UNICODE_LETTER_A; 
    }
    
    public PendingTable.PendingTableEntry GetPendTableEntry(char pUserID){
        
        int index = GetIndexOfUserID(pUserID);
        
        return mListPendingTable.get(index);
    }
    
    public void clearData(){
        
        strListLoggedInUsers="";
        strListPacketsToResend="";
        
        ListIterator pointer = mListPendingTable.listIterator();
        PendingTable.PendingTableEntry currentEntry;
     
        while (pointer.hasNext()) {
            currentEntry =(PendingTable.PendingTableEntry)pointer.next();
            
            currentEntry.RemovePacket();
            currentEntry.setLoggedIn(false);
            
            
        }
        
    }
    
    public synchronized boolean LogInUser(char pUserID){
        
        PendingTable.PendingTableEntry ptEntry = GetPendTableEntry(pUserID);
        
        if (ptEntry.isLoggedIn())
            return false; // user is logged in already
        else {
            ptEntry.setLoggedIn(true);
            //add to short string list of logged in users
            if  ((strListLoggedInUsers.lastIndexOf(pUserID))== -1)
                strListLoggedInUsers += String.valueOf(pUserID);
            return true; //user logged in succesfully
        }
        
        
    }
    
    public synchronized boolean AddPacket(Packet pPacket){
        char cDestination = pPacket.GetDestination();
        PendingTable.PendingTableEntry ptEntry = GetPendTableEntry(cDestination);
        // if not on list then add it
        
        if (ptEntry.HasPacket()){
            return false;
        } 
	else 
	{
            ptEntry.setPacket(pPacket);
            if  ((strListPacketsToResend.lastIndexOf(cDestination))== -1)
                strListPacketsToResend += String.valueOf(pPacket.GetDestination());
            
            return true;
        }
        
    }
  
    public synchronized void haltTranmission(char pUserID){
       PendingTableEntry pt = GetPendTableEntry(pUserID);
       pt.HaltReTransmission();
    }
    
    public synchronized boolean RemovePacket(char pUserID){
        
        PendingTable.PendingTableEntry ptEntry = GetPendTableEntry(pUserID);
        if  ((strListPacketsToResend.lastIndexOf(pUserID))== -1)
            strListPacketsToResend = strListPacketsToResend.replace(String.valueOf(pUserID), "");
        
        if (ptEntry.HasPacket())
	{
            ptEntry.RemovePacket();
            return true;
        } else {
            return false;
        }
        
    }
    
    public String getListPacketsToResend() {
        return strListPacketsToResend;
    }
    
    public class PendingTableEntry {
        
        private Packet mPacket;
        private char mUserID;
        private boolean mLoggedIn;
        private PacketTimeOut mPacketTimeOut;
        
        /**
         * Creates a new instance of PendingTable.PendingTableEntry
         * @return 
         */
	@Override
      public String toString(){
	 
	   String output = String.valueOf(mUserID) + "Logged in: " 
	      + String.valueOf(mLoggedIn) 
	      + "Transmission Attempts: ";
	 if (mPacketTimeOut!=null){
	    output+=String.valueOf(mPacketTimeOut.getCurrentTransmission())  ;
	 }
	 else 
	 {
	      output+="null";
	 }
	 
	   output+= "Packet Details"  + mPacket.toString();
	   return output;
      }
        public PendingTableEntry() {
            mLoggedIn =false;
            mUserID =Global.NULL_LOGIN_ID;
            mPacketTimeOut=null;
            mPacket = null;
            // no packet on initializing
        }
        
        public boolean HasPacket(){
            if (mPacket!=null)
                return true;
            else
                return false;
        }
        
        public Packet getPacket() {
            return mPacket;
        }
        
        public PacketTimeOut getPacketTimeOut() {
            return mPacketTimeOut;
        }
        public void setPacketTimeOut(PacketTimeOut pPacketTimeOut) {
            mPacketTimeOut = pPacketTimeOut;
        }
        
        public int getTransmissionAttempts() {
            if ( mPacketTimeOut!=null )
	       return mPacketTimeOut.getCurrentTransmission();
	    else 
	       return -1;
        }
        
        public char getUserID() {
            return mUserID;
        }
        
        public boolean isLoggedIn() {
            return mLoggedIn;
        }
        
        public synchronized void setLoggedIn(boolean pLoggedIn) {
            mLoggedIn = pLoggedIn;
        }
        
        public synchronized void setPacket(Packet pPacket) {
            mPacket = pPacket;
        }
        
	public synchronized void HaltReTransmission(){
	   RemovePacket();
	   mPacketTimeOut.cancel();
	}
	
        public synchronized void RemovePacket() {
            mPacket = null;
        }
        
                 
        public synchronized void setUserID(char pUserID) {
            mUserID = pUserID;
        }
        
        
        
    }
    
    
}
