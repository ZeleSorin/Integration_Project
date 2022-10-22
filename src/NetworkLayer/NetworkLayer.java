package NetworkLayer;

import ApplicationLayer.ApplicationLayer;
import TimerByUs.TimerByUs;
import TimerByUs.RetransmissionTimer;
import TimerByUs.DiscoveryRetransmissionTimer;
import Tools.Translator;
import client.Message;
import client.MessageType;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * NetworkLayer class models a real-life network layer .
 * It sits in between the Client class which models the physical layer and the ApplicationLayer which models the application layer.
 * The class takes care of the user-inputs and the packets received by analyzing the header or the type of th message.
 */
public class NetworkLayer extends Thread {


    private int mySeq = 0;
    private int help = 0;
    /**
     * forwardingMap holds the id as key and an arrayList with the id's the are accesible through that node.
     */
    Map<Integer, ArrayList<Integer>> forwardingMap = new HashMap<>();
    Lock lock = new ReentrantLock();

    public void increaseMySeq() {
        mySeq = mySeq + 1;
    }

    /**
     * In order to make our work easier we designed a discoveryPacket that uses the header
     * to send the unique id through the network. We later decided to use the discovery packet to also
     * send the username , thus adding a "secret" code to the packet in order to easily differentiate it.
     * The code doesn't occupy extra space as the packet is mostly empty.
     */
    private static final String DISCOVERYCODE = "DS:";
    /**
     * id in range of 250*250;
     */
    private int uniqueID;
    /**
     * number between 0 and 250. Its multiplication with the 'secondHalf' results in the uniqueID
     */
    private int firstHalf;
    /**
     * number between 0 and 250. Its multiplication with the 'firstHalf' results in the uniqueID
     */
    private int secondHalf;
    /**
     * local id representing your source address ;
     */
    private static int bitID;
    /**
     * boolean that is set to true the moment an Id is created.
     * Used for further checks.
     */
    private boolean Idcreated = false;

    private final PacketHandler packetHandler = new PacketHandler();


    /**
     * mat that links every local id to a username.
     */
    public Map<Integer, String> usernameToIDLink = new HashMap<>();
    /**
     * map that links the local id with the global ids.
     */
    private Map<Integer, Integer> localIDtoGlobalIdLink = new HashMap<>();
    /**
     * map that links the id of a node and the seqNr that we are need to send to that specific node.
     */
    private Map<Integer, Integer> sendingSeqNum = new HashMap<>();
    /**
     * map that links an id with the seqNr received from that id. It is used to check if a packet is a duplicate.
     */
    private Map<Integer, ArrayList<Integer>> receivingSeqNum = new HashMap<>();
    /**
     * List that contains the id and the last time we had contact with it.
     */
    private Map<Integer, Date> echoList;
    /**
     *
     */
    private BlockingQueue<Message> receivingQueue;
    /**
     *
     */
    private static BlockingQueue<Message> sendingQueue;


    /**
     * arrayList that stores all the long Id's;
     */
    public ArrayList<Integer> listWithLongIDs = new ArrayList();

