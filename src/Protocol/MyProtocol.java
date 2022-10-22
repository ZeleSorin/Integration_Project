package Protocol;

import NetworkLayer.NetworkLayer;
import client.*;

import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is just some example code to show you how to interact
 * with the server using the provided 'Client' class and two queues.
 * Feel free to modify this code in any way you like!
 */

public class MyProtocol {

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 1000;
    ExecutorService pool;
    private BlockingQueue<Message> receivedQueue;
    public BlockingQueue getReceived(){
        return receivedQueue;
    }

    private BlockingQueue<Message> sendingQueue;
    public BlockingQueue getSending(){
        return sendingQueue;
    }
    Lock lock = new ReentrantLock();
    public NetworkLayer networkLayer;
    public MyProtocol(String server_ip,
                      int server_port,
                      int frequency ,
                      LinkedBlockingQueue<Message> sQ,
                      LinkedBlockingQueue<Message> rQ,
                      NetworkLayer networkLayer,
                      ExecutorService pool) {
        this.pool = pool;
        receivedQueue = rQ;
        sendingQueue = sQ;
        this.networkLayer = networkLayer;

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use
        receiveThread rt = new receiveThread(receivedQueue);
        pool.execute(rt);

        // Start thread to handle received messages!

        // handle sending from stdin from this thread.
//        try {
//            Scanner scan = new Scanner(System.in);
//            //Console console = System.console();
//            String input = "";
//            while (true) {
//                input = scan.nextLine(); // read input
//                byte[] inputBytes = input.getBytes(); // get bytes from input
//                ByteBuffer toSend = ByteBuffer.allocate(inputBytes.length); // make a new byte buffer with the length of the input string
//                toSend.put(inputBytes, 0, inputBytes.length); // copy the input string into the byte buffer.
//                Message msg;
//                if ((input.length()) > 2) {
//                    msg = new Message(MessageType.DATA, toSend);
//                } else {
//                    msg = new Message(MessageType.DATA_SHORT, toSend);
//                }
//                sendingQueue.put(msg);
//            }
//        } catch (InterruptedException e) {
//            System.exit(2);
//        }
    }


    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue) {
            super();
            this.receivedQueue = receivedQueue;
        }

        public static void printByteBuffer(ByteBuffer bytes, int bytesLength) {
            for (int i = 0; i < bytesLength; i++) {
                System.out.print(Byte.toString(bytes.get(i)) + " ");
            }
            System.out.println();
        }

        // Handle messages from the server / audio framework
        public void run() {
            while (true) {
                try {
                    Message m = receivedQueue.take();
//                    if (!receivedQueue.isEmpty()) {
//                        Message received = receivedQueue.take();
//                        System.out.println("[CHECKPOINT]");
////                        int cap = received.getData().capacity();
//                        ByteBuffer buff ;
//                        buff = received.getData();
//                        int recvID = 1;
//                        for (int i = 0; i < buff.capacity(); i++) {
//                            recvID *= buff.get(i);
//                            System.out.println((int) buff.get(i) + " ");
//
//                        }
//                        System.out.println("Received ID is " + recvID);
                    //}
                    if (m.getType() == MessageType.BUSY) { // The channel is busy (A node is sending within our detection range)
                        //System.out.println("BUSY");
                    } else if (m.getType() == MessageType.FREE) { // The channel is no longer busy (no nodes are sending within our detection range)
                        //System.out.println("FREE");
                    } else if (m.getType() == MessageType.DATA) { // We received a data frame!
                        //System.out.print("DATA: ");
                       // System.out.println("Received DATA");

                        networkLayer.startPacketAnalysis(m);
                       // printByteBuffer(m.getData(), m.getData().capacity()); //Just print the data
                    } else if (m.getType() == MessageType.DATA_SHORT) { // We received a short data frame!
                       // System.out.print("DATA_SHORT: ");
                        // Checks if the DATA_SHORT IS NOT ACK OR SOMETHING ELSE
                        networkLayer.startPacketAnalysis(m);
                       // printByteBuffer(m.getData(), m.getData().capacity()); //Just print the data
                    } else if (m.getType() == MessageType.DONE_SENDING) { // This node is done sending
                        //System.out.println("DONE_SENDING");
                    } else if (m.getType() == MessageType.HELLO) { // Server / audio framework hello message. You don't have to handle this
                       // System.out.println("HELLO");
                    } else if (m.getType() == MessageType.SENDING) { // This node is sending
                       // System.out.println("SENDING");
                    } else if (m.getType() == MessageType.END) { // Server / audio framework disconnect message. You don't have to handle this
                       // System.out.println("END");
                        System.exit(0);
                    }

                } catch (InterruptedException e) {
                    System.err.println("Failed to take from queue: " + e);
                }

            }
        }
    }

}
