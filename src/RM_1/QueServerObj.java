package RM_1;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;

import javax.jws.WebService;





public class QueServerObj implements ServerInterface, Runnable {
    private HashMap<String, HashMap> typeMap = new HashMap<String, HashMap>();
    private static HashMap<String, Integer> docMap = new HashMap<String, Integer>();
    private static HashMap<String, Integer> dentMap = new HashMap<String, Integer>();
    private static HashMap<String, Integer> surgMap = new HashMap<String, Integer>();
    private static HashMap<String, QueBookingDb> users = new HashMap<String, QueBookingDb>();
    private HashMap<String, String> adminUsers = new HashMap<String, String>();
    private DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private FileWriter file;
    private String adminID;
    private QueBookingDb patientID;
    
    
    
    
   

   
	public QueServerObj() {
    	docMap.put("QUEE100619", 2);
//        docMap.put("QUEA081220", 3);
//        docMap.put("QUEA050719", 3);
//        docMap.put("QUEA040619", 3);
//        docMap.put("QUEA111219", 3);
//        docMap.put("QUEA131219", 3);
//        docMap.put("QUEA151219", 3);
//        dentMap.put("QUEA010919", 3);
        dentMap.put("QUEE110619", 1);
        surgMap.put("QUEM041219", 3);
        adminUsers.put("QUEA1111", "");
        Thread t1 = new Thread(this);
        t1.setName("This is Que default thread");
        t1.start();
        this.patientID = null;
        try {
            file = new FileWriter("QueServer.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, QueBookingDb> getUsers() {
        return users;
    }

    public static void setUsers(HashMap<String, QueBookingDb> users) {
        QueServerObj.users = users;
    }

    public static HashMap<String, Integer> getDocMap() {
			return docMap;
		}

		public static void setDocMap(HashMap<String, Integer> map) {
			docMap = map;
		}

		public static HashMap<String, Integer> getDentMap() {
			return dentMap;
		}

		public static void setDentMap(HashMap<String, Integer> map) {
			dentMap = map;
		}

		public static HashMap<String, Integer> getSurgMap() {
			return surgMap;
		}

		public static void setSurgMap(HashMap<String, Integer> map) {
			surgMap = map;
		}

	 
		public HashMap<String, String> getAdminUsers() {
			return adminUsers;
		}

		public void setAdminUsers(HashMap<String, String> adminUsers) {
			this.adminUsers = adminUsers;
		}


    public void writelog(String req, String id, String appointmentID, String appointmentType, String suc, String res) {
        try {
            file.write("\nDate and time: " + date.format(LocalDateTime.now()) + "\nRequest type: " + req +
                    "\nRequest Parameters: " + id + " " + appointmentID + " " + appointmentType +
                    "\nRequest successful: " + suc + "\nServer response: " + res + "\n\n");
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized String bookAppointment(String patientID, String appointmentID, String appointmentType) {
        if (users.containsKey(patientID)) {
            this.patientID = users.get(patientID);
        }
        if (!users.containsKey(patientID)) {
            this.patientID = new QueBookingDb(patientID);
        }
        String patLoc= patientID.substring(0,3);
        if (isBookableExt(patLoc, appointmentID,appointmentType, patientID)) {

            if (appointmentType.equals("Physician")) {
                if (docMap.get(appointmentID) > 0) {
                    docMap.put(appointmentID, (docMap.get(appointmentID) - 1));
                    this.patientID.book(appointmentID, appointmentType);
                }
                else {
                	writelog("bookAppointment", patientID, appointmentID, appointmentType, "Failed", "Unable to book due to conflicting date and or time");
                	return ("Fail -Unable to book due to conflicting date and or time");
                }

            }
            if (appointmentType.equals("Dentist")) {
                if (dentMap.get(appointmentID) > 0) {
                    dentMap.put(appointmentID, (dentMap.get(appointmentID) - 1));
                    this.patientID.book(appointmentID, appointmentType);
                }
                else {
                	writelog("bookAppointment", patientID, appointmentID, appointmentType, "Failed", "Unable to book due to conflicting date and or time");
                	return ("Fail -Unable to book due to conflicting date and or time");
                }

            }
            if (appointmentType.equals("Surgeon")) {
                if (surgMap.get(appointmentID) > 0) {
                    surgMap.put(appointmentID, (surgMap.get(appointmentID) - 1));
                    this.patientID.book(appointmentID, appointmentType);
                }
                else {
                	writelog("bookAppointment", patientID, appointmentID, appointmentType, "Failed", "Unable to book due to conflicting date and or time");
                	return ("Fail -Unable to book due to conflicting date and or time");
                }

            }

            users.put(patientID, this.patientID);
            writelog("bookAppointment", patientID, appointmentID, appointmentType, "completed", "Success your appointment with: " + this.patientID.getAppMap().get(appointmentID) + " " + appointmentID + " was booked.");
            return ("Success your appointment with: " + this.patientID.getAppMap().get(appointmentID) + " " + appointmentID + " was booked.");

        }
        writelog("bookAppointment", patientID, appointmentID, appointmentType, "Failed", "Unable to book due to conflicting date and or time");
        return ("Fail -Unable to book due to conflictiong date and or time");

    }

    public String getAppointmentSchedule(String patientID){
        String out = "";
        if (!users.containsKey(patientID)) {
            return "You have no appointments booked";
        }
        this.patientID = users.get(patientID);
        if (this.patientID.getAppMap().isEmpty()) {
            return ("You have no appointments booked");
        }
        for (String key : this.patientID.getAppMap().keySet()) {
            out = out + this.patientID.getAppMap().get(key) + "-" + key + "\n";

        }
        writelog("getAppointmentSchedule", patientID, "", "", "completed", out);

        return out;
    }

    // client model will chose which server to sent too, ensures only do local ops
    public synchronized String cancelAppointment(String patientID, String appointmentID, String appointmentType){
        if (!users.containsKey(patientID)) {
            writelog("cancelAppointment", patientID, appointmentID, "", "Failed", "You have no appointments booked");
            return ("You have no appointments booked");
        }
        this.patientID = users.get(patientID);
        if (!this.patientID.getAppMap().containsKey(appointmentID)) {
            writelog("cancelAppointment", patientID, appointmentID, "", "Failed", "Could not find appointment, please ensure you have entered the appointment Id correctly");
            return ("Counld not find appointment, please ensure you have entered the appointment Id correctly");
        }

        if (this.patientID.getAppMap().containsKey(appointmentID)
                && this.patientID.getAppMap().get(appointmentID).equals("Physician")) {
            docMap.put(appointmentID, (docMap.get(appointmentID) + 1));
            this.patientID.getAppMap().remove(appointmentID);
        }
        if (this.patientID.getAppMap().containsKey(appointmentID)
                && this.patientID.getAppMap().get(appointmentID).equals("Dentist")) {
            dentMap.put(appointmentID, (dentMap.get(appointmentID) + 1));
            this.patientID.getAppMap().remove(appointmentID);
        }
        if (this.patientID.getAppMap().containsKey(appointmentID)
                && this.patientID.getAppMap().get(appointmentID).equals("Surgeon")) {
            surgMap.put(appointmentID, (surgMap.get(appointmentID) + 1));
            this.patientID.getAppMap().remove(appointmentID);
        }
        writelog("cancelAppointment", patientID, appointmentID, "", "Completed", "Appointment " + appointmentID + " has been canceled");
        return ("Appointment " + appointmentID + " has been canceled");
    }

    public String showSlots(String appointmentType) {
        // TODO Auto-generated method stub
        if (appointmentType.equals("Physician")) {
            return ("The following Physician appointments are available in Quebec:\n" + docMap.keySet());
        }
        if (appointmentType.equals("Dentist")) {
            return ("The following Dentist appointments are available in Quebec:\n" + dentMap.keySet());
        }
        if (appointmentType.equals("Surgeon")) {
            return ("The following Surgeon appointments are available in Quebec:\n" + surgMap.keySet());
        }
        return ("There are no appointments slots available in Quebec");
    }


//    public class QueBookingDb {
//        String patID;
//        private String loc;
//        private HashMap<String, String> appMap = new HashMap<String, String>();
//        //apID, type
//
//        QueBookingDb() {
//
//        }
//
//        public void setPatID(String patID) {
//            this.patID = patID;
//        }
//
//        public String getPatID() {
//            return patID;
//        }
//
//        public HashMap<String, String> getAppMap() {
//            return appMap;
//        }
//
//        public void setAppMap(HashMap<String, String> appMap) {
//            this.appMap = appMap;
//        }
//
//        QueBookingDb(String patID) {
//            this.patID = patID;
//            loc = patID.substring(0, 3);
//        }
//
//        private void setPID(String patID) {
//            this.patID = patID;
//        }
//
//        private void book(String ID, String type) {
//            appMap.put(ID, type);
//        }
//
//        private boolean isBookable(String ID, String type) {
//            if (appMap.isEmpty()) {
//                appMap.put(ID, type);
//                return true;
//            }
//
//            // same type of appointment on same day
//            if (!appMap.containsKey(ID)) {
//                String test = ID.substring(4, 8);
//                String temp;
//                String locTemp;
//                int count = 0;
//                double date = Double.valueOf("." + ID.substring(4, 8));
//                for (String key : appMap.keySet()) {
//                    String year = ID.substring(8);
//                    String month = ID.substring(6, 8);
//                    String day = ID.substring(4, 6);
//                    temp = key.substring(4, 8);
//                    locTemp = key.substring(0, 3);
//                    if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && appMap.get(key).equals(type)) {
//                        System.out.println("Can not book the same type of app in the same day");
//                        return false;
//                    }
//                }
//                //Same appointment at same time in other city
//                for (String key : appMap.keySet()) {
//                    String year = ID.substring(8);
//                    String month = ID.substring(6, 8);
//                    String day = ID.substring(4, 6);
//                    String time = ID.substring(3, 4);
//                    temp = key.substring(4, 8);
//                    locTemp = key.substring(0, 3);
//
//                    if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && key.substring(3, 4).equals(time)) {
//                        System.out.println("Have another appointment booked someplace else");
//                        return false;
//                    }
//                }
//
//                // 3 in a week check
//                for (String key : appMap.keySet()) {
//                    locTemp = key.substring(0, 3);
//
//                    if (!locTemp.equals(loc) && ID.substring(6).equals(key.substring(6))
//                            && ((Double.valueOf("." + key.substring(4, 8)) - date < 0.07)
//                            || (Double.valueOf("." + key.substring(4, 8)) - date > -0.07))) {
//                        count++;
//
//                    }
//                    if (count >= 3) {
//                        System.out.println("Can not book more than 3 INTERNAL CHECK! appointments in one week outside your home city");
//                        return false;
//                    }
//                }
//                // appMap.put(ID,type);
//                return true;
//            }
//            System.out.println("Unable to book");
//            return false;
//        }
//
//    }

    public synchronized String addAppointment(String appointmentID, String appointmentType, int capacity){
        String rply = "Unable to add appointment slot";
        if (appointmentType.equals("Physician")) {
            if (!docMap.containsKey(appointmentID)) {
                docMap.put(appointmentID, capacity);
                rply = appointmentID + " time slot was added";
            }

        }
        if (appointmentType.equals("Dentist")) {
            if (!dentMap.containsKey(appointmentID)) {
                dentMap.put(appointmentID, capacity);
                rply = appointmentID + " time slot was added";
            }
        }
        if (appointmentType.equals("Surgeon")) {
            if (!surgMap.containsKey(appointmentID)) {
                surgMap.put(appointmentID, capacity);
                rply = appointmentID + " time slot was added";
            }
        }
        if (rply.equals("Unable to add appointment slot")) {
            writelog("addAppointment", adminID, appointmentID, appointmentType, "Failed", rply);
        } else {
            writelog("addAppointment", adminID, appointmentID, appointmentType, "Completed", rply);
        }
        return rply;
    }


    //adfunctionality here to check if a patient has booked a splot here, see if moveable...
    public synchronized String removeAppointment(String appointmentID, String appointmentType){
        String rply = "Unable to remove appointment slot";
        if (appointmentType.equals("Physician")) {
            if (docMap.containsKey(appointmentID)) {
                docMap.remove(appointmentID);
                for (String userKey : users.keySet()) {
                    for (String appKey : users.get(userKey).getAppMap().keySet()) {
                        if (appKey.equals(appointmentID)) {
                            users.get(userKey).getAppMap().remove(appKey);
                        }
                    }
                }
                rply = appointmentID + " time slot was removed";
            }


        }
        if (appointmentType.equals("Dentist")) {
            if (dentMap.containsKey(appointmentID)) {
                dentMap.remove(appointmentID);
                for (String userKey : users.keySet()) {
                    for (String appKey : users.get(userKey).getAppMap().keySet()) {
                        if (appKey.equals(appointmentID)) {
                            users.get(userKey).getAppMap().remove(appKey);
                        }
                    }
                }
                rply = appointmentID + " time slot was removed";
            }

        }
        if (appointmentType.equals("Surgeon")) {
            if (surgMap.containsKey(appointmentID)) {
                surgMap.remove(appointmentID);
                for (String userKey : users.keySet()) {
                    for (String appKey : users.get(userKey).getAppMap().keySet()) {
                        if (appKey.equals(appointmentID)) {
                            users.get(userKey).getAppMap().remove(appKey);
                        }
                    }
                }
                rply = appointmentID + " time slot was removed";
            }

        }
        if (rply.equals("Unable to remove appointment slot")) {
            writelog("removeAppointment", adminID, appointmentID, appointmentType, "Failed", rply);
        } else {
            writelog("removeAppointment", adminID, appointmentID, appointmentType, "Completed", rply);
        }
        return rply;
    }

    public String listAppointmentAvailability(String appointmentType){
        String retMsg = "other";
        String build;
        if (appointmentType.equals("Physician")) {
            retMsg = ("#Available Physician appointments in Quebec:\n" + docMap.toString());
        }
        if (appointmentType.equals("Dentist")) {
            retMsg = ("#Available Dentist appointments in Quebec: \n" + dentMap.toString());
        }
        if (appointmentType.equals("Surgeon")) {
            retMsg = ("#Available Surgeon appointments in Quebec: \n" + surgMap.toString());
        }
        build = retMsg + "\n" + recvData(appointmentType);
        if (retMsg.equals("Could not retrieve")) {
            writelog("listAppointmentAvailability", adminID, "", appointmentType, "Failed", build);
        } else {
            writelog("listAppointmentAvailability", adminID, "", appointmentType, "Completed", build);
        }
        return (build);
    }

    public void run() {
        sendData();
    }

    public String cleanStr(String msg) {
        msg = msg.replaceAll("\\}", "");
        msg = msg.replaceAll("\\{", "");
        msg = msg.replaceAll("\\s+", "");
        msg = msg.replaceAll("\0", "");
        msg = msg.replaceAll("\\$", "");
        msg = msg.replaceAll("\\%", "");
        msg = msg.replaceAll("\\&", "");
        return (msg);
    }

    public void sendData() {
        System.out.println("Inside send Data run by thread" + Thread.currentThread().getName());
        DatagramSocket aSocket = null;
        int index;
        try {
            System.out.println("UDPServer.main()");
            aSocket = new DatagramSocket(7002);
//			byte[] buffer = new byte[1000];
//			byte[] outGoing = new byte[1000];
//			String inMsg;
            while (true) {
                byte[] buffer = new byte[1000];
                byte[] outGoing = new byte[1000];
                String inMsg = null;
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                inMsg = new String(request.getData());
                System.out.println("SERVER INTAKE: " + inMsg);

                if (inMsg.startsWith("Physician")) {
                    outGoing = (docMap.toString()).getBytes();
                    DatagramPacket reply = new DatagramPacket(outGoing, outGoing.length, request.getAddress(), request.getPort());
                    aSocket.send(reply);
                }
                if (inMsg.startsWith("Dentist")) {
                    outGoing = (dentMap.toString()).getBytes();
                    DatagramPacket reply = new DatagramPacket(outGoing, outGoing.length, request.getAddress(), request.getPort());
                    aSocket.send(reply);
                }
                if (inMsg.startsWith("Surgeon")) {
                    outGoing = (surgMap.toString()).getBytes();
                    DatagramPacket reply = new DatagramPacket(outGoing, outGoing.length, request.getAddress(), request.getPort());
                    aSocket.send(reply);
                }

                if (inMsg.startsWith("$")) {
                    System.out.println("inside que server if(send $) ");
                   // inMsg= inMsg.replaceAll("\\$","");
                    inMsg= cleanStr(inMsg);

                    if (users.containsKey(inMsg)) {
                        System.out.println("users.containsKey(inMsg) ");
                        outGoing = "".toString().getBytes();
                        this.patientID = users.get(inMsg);
                        outGoing = ((patientID.getAppMap()).toString()).getBytes();
                        System.out.println("This was sent " + patientID.getAppMap());
                    }
                    DatagramPacket reply = new DatagramPacket(outGoing, outGoing.length, request.getAddress(), request.getPort());
                    aSocket.send(reply);
                }
                if (inMsg.startsWith("%")) {
                    System.out.println("inside server udp book %");
                    inMsg = cleanStr(inMsg);
                    String[] arr = inMsg.split(",");
                    if (!inMsg.isEmpty()) {
                        String res = bookAppointment(arr[0],arr[1],arr[2]);
                        if(res.startsWith("Success")){
                            outGoing = "pass".getBytes();
                        }
                        else outGoing = "fail".getBytes();
                    }
                    DatagramPacket reply = new DatagramPacket(outGoing, outGoing.length, request.getAddress(), request.getPort());
                    aSocket.send(reply);
                    System.out.println("This was sent " + new String(outGoing));
                }
                if (inMsg.startsWith("&")) {
                    System.out.println("inside server udp cancel &");
                    inMsg = cleanStr(inMsg);
                    String[] arr = inMsg.split(",");
                    if (!inMsg.isEmpty()) {
                        String res = cancelAppointment(arr[0],arr[1],"junk");
                        if(res.startsWith("Appointment")){
                            outGoing = "pass".getBytes();
                        }
                        else outGoing = "fail".getBytes();
                    }
                    DatagramPacket reply = new DatagramPacket(outGoing, outGoing.length, request.getAddress(), request.getPort());
                    aSocket.send(reply);
                    System.out.println("This was sent " + new String(outGoing));
                }
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null) aSocket.close();
        }

    }

    //This is my client
    public String recvData(String appointmentType) {
        String retMsg = null;
        String temp = null;
        DatagramSocket aSocket = null;
        try {
            //System.out.println("Start send");
            aSocket = new DatagramSocket();
            //String s = "MS" + docMap.toString() + "$" + dentMap.toString() + "$" + surgMap.toString() + "$";
            byte[] m = appointmentType.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
///send and receive reply from Sherbrooke
            int serverPortShe = 7001;
            DatagramPacket requestShe = new DatagramPacket(m, m.length, aHost, serverPortShe);
            aSocket.send(requestShe);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(reply);
            temp = cleanStr(new String(reply.getData()));
            retMsg = ("#Available " + appointmentType + " appointments in Sherbrooke:\n" + temp);
///send and receive reply from Montreal
            int serverPortQue = 7000;
            DatagramPacket requestQue = new DatagramPacket(m, m.length, aHost, serverPortQue);
            aSocket.send(requestQue);
            buffer = new byte[1000];
            reply = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(reply);
            //System.out.println("Reply: " + new String(reply.getData()));
            temp = cleanStr(new String(reply.getData()));
            retMsg = (retMsg + "\n#Available " + appointmentType + " appointments in Montreal:\n" + temp);
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
       // System.out.println(retMsg + "Just before returning RMI call");
        return retMsg;

    }

    public boolean checkCred(String AdminID) {
        if (adminUsers.containsKey(AdminID)) {
            this.adminID = adminID;
            writelog("checkCred", AdminID, "", "", "Completed", "true");
            return (true);
        }
        writelog("checkCred", AdminID, "", "", "Failed", "false");
        return false;
    }


    public String getAppList(String locReq, String patID) {
        //need to get all appointments booked by patient so send req to other 2 servers
        DatagramSocket aSocket = null;
        String temp= null;
        HashMap<String, String> retMsg = new HashMap<>();
        try {

            aSocket = new DatagramSocket();
            byte[] m = ("$"+patID).getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
///send and receive reply from Sherbrooke
            if(locReq.equals("SHE")) {
                int serverPortShe = 7001;
                DatagramPacket request= new DatagramPacket(m, m.length, aHost, serverPortShe);
                aSocket.send(request);

                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);
                temp = cleanStr(new String(reply.getData()));
                String[] arr = temp.split(",");
//                if(!temp.isEmpty()) {
//                    for (String c : arr) {
//                        String[] tempArr = c.split("=");
//                        retMsg.put(tempArr[0], tempArr[1]);
//                    }
//                }
            }
            ///send and receive reply from Quebec
            if(locReq.equals("MTL")) {
                int serverPortShe = 7000;
                DatagramPacket request = new DatagramPacket(m, m.length, aHost, serverPortShe);
                aSocket.send(request);

                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);
                temp = cleanStr(new String(reply.getData()));
                String[] arr = temp.split(",");
//                if(!temp.isEmpty()) {
//                    for (String c : arr) {
//                        String[] tempArr = c.split("=");
//                        retMsg.put(tempArr[0], tempArr[1]);
//                    }
//                }
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        System.out.println(temp + "Just before returning RMI call");
        return temp;

    }


    public boolean isBookableExt(String patLoc, String ID, String type, String patID) {

        HashMap<String, String> que = null;
        if(users.containsKey(patID) && users.get(patID).getAppMap() != null && !users.get(patID).getAppMap().isEmpty()){
            que = users.get(patID).getAppMap();
        }

        HashMap<String, String> mtl = new HashMap<>();
        HashMap<String, String> she = new HashMap<>();


        String mtlTemp = getAppList("MTL", patID);
        String[] arr = mtlTemp.split(",");
        if (!mtlTemp.isEmpty()) {
            for (String c : arr) {
                String[] tempArr = c.split("=");
                mtl.put(tempArr[0], tempArr[1]);
            }
        }
        String sheTemp = getAppList("SHE", patID);
        String[] arr2 = sheTemp.split(",");
        if (!sheTemp.isEmpty()) {
            for (String c : arr2) {
                String[] tempArr = c.split("=");
                she.put(tempArr[0], tempArr[1]);
            }
        }

        System.out.println("IsBookable QUE =:\n" +
                "MTL = " +mtl +
                "\nSHE = " +she+
                "\nQUE = "+ que);

        String test = ID.substring(4, 8);
        String temp;
        String locTemp;

        double date = Double.valueOf("." + ID.substring(4, 8));

        int count = 0;
        // MTL DB
        if (mtl != null) {
            for (String key : mtl.keySet()) {
                String year = ID.substring(8);
                String month = ID.substring(6, 8);
                String day = ID.substring(4, 6);
                String time = ID.substring(3, 4);
                temp = key.substring(4, 8);
                locTemp = key.substring(0, 3);
                //MTL DB
                if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && mtl.get(key).equals(type)) {
                    System.out.println("Can not book the same type of app in the same day");
                    return false;
                }
                if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && key.substring(3, 4).equals(time)) {
                    System.out.println("Have another appointment booked someplace else");
                    return false;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
                locTemp = key.substring(0, 3);
                Date keyDate= null;
                Date compDate=null;
				try {
					keyDate = sdf.parse(key.substring(4));
					compDate = sdf.parse(ID.substring(4));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                long diff = compDate.getTime() - keyDate.getTime();


                int days = (int) (diff / (1000*60*60*24));
                days= Math.abs(days);

                if (!locTemp.equals(patLoc) && days < 7){
                    count++;

                }
                if (count >= 3 && !patLoc.equals("QUE")) {
                    System.out.println("Can not book more than 3 appointments in one week outside your home city");
                    return false;
                }
            }
        }
        //Que DB
        if (que != null) {
            for (String key : que.keySet()) {
                String year = ID.substring(8);
                String month = ID.substring(6, 8);
                String day = ID.substring(4, 6);
                String time = ID.substring(3, 4);
                temp = key.substring(4, 8);
                locTemp = key.substring(0, 3);
                if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && que.get(key).equals(type)) {
                    System.out.println("Can not book the same type of app in the same day");
                    return false;
                }
                if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && key.substring(3, 4).equals(time)) {
                    System.out.println("Have another appointment booked someplace else");
                    return false;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
                locTemp = key.substring(0, 3);
                Date keyDate= null;
                Date compDate=null;
				try {
					keyDate = sdf.parse(key.substring(4));
					compDate = sdf.parse(ID.substring(4));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                long diff = compDate.getTime() - keyDate.getTime();


                int days = (int) (diff / (1000*60*60*24));
                days= Math.abs(days);

                if (!locTemp.equals(patLoc) && days < 7){
                    count++;

                }
                if (count >= 3 && !patLoc.equals("QUE")) {
                    System.out.println("Can not book more than 3 appointments in one week outside your home city");
                    return false;
                }
            }
        }
        //SHE DB
        if (she != null) {
            for (String key : she.keySet()) {
                String year = ID.substring(8);
                String month = ID.substring(6, 8);
                String day = ID.substring(4, 6);
                String time = ID.substring(3, 4);
                temp = key.substring(4, 8);
                locTemp = key.substring(0, 3);

                if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && she.get(key).equals(type)) {
                    System.out.println("Can not book the same type of app in the same day");
                    return false;
                }
                if (key.substring(8).equals(year) && key.substring(6, 8).equals(month) && key.substring(4, 6).equals(day) && key.substring(3, 4).equals(time)) {
                    System.out.println("Have another appointment booked someplace else");
                    return false;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
                locTemp = key.substring(0, 3);
                Date keyDate= null;
                Date compDate=null;
				try {
					keyDate = sdf.parse(key.substring(4));
					compDate = sdf.parse(ID.substring(4));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                long diff = compDate.getTime() - keyDate.getTime();


                int days = (int) (diff / (1000*60*60*24));
                days= Math.abs(days);

                if (!locTemp.equals(patLoc) && days < 7){
                    count++;

                }
                if (count >= 3 && !patLoc.equals("QUE")) {
                    System.out.println("Can not book more than 3 appointments in one week outside your home city");
                    return false;
                }
            }

        }
        return true;
    }

    public synchronized boolean swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {
        String oldLoc = oldAppointmentID.substring(0, 3);
        String newLoc = newAppointmentID.substring(0, 3);
        byte[] sentMsgNew = ("%" + patientID+","+newAppointmentID+","+newAppointmentType).getBytes();
        byte[] sentMsgOld = ("%" + patientID+","+oldAppointmentID+","+oldAppointmentType).getBytes();
        byte[] cancelMsgOld = ("&" + patientID+","+oldAppointmentID).getBytes();
        DatagramSocket aSocket = null;
        String temp;
        int serverPort =0000;
        if(newLoc.equals("QUE")){
            serverPort=7002;
        }
        if(newLoc.equals("SHE")){
            serverPort=7001;
        }
        if(newLoc.equals("MTL")){
            serverPort=7000;
        }
        //Montreal local operation
        if (oldLoc.equals("QUE")) {
            if (!cancelAppointment(patientID, oldAppointmentID,"junk").startsWith("Appointment")) {
    	        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                return false;
            }
            if (newLoc.equals("QUE")) {
                if (!bookAppointment(patientID, newAppointmentID, newAppointmentType).startsWith("Success")) {
                    bookAppointment(patientID, oldAppointmentID, oldAppointmentType);
                    writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                    return false;
                } else {
                	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
                	return true;
                }
            }
            ///send and receive reply from Quebec ans SHE
            if (newLoc.equals("MTL") || newLoc.equals("SHE")) {
                try {
                    aSocket = new DatagramSocket();
                    InetAddress aHost = InetAddress.getByName("localhost");
                    DatagramPacket request = new DatagramPacket(sentMsgNew, sentMsgNew.length, aHost, serverPort);
                    aSocket.send(request);
                    byte[] buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);
                    temp = cleanStr(new String(reply.getData()));
                    System.out.println("\n\n\n                       This is the value of temp just before if " + temp + "\n\n\n\n ");
                    if(!temp.startsWith("pass")){
                    	String rest= bookAppointment(patientID, oldAppointmentID, oldAppointmentType);
                    	System.out.println("\n\n\n   "+ rest);
 	        	        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
 	                    return false;
//                        request = new DatagramPacket(sentMsgOld, sentMsgOld.length, aHost, serverPort);
//                        aSocket.send(request);
//                        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
//                        return false;
                    }
                    else {
                    	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
                	return true;
                	}

                } catch (SocketException e) {
                    System.out.println("Socket: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO: " + e.getMessage());
                } finally {
                    if (aSocket != null)
                        aSocket.close();
                }
            }
        }

        if (oldLoc.equals("MTL")) {
        	int oldPort=7000;
            try {
                
                aSocket = new DatagramSocket();
                InetAddress aHost = InetAddress.getByName("localhost");
                DatagramPacket request = new DatagramPacket(cancelMsgOld, cancelMsgOld.length, aHost, oldPort);
                aSocket.send(request);
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);
                temp = cleanStr(new String(reply.getData()));
                if(!temp.startsWith("pass")){
                	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                    return false;
                }
                //changed this!
                if (newLoc.equals("QUE")) {
	                if (!bookAppointment(patientID, newAppointmentID, newAppointmentType).startsWith("Success")) {
	                	request = new DatagramPacket(sentMsgOld, sentMsgOld.length, aHost, oldPort);
                        aSocket.send(request);
                        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                        return false;
	                } else {
	                	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
                    	return true;
	                }
	            }

            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            } finally {
                if (aSocket != null)
                    aSocket.close();
            }

            if (newLoc.equals("MTL") || newLoc.equals("SHE")) {
                try {
                    aSocket = new DatagramSocket();
                    InetAddress aHost = InetAddress.getByName("localhost");
                    DatagramPacket request = new DatagramPacket(sentMsgNew, sentMsgNew.length, aHost, serverPort);
                    aSocket.send(request);
                    byte[] buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);
                    temp = cleanStr(new String(reply.getData()));
                    if(!temp.startsWith("pass")){
                        request = new DatagramPacket(sentMsgOld, sentMsgOld.length, aHost, oldPort);
                        aSocket.send(request);
                        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                        return false;
                    }
                    else{
                    	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
                    	return true;
                    }

                } catch (SocketException e) {
                    System.out.println("Socket: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO: " + e.getMessage());
                } finally {
                    if (aSocket != null)
                        aSocket.close();
                }
            }
        }

        if (oldLoc.equals("SHE")) {
        	int oldPort=7001;
            try {
                
                aSocket = new DatagramSocket();
                InetAddress aHost = InetAddress.getByName("localhost");
                DatagramPacket request = new DatagramPacket(cancelMsgOld, cancelMsgOld.length, aHost, oldPort);
                aSocket.send(request);
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);
                temp = cleanStr(new String(reply.getData()));
                if(!temp.startsWith("pass")){
                	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                    return false;
                }
                if (newLoc.equals("QUE")) {
	                if (!bookAppointment(patientID, newAppointmentID, newAppointmentType).startsWith("Success")) {
	                	request = new DatagramPacket(sentMsgOld, sentMsgOld.length, aHost, oldPort);
                        aSocket.send(request);
                        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                        return false;
//	                    bookAppointment(patientID, oldAppointmentID, oldAppointmentType);
//	                    writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
//	                    return false;
	                } else {
	                	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
                    	return true;
	                }
	            }

            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            } finally {
                if (aSocket != null)
                    aSocket.close();
            }

            if (newLoc.equals("MTL") || newLoc.equals("SHE")) {
                try {
                    aSocket = new DatagramSocket();
                    InetAddress aHost = InetAddress.getByName("localhost");
                    DatagramPacket request = new DatagramPacket(sentMsgNew, sentMsgNew.length, aHost, serverPort);
                    aSocket.send(request);
                    byte[] buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);
                    temp = cleanStr(new String(reply.getData()));
                    if(!temp.startsWith("pass")){
                        request = new DatagramPacket(sentMsgOld, sentMsgOld.length, aHost, oldPort);
                        aSocket.send(request);
                        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
                        return false;
                    }
                    else {
                    	
                    	writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Completed", "pass");
                    	return true;
                    }
                     

                } catch (SocketException e) {
                    System.out.println("Socket: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO: " + e.getMessage());
                } finally {
                    if (aSocket != null)
                        aSocket.close();
                }
            }
        }
        writelog("swapAppointment", patientID, (oldAppointmentID +" for "+ newAppointmentID), (oldAppointmentType+" for "+newAppointmentType),"Failed", "fail");
        return false;
    }

	



}