    /**
     * arrayList that contains all the usernames received.
     */
    public ArrayList<String> usernames = new ArrayList<>();
    /**
     *
     */
    private NetworkHandler networkHandler;
    /**
     *
     */
    private BlockingQueue<String> queueThatSendsMessagesToTheApplicationLAyer;
    /**
     *
     */
    private BlockingQueue<String> queueWhereWeReadInputsFromTheAPPLayer;
    /**
     *
     */
    private static Fragmentor fragmentor;
    /**
     *
     */
    private TimerByUs timer;
    /**
     *
     */
    private DiscoveryRetransmissionTimer discoveryRetransmissionTimer;
    /**
     * message used to create the automated discovery process.
     */
    private Message discoveryBackup;
    /**
     *
     */
    private RetransmissionTimer retransmissionTimer;
    /**
     *
     */
    private Map<Integer, Date> retransmissionMap = new HashMap<>();
    /**
     *
     */
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);

    /**
     *
     */
    public static final byte PADDING = '*';

    /**
     *
     */
    public void setUniqueID() {
        this.uniqueID = new Random().nextInt(250);
    }

    /**
     *
     */
    public static int getBitID() {
        return bitID;
    }

    /**
     *
     */


    public NetworkLayer(BlockingQueue<String> queueWhereWeReadInputsFromTheAPPLayer,
                        BlockingQueue<String> queueThatSendsMessagesToTheApplicationLAyer,
                        BlockingQueue<Message> sendingQueue,
                        BlockingQueue<Message> receivingQueue) {

        this.queueThatSendsMessagesToTheApplicationLAyer = queueThatSendsMessagesToTheApplicationLAyer;

        this.sendingQueue = sendingQueue;

        this.receivingQueue = receivingQueue;


        networkHandler = new NetworkHandler();

        echoList = new HashMap<>();

        this.queueWhereWeReadInputsFromTheAPPLayer = queueWhereWeReadInputsFromTheAPPLayer;

        timer = new TimerByUs(echoList);

        fragmentor = new Fragmentor(queueWhereWeReadInputsFromTheAPPLayer, queueThatSendsMessagesToTheApplicationLAyer, sendingQueue);

        ApplicationListener applicationListener = new ApplicationListener(queueWhereWeReadInputsFromTheAPPLayer, packetHandler);

        threadPool.execute(applicationListener);

        threadPool.execute(timer);

        retransmissionTimer = new RetransmissionTimer(retransmissionMap);

        threadPool.execute(retransmissionTimer);

        ID();


    }

    /**
     * run method automatically starts the communication by sending a discovery packet.
     */
    public void run() {

        sendDiscovery();
    }

    /**
     * sendDiscovery method that creates a discovery message, stores and sends it.
     * The message is later reused
     */
    private void sendDiscovery() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put((byte) firstHalf);
        buffer.put((byte) secondHalf);
        buffer.put((byte) 'D');
        buffer.put((byte) 'S');
        buffer.put((byte) ':');
        while (true) {

            if (ApplicationLayer.usernameSet) {

                System.out.println("USERNAME:" + ApplicationLayer.username);
                usernames.add(ApplicationLayer.username);
                byte[] bytes = ApplicationLayer.username.getBytes(StandardCharsets.UTF_8);
                for (int i = 5; i < bytes.length + 5; i++) {
                    buffer.put(bytes[i - 5]);
                }
                for (int i = bytes.length + 5; i < buffer.capacity(); i++) {
                    buffer.put(PADDING);
                }
                Message msg = new Message(MessageType.DATA, buffer);
                discoveryBackup = msg;
                this.discoveryRetransmissionTimer = new DiscoveryRetransmissionTimer(msg, sendingQueue);
                threadPool.execute(discoveryRetransmissionTimer);
                try {
                    sendingQueue.put(msg);
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    /**
     * ID method creates a uniqueID
     */
    private void ID() {
        if (!Idcreated) {
            setUniqueID();
            firstHalf = uniqueID;
            setUniqueID();
            secondHalf = uniqueID;
            uniqueID = firstHalf * secondHalf;
            Idcreated = true;
            listWithLongIDs.add(uniqueID);
        }
    }

    /**
     * calls the packetHandler class to analyse a received message.
     *
     * @param m message received from the linkLayer
     */
    public void startPacketAnalysis(Message m) {
        packetHandler.packetAnalyzer(m);

    }

    /**
     * Method called for retransmission.
     * It takes the message from the backupBuffer using the sequenceNr as the key.
     *
     * @param seq sequence nr of specific packet that needs to be retransmited.
     */
    public static void retransmission(int seq) {
        Message m = new Message(MessageType.DATA, fragmentor.backupBuffer.get(seq));
        try {
            sendingQueue.put(m);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    //--------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------
    // NETWORK HANDLER

    /**
     * Private class that creates a network handler.
     * Network handler is used to monitor the network.
     * Monitoring the network includes:
     * - managing the discovery packets.
     * - managing the ACK packets.
     */
    private class NetworkHandler {


        /**
         * The method takes a header as a parameter and handles the seqNr updates and retransmission
         * of certain packets.
         *
         * @param buffer contains the header stored as a binary representation..
         */
        private void handleACKs(ByteBuffer buffer) {
            byte fst = buffer.get(0);

            byte scnd = buffer.get(1);

            char[] fsarr = Translator.byteTranslator(fst);

            char[] sndarr = Translator.byteTranslator(scnd);

            char[] srcAddress;

            char[] sequenceNumber;

            char[] destAddress = Arrays.copyOfRange(fsarr, 4, 8);

            String ss = new String(destAddress);

            int destAd = Integer.parseInt(ss, 2);

            help = help + 1;

            sequenceNumber = Arrays.copyOfRange(sndarr, 0, 5);

            srcAddress = Arrays.copyOfRange(fsarr, 0, 4);

            String s = new String(sequenceNumber);

            int seq = Integer.parseInt(s, 2);

            String y = new String(srcAddress);

            int srcAddre = Integer.parseInt(y, 2);

            echoList.put(srcAddre, new Date(System.currentTimeMillis()));


            if ((destAd == bitID)) {

                System.out.println("[ACK RECEIVED FROM USER] : " + usernameToIDLink.get(srcAddre));


                if (!sendingSeqNum.containsKey(srcAddress)) {

                    sendingSeqNum.put(srcAddre, seq);

                } else {

                    if (seq == sendingSeqNum.get(srcAddre)) {

                        int a = seq + 1;

                        sendingSeqNum.remove(srcAddre);

                        sendingSeqNum.put(srcAddre, a);

                    } else {

                        retransmission(seq);
                    }
                }
            }

            if (help == sendingSeqNum.size()) {

                help = 0;

                lock.lock();

                retransmissionMap.remove(seq);

                lock.unlock();
            }
        }


        /**
         * Increases the sequence number.
         */
        private void increaseSeqNum() {
            mySeq++;
        }


        /**
         * resets seqNum if
         * it has
         * reached 32(
         * equivalent to 5bits).
         */

        private int resetSeqNum(int seqNum) {
            if (seqNum == 32) {
                seqNum = 0;
            }
            return seqNum;
        }


        /**
         * The method takes a header as a parameter and handles the creation of a new entry in the routing tables.
         *
         * @param first,second contains the header stored as a binary representation.
         * @param usermane     contains the username for this specific node.
         */
        private void handleDISCOVERYs(byte first, byte second, String usermane) {
            int ID1 = first & 0xFF;
            int ID2 = second & 0xFF;
            int finalID = Math.abs(ID1 * ID2);

            if (!listWithLongIDs.contains(finalID)) {

                listWithLongIDs.add(finalID);


                Collections.sort(listWithLongIDs);
                for (int i = 0; i < listWithLongIDs.size(); i++) {
                    int a = listWithLongIDs.get(i);
                    localIDtoGlobalIdLink.put(i + 1, a);
                    if (a == uniqueID) {
                        bitID = i + 1;

                    }
                    if (a == finalID) {
                        receivingSeqNum.put(i + 1, new ArrayList<Integer>());
                        usernameToIDLink.put(i + 1, usermane);
                        sendingSeqNum.put(i + 1, 0);

                    }
                }

            }
        }


    }


    /**
     * PacketHandler class that handles the packets received from the LinkLayer.
     * It analyses messages and acts acording with the type of message received
     */
    private class PacketHandler {


        /**
         * packetAnalizer its gonna take a packet and analize its data type, and then it will check the content
         * of the packet and further transmit it to the application layer.
         */
        private void packetAnalyzer(Message message) {

            if (message.getType().equals(MessageType.DATA_SHORT)) {

                shortDataAnalyze(message.getData());
            } else {
                dataAnalyze(message.getData());
            }
        }

        /**
         * dataAnalyze take 32 byte packet and analyzes it.
         *
         * @param buffer
         */
        private void dataAnalyze(ByteBuffer buffer) {
            byte[] bytes;
            bytes = Translator.returnAllByteArray(buffer);

            byte[] b = Translator.deletePadding(bytes);
            ByteBuffer bb = ByteBuffer.allocate(b.length);
            bb.put(b);

            if (!isDicoveryCheck(bb)) {

                isPacket(bb);

            }

        }

        /**
         * shortdataAnalize receives a buffer of a small packet and analyzes it .
         * By default a short data is a header size message
         *
         * @param buffer the content of the message.
         */
        private void shortDataAnalyze(ByteBuffer buffer) {
            char[] firstByte = Translator.byteTranslator(buffer.get(0));
            char[] secondByte = Translator.byteTranslator(buffer.get(1));


            boolean isEcho = isEchoCheck(firstByte, secondByte);
            if (!isEcho) {

                networkHandler.handleACKs(buffer);
            }

        }


        /**
         * @param buff buffer containing the packet.
         * @return true is the header is one of a discovery message.
         * Discovery messages are characterized by the following rules:
         * - bits 4-8 should equal to my ID(It might be the case where this is a ACK but not for me)
         * - bits 8-13 should represent a value closer to my sequence of the packets exchanged
         * with this node.
         * If the above conditions are not meet, a further check is needed to ensure this packet is indeed
         * a discovery packet.
         * The further check is done by checking the sourceAddres bits(0-3) into our routing tables.
         * If the ID doesn't exists in our table, we treat this packet as a discovery packet,thus
         * processing hi
         */
        private boolean isDicoveryCheck(ByteBuffer buff) {
            byte first = buff.get(0);
            byte second = buff.get(1);
            byte[] bytes = new byte[3];

            //-------------------DO DISCOVERY CHECK---------------------
            if (buff.capacity() > 5) {
                if (extractCode(Translator.getBytesInRange(buff, 2, 4, true)).equals(DISCOVERYCODE)) {
                    networkHandler.handleDISCOVERYs(first, second, packetHandler.getUsername(buff));
                    return true;
                } else {
                    return false;
                }
            }
            return false;

        }


        /**
         * This method takes a byte[] and appends all the elements to a string and returns it.
         * The elements are the byte nr 3,4 and 5 which represents the secret discovery Code.
         *
         * @param aByte byte array containing the first 3 bytes of the data.
         * @return String containing the "potential" code.
         */
        private String extractCode(byte[] aByte) {
            return new String(aByte);
        }

        /**
         * Method takes the header stored in two char[].
         * It checks if the header is one of an Echo message.
         *
         * @param first  first half of the header in binary representation.
         * @param second second hald of the header in binary representation.
         * @return true if the header is one of an Echo message.
         * Echo messages are characterized by having only the first quarter of the header != 0.
         */
        private boolean isEchoCheck(char[] first, char[] second) {
            for (int i = 4; i < 8; i++) {
                if (first[i] != 0) {
                    return false;
                }
            }
            for (int i = 0; i < 8; i++) {
                if (second[i] != 0) {
                    return false;
                }
            }

            return true;
        }


        private String getUsername(ByteBuffer buffer) {
            byte[] byteUsername = new byte[5];
            for (int i = 5; i < 10; i++) {
                byteUsername[i - 5] = buffer.get(i);
            }

            String usernam = Translator.byteTranslatorToString(byteUsername);
            return usernam;

        }


        /**
         * This method  formats the message that follows to be sent to the app layer.
         * The method takes care of sending Ack packets.
         *
         * @param buffer The method processes the content of the message and sends it to the application layer using
         *               ApplicationQueue
         */
        private void isPacket(ByteBuffer buffer) {
            char[] chars;
            boolean retr = false;
            chars = Translator.byteTranslator(buffer.get(1));
            if (Integer.parseInt(String.valueOf(Translator.byteTranslator(buffer.get(1))[6])) == 1) {
                Message m = new Message(MessageType.DATA, buffer);
                try {
                    sendingQueue.put(m);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            int id = processID(Translator.byteTranslator(buffer.get(0)));
            int seq = processSeq(Translator.byteTranslator(buffer.get(1)));
            if (!(id == bitID)) {
                echoList.remove(id);
                echoList.put(id, new Date(System.currentTimeMillis()));
                if (receivingSeqNum.containsKey(id)) {
                    ArrayList<Integer> arr = receivingSeqNum.get(id);
                    if (!arr.contains(seq)) {

                        receivingSeqNum.get(id).add(seq);

                    } else {
                        retr = true;

                    }
                } else {
                    receivingSeqNum.put(id, new ArrayList<Integer>());
                    receivingSeqNum.get(id).add(seq);

                }
                if (retr) {
                    if (Integer.parseInt(String.valueOf(chars[5])) == 1) {
                        System.out.println("WE HAVE FRAGMENTATION FLAG ON!");

                        fragmentor.mergeMessage(buffer);
                    } else {
                        byte[] bytes = Translator.returnByteArray(buffer, 2);
                        sendAck(buffer);
                        byte[] bb = Translator.deletePadding(bytes);

                        ByteBuffer newOne = ByteBuffer.allocate(bb.length);
                        newOne.put(bb);

                        String ss = Translator.createString(newOne);
                        try {

                            queueThatSendsMessagesToTheApplicationLAyer.put(ss);

                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    byte[] bytes = Translator.returnByteArray(buffer, 2);

                    byte[] bb = Translator.deletePadding(bytes);

                    ByteBuffer newOne = ByteBuffer.allocate(bb.length);
                    newOne.put(bb);

                    String ss = Translator.createString(newOne);
                    ArrayList<Integer> arr = new ArrayList<>();
                    arr.add(seq);
                    receivingSeqNum.put(id, arr);
                    String ret = parseMessage(usernameToIDLink.get(id), ss);
                    try {
                        queueThatSendsMessagesToTheApplicationLAyer.put(ret);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    sendAck(buffer);
                }

                if (getForwarding(buffer.get(1))) {
                    if (forwardingMap.containsKey(id)) {
                        if (forwardingMap.get(id).equals(seq)) {
                            //If we have the id and the seq, we dont do anything
                        } else {
                            // if we have the id but not the seq, we retr.
                            ByteBuffer buffs = addPadding(buffer);
                            Message m = new Message(MessageType.DATA, buffs);
                            forwardingMap.get(id).add(seq);
                            try {
                                sendingQueue.put(m);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        //if we dont have the id in the map
                        forwardingMap.put(id, new ArrayList<Integer>());
                        forwardingMap.get(id).add(seq);
                        ByteBuffer b = addPadding(buffer);
                        Message m = new Message(MessageType.DATA, b);
                        try {
                            sendingQueue.put(m);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        }

        private ByteBuffer addPadding(ByteBuffer b) {
            int n = b.capacity();
            ByteBuffer buffer = ByteBuffer.allocate(32);
            for (int i = 0; i < n; i++) {
                buffer.put(b.get(i));
            }
            if (n < 32) {
                for (int i = n; n < 32; n++) {
                    buffer.put(PADDING);
                }
            }
            return buffer;
        }


        private String parseMessage(String username, String message) {
            return "[" + username + "]: " + message;
        }

        public static void printByteBuffer(ByteBuffer bytes, int bytesLength) {
            for (int i = 0; i < bytesLength; i++) {
                System.out.print(Byte.toString(bytes.get(i)) + " ");
            }
            System.out.println();
        }

        public int processSeq(char[] c) {
            String s = Translator.charToStringBuilder(c, 0, 4);
            int seq = Integer.parseInt(s, 2);
            return seq;
        }

        public int processID(char[] c) {
            String s = Translator.charToStringBuilder(c, 0, 3);
            int id = Integer.parseInt(s, 2);
            return id;
        }


        /**
         * Method sends acknowledgement
         *
         * @param buffer header of the message.
         */
        private void sendAck(ByteBuffer buffer) {
            int seq = getSeqNr(buffer.get(1));
            int id = getReceivedID(buffer.get(0));
            boolean retransmission = getForwarding(buffer.get(1));
            System.out.println(id);
            if (receivingSeqNum.get(id).contains(seq)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                createAck(buffer);
            } else {
                receivingSeqNum.get(id).add(seq);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                retransmissionMap.put(seq, new Date(System.currentTimeMillis()));
                createAck(buffer);
            }
        }

        /**
         * @param aByte
         * @return true if we need to forward the message.
         */
        private boolean getForwarding(byte aByte) {
            char[] header = Translator.byteTranslator(aByte);
            if (Integer.parseInt(String.valueOf(header[6])) == 1) {
                return true;
            }

            return false;
        }

        /**
         * @param aByte contain the second half of the header.
         * @return true if we need to retransmit the packet
         */
        private boolean getRetransmission(byte aByte) {
            char[] header = Translator.byteTranslator(aByte);
            if (Integer.parseInt(String.valueOf(header[7])) == 1) {
                return true;
            }

            return false;
        }

        /**
         * Method creates an Ack message
         *
         * @param buffer header of the received message.
         */
        public void createAck(ByteBuffer buffer) {
            char[] header = new char[16];
            char[] intToCharArray;
            int id = getReceivedID(buffer.get(0));
            int seq = getSeqNr(buffer.get(1));

            boolean forwarding = getForwarding(buffer.get(1));
            boolean retransmission = getRetransmission(buffer.get(1));

            //-----srcAddress------\\
            int srcAddress = NetworkLayer.getBitID();
            intToCharArray = fragmentor.headerHelper(4, srcAddress);
            for (int i = 0; i < 4; i++) {
                header[i] = intToCharArray[i];
            }


            //--------------DestAddress------------------\\
            char[] help = fragmentor.headerHelper(4, id);
            for (int i = 4; i < 8; i++) {
                header[i] = help[i - 4];

            }


            //----------seqNum--------\\


            intToCharArray = fragmentor.headerHelper(5, seq);
            for (int i = 8; i < 13; i++) { //MEthod for this
                header[i] = intToCharArray[i - 8];

            }


            //------------OffsetFlag-----------------\\

            header[13] = '0';


            //-------------forwardingFlag--------\\
            if (forwarding) {
                header[14] = '1';
            } else {
                header[14] = '0';
            }


            //--retransmission-----\\
            if (retransmission) {
                header[15] = '1';
            } else {
                header[15] = '0';
            }


            byte fb = fragmentor.quickMethod(0, 8, header);

            byte sb = fragmentor.quickMethod(8, 16, header);
            ByteBuffer buffy = ByteBuffer.allocate(2);
            buffy.put(fb);
            buffy.put(sb);
            Message m = new Message(MessageType.DATA_SHORT, buffy);
            try {
                sendingQueue.put(m);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        }

        private int getReceivedID(byte aByte) {

            char[] chars = Translator.byteTranslator(aByte);
            char[] chars1 = new char[4];
            for (int i = 0; i < 4; i++) {
                chars1[i] = chars[i];
            }
            String s = new String(chars1);
            int a = Integer.parseInt(s, 2);
            return a;


        }

        private int getSeqNr(byte aByte) {
            char[] chars = Translator.byteTranslator(aByte);
            char[] chars1 = new char[5];

            for (int i = 0; i < 5; i++) {
                chars1[i] = chars[i];

            }
            String s = new String(chars1);


            return Integer.parseInt(s, 2);
        }

// SENDING SIDE.

        /**
         * Adds a header to the packet and sends the packet to be transmitted.
         *
         * @param m message to be sended.
         */
        private void sendPacket(String m) {
            if (m.getBytes(StandardCharsets.UTF_8).length > 2) {
                byte[] bytes = m.getBytes(StandardCharsets.UTF_8);
                //System.out.println("THIS IS THE MESSAGE AND THE REPR IN BYTES : " + m );
                ByteBuffer buff = ByteBuffer.allocate(bytes.length);


                buff.put(bytes); // Here we have a buffer that contains our message.
                //printByteBuffer(buff,buff.capacity());
                if (!fragmentor.checkFragmentation(m)) {

                    fragmentor.createPacket(mySeq, buff, false, true, false);
                    lock.lock();
                    retransmissionMap.put(mySeq, new Date(System.currentTimeMillis()));
                    lock.unlock();
                    increaseMySeq();
                }


            }

        }




    }

    /**
     * private class that listens for userInputs to be formatted into packets.
     */

    private class ApplicationListener extends Thread {
        BlockingQueue<String> receivingQueue;
        PacketHandler packetHandler;

        ApplicationListener(BlockingQueue<String> receivingQueue, PacketHandler packetHandler) {
            this.receivingQueue = receivingQueue;
            this.packetHandler = packetHandler;
        }

        public void receivingLoop() {

            String message;
            try {
                while (true) {
                    message = receivingQueue.take();
                    if (message != null) {
                        packetHandler.sendPacket(message);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            receivingLoop();
        }
    }
}
