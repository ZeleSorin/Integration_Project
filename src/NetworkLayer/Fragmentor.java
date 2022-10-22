package NetworkLayer;


import Tools.Translator;
import client.Message;
import client.MessageType;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Fragmentor object that handles the fragmentation and merging process of packets.
 * The fragmetor should take as input a String and fragment it into 29bytes packets with the
 * corresponding header. It has to take as parameter the seqnumber with which the numbering of the
 * fragmented packets should start.
 * <p>
 * <p>
 * As per packets that follow to be merged. The fragmentor will be an entity and not just a method call , in order
 * for it to be able to store fragments without overloading the main Classes.
 * In order to merge packets, the Fragmentor has to receive as parameters a buffer or some other storing structure
 * that holds the data of the packets. It also needs to receive the source IP as parameter so it can distinguish between
 * messages from different users. It will also receive a boolean that will state if there is any more packets that follows
 * to arrive.
 * <p>
 * returns String that will be sent by the Network layer to the app layer.
 */
public class Fragmentor {
    private BlockingQueue<String> sendingQueue;
    private BlockingQueue<Message> sendingQueueToLinkLayer;
    private BlockingQueue<String> receivingQueue;
    private final byte PADDINGBYTE = (byte) '*';
    private final String PADDINGSTRING = "*";
    public Fragmentor(BlockingQueue<String> receing, BlockingQueue<String> sending, BlockingQueue<Message> sendingQueueToLinkLayer) {
        this.sendingQueue = sending;
        this.receivingQueue = receing;
        this.sendingQueueToLinkLayer = sendingQueueToLinkLayer;
    }


    private String message;

    public HashMap<Integer, ByteBuffer> backupBuffer = new HashMap<>();
    private final HashMap<Integer, Date> backupBufferWithDates = new HashMap<>();

    private final HashMap<Integer, ArrayList<Integer>> IdMappedToSeqNumber = new HashMap<>();
    private final HashMap<Integer, ArrayList<ByteBuffer>> IDMappedToMessageFragments = new HashMap<>();

    /**
     * returns false if no fragmentation is needed,
     * returns true if fragmentation is needed.
     *
     * @param m message to be inspected.
     * @return true if the message needs to be fragmented.
     */
    public boolean checkFragmentation(String m) {
        return (Translator.stringToByteArray(m).length > 30);

    }


    /**
     * Method merges multiple messages into one string .
     * The merging happens after all the packets have arrived.The fragments are stored until this
     * condition is fullfiled.
     *
     * @param buffer buffer that follows to be combined with other packets.
     * @return boolean stating if the message was combined and added to the queue.
     */
    public void mergeMessage(ByteBuffer buffer) {

        char[] chars1 = Translator.byteTranslator(buffer.get(0));
        char[] chars2 = Translator.byteTranslator(buffer.get(1));
        int id = Integer.parseInt(Translator.charToStringBuilder(chars1, 0, 3));
        int offset = Integer.parseInt(String.valueOf(chars2[5]));
        int seqNr = Integer.parseInt(Translator.charToStringBuilder(chars2, 0, 4));
        buffer.position(1);


        if (offset == 1) {
            if (IdMappedToSeqNumber.containsKey(id)) {
                if (!IdMappedToSeqNumber.get(id).contains(seqNr)) {
                    IdMappedToSeqNumber.get(id).add(seqNr);
                    IDMappedToMessageFragments.get(id).add(buffer.slice());
                }

            } else {
                IdMappedToSeqNumber.put(id, new ArrayList<>());
                ArrayList<ByteBuffer> arr = new ArrayList<>();
                arr.add(buffer.slice());
                IDMappedToMessageFragments.put(id, arr);
            }

        } else {
            if (IdMappedToSeqNumber.containsKey(id)) {
                if (!IdMappedToSeqNumber.get(id).contains(seqNr)) {
                    IdMappedToSeqNumber.get(id).add(seqNr);
                    IDMappedToMessageFragments.get(id).add(buffer.slice());
                }
                ArrayList<ByteBuffer> arry = IDMappedToMessageFragments.get(id);
                for (int i = 0; i < arry.size(); i++) {
                    message += Translator.byteTranslatorToString(Translator.returnAllByteArray(arry.get(i)));
                }
                IdMappedToSeqNumber.remove(id);
                IDMappedToMessageFragments.remove(id);
            }


        }
        addToQueue(message);


    }


    /**
     * Method adds the merged messages to the queue for the App layer.
     *
     * @param m String to be added to queue.
     */
    public void addToQueue(String m) {
        sendingQueue.add(m);
    }

    //---------------------------------------------------------


