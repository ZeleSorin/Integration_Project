package ApplicationLayer;

import NetworkLayer.NetworkLayer;
import Protocol.MyProtocol;
import client.Message;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class ApplicationLayer {

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 1001;
    private BlockingQueue<String> sendingQueue;
    private BlockingQueue<String> receivingQueue;
    private Scanner scanner;
    public NetworkLayer networkLayer;
    public static boolean usernameSet = false;
    private LinkedBlockingQueue<Message> sQ ;
    private LinkedBlockingQueue<Message> rQ ;
    public static String username = "";
    ExecutorService pool = Executors.newFixedThreadPool(10);
    public ApplicationLayer(BlockingQueue<String> queueThatSendsTheUserInputToTheNetworkLayer, BlockingQueue<String> queueFromWhereWeReadMessagesFromTheNetworkLayer) {
        this.sendingQueue = queueThatSendsTheUserInputToTheNetworkLayer;
        this.receivingQueue = queueFromWhereWeReadMessagesFromTheNetworkLayer;
        sQ = new LinkedBlockingQueue<>();
        rQ = new LinkedBlockingQueue<>();

        networkLayer = new NetworkLayer( queueThatSendsTheUserInputToTheNetworkLayer, queueFromWhereWeReadMessagesFromTheNetworkLayer,sQ,rQ);
        MyProtocol myProtocol = new MyProtocol(SERVER_IP, SERVER_PORT, frequency,sQ,rQ,networkLayer, pool);
        pool.execute(networkLayer);
        receiveThread rt = new receiveThread(this.receivingQueue);
        pool.execute(rt);


        try {
            System.out.println("Please, enter your username!");
            System.out.println("[Hint: Your username should not be longer than 10 characters.]");
            scanner = new Scanner(System.in);
            String userInput = "";
            username = scanner.nextLine();
            while (username == "") {
                System.out.println("DOING");
                if (username != null && username != "") {
                    if (username.length() >= 10) {
                        System.out.println("username must be of EXACT 5 characters");
                    }
                    for (int i = 0; i < username.length(); i++) {
                        if (" ".charAt(0) == username.charAt(i)) {
                            username = "";
                        }
                    }
                }
                if (username == "") {
                    System.out.println("this is not a valid username \n\t pls try again");
                }
            }


            System.out.println("username accepted\n if you are interested to see the commands\n you can type the following:!help");
            usernameSet = true;
            networkLayer.run();
            boolean stop = false;
            while (!stop) {

                userInput = scanner.nextLine();

                boolean isACommand = false;
                if (userInput.contains("!")) {
                    isACommand = true;
                    switch (userInput.split("!")[1]) {
                        case "help":
                            System.out.println("Available commands:\n!exit------------->stops the program\n" +
                                    "!users-------> prints detailed information about nodes used to transfer data\n" +
                                    "!whispering------> once you call this you will send a private message to the node with the same username you                                                                                                                                           provided");
                            break;
                        case "exit":
                            stop = true;
                            System.out.println("[ending program]");
                            break;


                        case "users":

                            System.out.println("USERS:");
                            for (Integer k : networkLayer.usernameToIDLink.keySet()){
                                System.out.println(networkLayer.usernameToIDLink.get(k));
                            }
                            break;

                        case "whispering":

                            System.out.println("first introduce the username of the destination user");
                            String destinationUsername = scanner.nextLine();

                            System.out.println("write your message");
                            String message = scanner.nextLine();
                            break;
                        default:
                            queueThatSendsTheUserInputToTheNetworkLayer.put(userInput);
                            isACommand = false;
                    }
                }
                if (!isACommand) {
                    queueThatSendsTheUserInputToTheNetworkLayer.put(userInput);
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


        public static void main (String[]args){

            new ApplicationLayer(new LinkedBlockingQueue<String>(), new LinkedBlockingQueue<String>());

        }


        /**
         * Private class for creating the receiving thread.
         */

        private class receiveThread extends Thread {
            private BlockingQueue<String> receivedQueue;

            /**
             * Constructor for receiving thread
             *
             * @param receivedQueue
             */
            public receiveThread(BlockingQueue<String> receivedQueue) {
                super();
                this.receivedQueue = receivedQueue;
            }

            /**
             * verifying if the received String message contains the username glued with the data by "*".
             * if this is true, then printingMethod will split the message and prints the data
             * in a user-friendly format.
             *
             * @param input
             */
            public void printingMethod(String input) {
                //verify if the message has * in it
                //split the message

                if (input.contains("*")) {
                    String[] inputList = input.split(Pattern.quote("*"));
                    String username = inputList[0];
                    String data = inputList[1];
                    System.out.println(username + ": " + data);
                } else {
                    System.out.println(input);
                }
            }

            /**
             * method that runs continuously in "background" of the program
             * it will continuously try to take data from the received queue.
             */
            public void run() {
                while (true) {
                    try {

                        String input = receivedQueue.take();
                        if (input != null) {
                            printingMethod(input);
                        }
                    } catch (InterruptedException e) {
                        System.err.println("Failed to take from queue: " + e);
                    }
                }
            }
        }

    }