    /**
     * fragments a message and returns an arrayList containing the buffers that follow to be sent.
     *
     * @param m message that needs to be fragmented
     * @return String array containing on each position a packet to be sent.
     */
    public void fragmentMessage(String m, int seqNr) {
        int numberOfPackets = 0;
        String fragmentedData = null;
        byte[] dataToSend = null;
        ByteBuffer buff = ByteBuffer.allocate(m.length());
        List<ByteBuffer> list = new ArrayList<>();

        if (m.length() > 30) {
            ArrayList<ByteBuffer> finalListToSend = null;
            if (m.length() % 30 != 0) {
                numberOfPackets = m.length() / 30 + 1;
            } else {
                numberOfPackets = m.length() / 30;
            }
            if (numberOfPackets == 1) {
                buff.put(Translator.stringToByteArray(m));
                list.add(buff);
            }
            for (int i = 0; i < numberOfPackets; i++) {
                if ((i + 1) * 30 < m.length()) {
                    fragmentedData = m.substring(i * 30, ((i + 1) * 30) - 1);
                    dataToSend = fragmentedData.getBytes();
                    ByteBuffer bufff = ByteBuffer.allocate(30);
                    bufff.put(dataToSend);
                    list.add(bufff);
                    createPacket(seqNr, bufff, true, false, false);


                } else {
                    fragmentedData = m.substring(i * 30);
                    dataToSend = fragmentedData.getBytes();
                    ByteBuffer bufff = ByteBuffer.allocate(30);
                    bufff.put(dataToSend);//method that adds the header accordingly and use that instead of dataToSend
                    list.add(bufff);
                    createPacket(seqNr, bufff, true, false, false);

                }
            }

        }

    }

    /**
     * Creates a separate packet with corresponding flags and sequence number.
     *
     * @param seqNum
     * @param buffer
     * @param offsetFlag
     * @param forwarding
     * @param retransmission
     */
    public void createPacket(int seqNum, ByteBuffer buffer, boolean offsetFlag, boolean forwarding, boolean retransmission) {

        ByteBuffer completePacket = addingHeaderToFragment(seqNum, buffer, offsetFlag, forwarding, retransmission);
        backupBuffer.put(seqNum, completePacket);
        backupBufferWithDates.put(seqNum, new Date(System.currentTimeMillis()));
        Message m = new Message(MessageType.DATA, completePacket);
        try {
            sendingQueueToLinkLayer.put(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Appends the header with the given values and personal node-ID to the given message.
     * Provides also a basis for when fragmentation, forwarding and/or retransmission is/are needed.
     *
     * @param seqNum
     * @param message
     * @param offsetFlag
     */
    public ByteBuffer addingHeaderToFragment(int seqNum,
                                             ByteBuffer message,
                                             boolean offsetFlag,
                                             boolean forwarding,
                                             boolean retransmission) {
        char[] header = new char[16];
        char[] intToCharArray;
        //char[] packet = new char[message.capacity() + header.length];


        //-----srcAddress------\\
        int srcAddress = NetworkLayer.getBitID();
        intToCharArray = headerHelper(4,srcAddress);
        for (int i = 0; i < 4; i++) {
            header[i] = intToCharArray[i];
        }


        //--------------DestAddress------------------\\
        for (int i = 4; i < 8; i++) {
            header[i] = '0';
        }


        //----------seqNum--------\\
        String s = Integer.toBinaryString(seqNum);
      //  System.out.println("FRAGMETOR CLASS LINE 241                                                          " +seqNum );
        intToCharArray = headerHelper(5,seqNum);
        for (int i = 8; i < 13; i++) { //MEthod for this
            header[i] = intToCharArray[i-8];
        }


        //------------OffsetFlag-----------------\\
        if (offsetFlag) {
            header[13] = '0';
        } else {
            header[13] = '0';
        }


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


        byte fb = quickMethod(0,8,header);

        byte sb = quickMethod(8,16,header);

//------conversions and extra helping fields----//


        ByteBuffer buffy = ByteBuffer.allocate(32);
        buffy.put(fb);
        buffy.put(sb);

        for (int i = 2; i<message.capacity()+2;i++){
            buffy.put(message.get(i-2));
        }

        if (message.capacity() < 30) {
            for (int i = message.capacity()+2;i<32 ;i++){
                buffy.put( PADDINGBYTE);
            }
        }

        return buffy;


    }
    public static void printByteBuffer(ByteBuffer bytes, int bytesLength) {
        for (int i = 0; i < bytesLength; i++) {
            System.out.print(Byte.toString(bytes.get(i)) + " ");
        }
        System.out.println();
    }

    /**
     * Method that takes charArray and return byte repr.
     * @param from
     * @param to
     * @param chars
     * @return
     */

    public byte quickMethod(int from, int to , char[] chars){
        char[] chars1 = new char[8];
        for (int i = from; i<to;i++){
            chars1[i-from] = chars[i];
        }
        String s = Translator.charToStringBuilder(chars1);
        int fByte = Integer.parseInt(s,2);
        byte b = (byte) fByte;
        return b;
    }

    /**
     * Method that adds specific numbers into a char array in order to solve the issue of having a slot
     * of size 5 but a binnary nr that is stored as for example a two digit nr.
     *
     * @param size        size of the field
     * @param nrToBeAdded nr to be added into the header field
     * @return field
     */
    public char[] headerHelper(int size, int nrToBeAdded) {
        char[] chars = new char[size];

        String s = Integer.toBinaryString(nrToBeAdded);
        char[] chars1 = s.toCharArray();
        int l = chars1.length;
        int diff = size - l;
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                chars[i] = '0';
            }
        }

        for (int i = diff;i<size;i++){
            chars[i] = chars1[i-diff];
        }
        return chars;
    }
}